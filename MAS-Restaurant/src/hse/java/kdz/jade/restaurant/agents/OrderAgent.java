package hse.java.kdz.jade.restaurant.agents;

import hse.java.kdz.jade.restaurant.types.DishCard;
import hse.java.kdz.jade.restaurant.types.VisitorOrderedDish;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.Property;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.formattedReport;

/**
 * Класс агента заказа.
 * Поведения агента:
 * 1) Сообщить посетителю о готовности заказа
 * 2) Сообщить посетителю сколько времени осталось до приготовления заказа
 * 3) Отменить весь заказ
 */
public class OrderAgent extends RestaurantAgent {
    /**
     * Имя посетителя, сделавшего заказ
     */
    private String visitorName;
    /**
     * Id заказа
     */
    private int orderID;
    /**
     * Заказанные посетителем блюда
     */
    private ArrayList<VisitorOrderedDish> visitorOrderedDishes;
    /**
     * Агенты блюд из данного заказа
     */
    private AID[] dishAgents;
    /**
     * Завершен ли заказ
     */
    private boolean ended = false;


    @Override
    protected void setup() {
        visitorName = (String) getArguments()[0];
        orderID = (int) getArguments()[1];
        visitorOrderedDishes = (ArrayList<VisitorOrderedDish>) getArguments()[2];
        addBehaviour(new InitializationBehaviour());
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику инициализации агента заказа
     */
    private class InitializationBehaviour extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            dishAgents = find("Dish", new Property("orderID", orderID));
            if (dishAgents.length == visitorOrderedDishes.size()) {
                isDone = true;
                register("Order", new Property("orderID", orderID), new Property("visitorName", visitorName));
                report("был создан");
                addBehaviour(new OrderCompletionCheckingBehaviour());
                addBehaviour(new OrderTimeRecognitionBehaviour());
                addBehaviour(new OrderCancellationBehaviour());
                addBehaviour(new DeleteBehaviour());
            }
        }

        @Override
        public boolean done() {
            return isDone;
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику проверки готовности всего заказа и информирования об этом посетителя
     */
    private class OrderCompletionCheckingBehaviour extends Behaviour {
        private int step = 0;
        private int repliesCount = 0;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    if (dishAgents.length == 0) {
                        step = 1;
                        break;
                    }
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                            MessageTemplate.MatchConversationId("Dish-completion"));
                    ACLMessage msg = receive(mt);
                    if (msg != null) {
                        formattedReport("blue", String.format("Блюдо (%s) было подано посетителю '%s'", msg.getContent(), visitorName));
                        repliesCount++;
                        if (repliesCount >= dishAgents.length) {
                            ended = true;
                            step = 1;
                        }
                    } else {
                        block();
                    }
                }
                case 1 -> {
                    AID[] visitorAgent = find("Visitor", new Property("visitorName", visitorName));
                    if (visitorAgent.length > 0) {
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.setConversationId("Order-completion");
                        msg.addReceiver(visitorAgent[0]);
                        send(msg);
                        step = 2;
                    }
                }
            }
        }

        @Override
        public boolean done() {
            return step == 2;
        }
    }


    /**
     * Класс поведения агента.
     * Поведение описывает логику распознавания оставшегося времени до приготовления всего заказа
     */
    private class OrderTimeRecognitionBehaviour extends CyclicBehaviour {
        private int step = 0;
        private int repliesCount;
        private double time;
        private ArrayList<DishCard> dishCards;
        MessageTemplate mt;
        ACLMessage msg;
        private ACLMessage reply;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    dishCards = new ArrayList<>();
                    time = 0;
                    repliesCount = 0;
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Order-time"));
                    msg = receive(mt);
                    if (msg != null) {
                        reply = msg.createReply();
                        if (ended) {
                            reply.setContent("0");
                            send(reply);
                            step = 0;
                            break;
                        }
                        msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.setConversationId("Dish-time");
                        for (var dishAgent : dishAgents) {
                            msg.addReceiver(dishAgent);
                        }
                        send(msg);
                        step = 1;
                    } else {
                        block();
                    }
                }
                case 1 -> {
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Dish-time"));
                    msg = receive(mt);
                    if (msg != null) {
                        repliesCount++;
                        if (msg.getOntology().equals("Time")) {
                            time += Double.parseDouble(msg.getContent());
                        } else {
                            dishCards.add(getObjectFromMsg(msg));
                        }
                        if (repliesCount >= dishAgents.length) {
                            if (dishCards.size() == 0) {
                                reply.setContent(String.valueOf(time));
                                send(reply);
                                step = 0;
                            }
                            msg = new ACLMessage(ACLMessage.REQUEST);
                            msg.setConversationId("Time-calculation");
                            setObjectToMsg(msg, dishCards);
                            msg.setOntology("DishCards");
                            msg.addReceiver(find("Menu")[0]);
                            send(msg);
                            step = 2;
                        }
                    } else {
                        block();
                    }
                }
                case 2 -> {
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Time-calculation"));
                    msg = receive(mt);
                    if (msg != null) {
                        time += Double.parseDouble(msg.getContent());
                        reply.setContent(String.valueOf(time));
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
     * Поведение описывает логику отмены заказа посетителем и последующей остановки приготовления всех блюд
     */
    private class OrderCancellationBehaviour extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("Order-cancellation"));
            ACLMessage msg = receive(mt);
            if (msg != null) {
                ended = true;
                report("заказ был отменен!");
                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setConversationId("Stop-cooking");
                for (var dishAgent : dishAgents) {
                    msg.addReceiver(dishAgent);
                }
                send(msg);
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
