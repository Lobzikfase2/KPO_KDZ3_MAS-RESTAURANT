package hse.java.kdz.jade.restaurant.agents;

import hse.java.kdz.jade.restaurant.types.DishCard;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.Property;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.findDishCardByMenuId;
import static hse.java.kdz.jade.restaurant.tools.AgentTools.getAndIncrementProcessesCount;


/**
 * Класс агента блюда.
 * Поведения агента:
 * 1) Начать готовить блюдо (по запросу от агента супервизора)
 * 2) Отменить приготовление блюда (по запросу от агента супервизора)
 * 3) Сообщить сколько времени осталось до приготовления блюда (по запросу от агента заказа)
 * 4) Сообщить агенту заказа о готовности блюда
 */
public class DishAgent extends RestaurantAgent {
    /**
     * Перечисление - статус, в котором может находиться блюдо
     */
    private enum Status {
        NOT_COOKING,
        COOKING,
        COOKED,
        CANCELLED,
        DELETING
    }

    /**
     * Id заказанного блюда
     */
    private int orderedDishID;
    /**
     * Id заказа
     */
    private int orderID;
    /**
     * Карточка блюда
     */
    private DishCard dishCard;
    /**
     * Статус приготовления блюда
     */
    private Status status;
    /**
     * Приоритет блюда.
     * Влияет на расчеты алгоритма выбора очередного блюда для готовки.
     * Исключает ситуации, когда блюда из ранних заказов не готовятся длительное время
     */
    private int priority = 1;

    @Override
    protected void setup() {
        orderedDishID = (int) getArguments()[0];
        dishCard = findDishCardByMenuId((int) getArguments()[1]);
        orderID = (int) getArguments()[2];
        status = Status.NOT_COOKING;
        register("Dish", new Property("orderedDishID", orderedDishID), new Property("orderID", orderID));
        report(String.format("был создан для блюда '%s'", dishCard.getName()));
        addBehaviour(new StartCookingBehaviour());
        addBehaviour(new StopCookingBehaviour());
        addBehaviour(new DishTimeRecognitionBehaviour());
        addBehaviour(new ManagementBehaviour());
        addBehaviour(new PriorityIncreasingBehaviour());
        addBehaviour(new DeleteBehaviour());
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику начала готовки блюда
     */
    private class StartCookingBehaviour extends Behaviour {
        private int step = 0;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Start-cooking"));
                    ACLMessage msg = receive(mt);
                    if (msg != null) {
                        report("начинает процесс приготовления...");
                        int processesCount = getAndIncrementProcessesCount();
                        create(String.format("ProcessAgent[%d, dish:%d]", processesCount, orderedDishID), ProcessAgent.class,
                                dishCard.getOperations(), orderedDishID, dishCard.getId(), processesCount);
                        step = 1;
                    } else {
                        block();
                    }
                }
                case 1 -> {
                    AID[] processAgent = find("Process", new Property("orderedDishID", orderedDishID));
                    if (processAgent.length > 0) {
                        status = Status.COOKING;
                        step = 2;
                    }
                }
                case 2 -> {
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                            MessageTemplate.MatchConversationId(String.valueOf(orderedDishID)));
                    ACLMessage reply = receive(mt);
                    if (reply != null) {
                        report("приготовлено!");
                        status = Status.COOKED;
                        AID[] orderAgent = find("Order", new Property("orderID", orderID));
                        if (orderAgent.length > 0) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.setConversationId("Dish-completion");
                            msg.setContent(dishCard.getName());
                            msg.addReceiver(orderAgent[0]);
                            send(msg);
                        }
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.setConversationId("delete");
                        for (var productAgent : find("Product", new Property("orderedDishID", orderedDishID))) {
                            msg.addReceiver(productAgent);
                        }
                        send(msg);
                        step = 3;
                    } else {
                        block();
                    }
                }
            }
        }

        @Override
        public boolean done() {
            return step == 3;
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику остановки приготовления блюда
     */
    private class StopCookingBehaviour extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("Stop-cooking"));
            ACLMessage msg = receive(mt);
            if (msg != null) {
                isDone = true;
                report("отменяет процесс приготовления блюда...");
                if (status == Status.COOKING) {
                    addBehaviour(new CancelProcessBehaviour());
                }
                status = Status.CANCELLED;
                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setConversationId("delete");
                for (var productAgent : find("Product", new Property("orderedDishID", orderedDishID))) {
                    msg.addReceiver(productAgent);
                }
                send(msg);
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return isDone;
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику отмены текущего процесса приготовления в случае,
     * если агент процесса был уже запущен
     */
    private class CancelProcessBehaviour extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            AID[] processAgent = find("Process", new Property("orderedDishID", orderedDishID));
            if (processAgent.length > 0) {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setConversationId("Process-cancellation");
                msg.addReceiver(processAgent[0]);
                send(msg);
                isDone = true;
            }
        }

        @Override
        public boolean done() {
            return isDone;
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику распознавания оставшегося времени до приготовления блюда
     */
    private class DishTimeRecognitionBehaviour extends CyclicBehaviour {
        private int step = 0;
        ACLMessage reply;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Dish-time"));
                    ACLMessage msg = receive(mt);
                    if (msg != null) {
                        reply = msg.createReply();
                        if (status == Status.CANCELLED || status == Status.COOKED) {
                            reply.setContent("0");
                            reply.setOntology("Time");
                            send(reply);
                        } else if (status == Status.NOT_COOKING) {
                            reply.setOntology("DishCard");
                            setObjectToMsg(reply, dishCard);
                            send(reply);
                        } else if (status == Status.COOKING) {
                            msg = new ACLMessage(ACLMessage.REQUEST);
                            msg.setConversationId("Process-time");
                            msg.addReceiver(find("Process", new Property("orderedDishID", orderedDishID))[0]);
                            send(msg);
                            step = 1;
                        }
                    } else {
                        block();
                    }
                }
                case 1 -> {
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Process-time"));
                    ACLMessage msg = receive(mt);
                    if (msg != null) {
                        reply.setOntology("Time");
                        reply.setContent(msg.getContent());
                        send(reply);
                        step = 0;
                    } else {
                        block();
                    }
                }
            }
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику взаимодействия с агентом супервизора,
     * в моменты когда тот актуализирует информацию на кухне для принятия решения
     * о выборе очередного блюда для готовки
     */
    private class ManagementBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("Management"));
            ACLMessage msg = receive(mt);
            if (status == Status.DELETING && find("Supervisor").length == 0) {
                doDelete();
            } else if (msg != null) {
                ACLMessage reply = msg.createReply();
                if (status == Status.DELETING) {
                    send(reply);
                    doDelete();
                } else if (status == Status.NOT_COOKING) {
                    reply.setOntology("DishCard");
                    reply.setProtocol(String.valueOf(priority));
                    setObjectToMsg(reply, dishCard);
                    send(reply);
                } else {
                    reply.setOntology("Pass");
                    if (status == Status.COOKING) {
                        reply.setContent("Cooking");
                    } else {
                        reply.setContent("Canceled");
                    }
                    send(reply);
                }
            } else {
                block();
            }
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику увеличения приоритета блюда
     */
    private class PriorityIncreasingBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("Increase-priority"));
            ACLMessage msg = receive(mt);
            if (msg != null) {
                if (status == Status.NOT_COOKING) {
                    priority += Integer.parseInt(msg.getContent());
                    report("приоритет был изменен. Текущий приоритет " + priority);
                }
            } else {
                block();
            }
        }
    }

    private class DeleteBehaviour extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("delete"));
            ACLMessage msg = receive(mt);
            if (msg != null) {
                status = Status.DELETING;
                isDone = true;
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return isDone;
        }
    }
}
