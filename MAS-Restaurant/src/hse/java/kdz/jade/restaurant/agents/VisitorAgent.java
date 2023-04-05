package hse.java.kdz.jade.restaurant.agents;

import hse.java.kdz.jade.restaurant.types.VisitorOrder;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAAgentManagement.Property;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Date;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.*;

/**
 * Класс агента посетителя.
 * Поведения агента:
 * 1) Сделать заказ
 * 2) Отменить заказ
 * 3) Узнать время до завершения приготовления всего заказа
 * 4) Уйти, не дождавшись заказа
 * 5) Уйти, не сделав заказ из отсутствия блюд в меню в данный момент
 * 6) Дождаться завершения выполнения заказа и заплатить
 */
public class VisitorAgent extends RestaurantAgent {
    /**
     * Агент супервизора
     */
    private AID supervisorAgent;
    /**
     * Агент заказа
     */
    private AID orderAgent;
    /**
     * Заказ посетителя
     */
    private VisitorOrder visitorOrder;
    /**
     * Проинициализирован ли агент
     */
    private boolean initialized = false;

    @Override
    protected void setup() {
        visitorOrder = (VisitorOrder) getArguments()[0];
        supervisorAgent = find("Supervisor")[0];
        visitorOrder.setStarted(new Date());
        register("Visitor", new Property("visitorName", visitorOrder.getVisitorName()));
        report("был создан");
        addBehaviour(new InitializationBehaviour());
        addBehaviour(new OrderCompletionCheckingBehaviour());
        addBehaviour(new OrderCancellationBehaviour(rand.nextInt(100) < ORDER_CANCELLATION_PROBABILITY));
        addBehaviour(new OrderTimeRecognitionBehaviour());
        addBehaviour(new WaitingBehaviour(this, VISITOR_WAITING_TIME));
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику инициализации агента посетителя
     */
    private class InitializationBehaviour extends Behaviour {
        private int step = 0;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setConversationId("Order-creation");
                    msg.setOntology(visitorOrder.getVisitorName());
                    setObjectToMsg(msg, visitorOrder.getDishes());
                    msg.addReceiver(supervisorAgent);
                    send(msg);
                    step = 1;
                }
                case 1 -> {
                    MessageTemplate mt = MessageTemplate.MatchConversationId("Order-creation");
                    ACLMessage reply = receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            visitorOrder.setStarted(new Date());
                            visitorOrder.setTotalCost(calculatePriceByVisitorOrderedDishes(visitorOrder.getDishes()));
                            visitorOrder.setDishes(getObjectFromMsg(reply));
                            step = 2;
                        } else {
                            formattedReport("red", String.format("Посетитель '%s' ушёл, не сделав заказ", visitorOrder.getVisitorName()));
                            visitorOrder.setEnded(new Date());
                            step = 3;
                            addBehaviour(new SimulationInformingBehaviour());
                        }
                    } else {
                        block();
                    }
                }
                case 2 -> {
                    AID[] orders = find("Order", new Property("visitorName", visitorOrder.getVisitorName()));
                    if (orders.length > 0) {
                        orderAgent = orders[0];
                        initialized = true;
                        step = 3;
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
     * Поведение описывает логику работы агента в случае готовности заказа
     */
    private class OrderCompletionCheckingBehaviour extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("Order-completion"));
            ACLMessage msg = receive(mt);
            if (msg != null) {
                visitorOrder.setEnded(new Date());
                formattedReport("green", String.format("Посетитель '%s' получил весь свой заказ!", visitorOrder.getVisitorName()),
                        "Время получения заказа: " + getFormattedDate(visitorOrder.getEnded()));
                addBehaviour(new SimulationInformingBehaviour());
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

    /**
     * Класс поведения агента.
     * Поведение описывает логику отмены заказа посетителем
     */
    private class OrderCancellationBehaviour extends OneShotBehaviour {
        private final boolean willCancelOrder;

        private OrderCancellationBehaviour(boolean willCancelOrder) {
            this.willCancelOrder = willCancelOrder;
        }

        @Override
        public void action() {
            if (willCancelOrder) {
                int delay = rand.nextInt((MAX_ORDER_CANCELLATION_DELAY - MIN_ORDER_CANCELLATION_DELAY) + 1) + MIN_ORDER_CANCELLATION_DELAY;
                addBehaviour(new WakerBehaviour(myAgent, delay) {
                    @Override
                    protected void onWake() {
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.setConversationId("Order-cancellation");
                        msg.addReceiver(orderAgent);
                        send(msg);
                        visitorOrder.setEnded(new Date());
                        visitorOrder.setTotalCost(0);
                        formattedReport("red", String.format("Посетитель '%s' отменил свой заказ!", visitorOrder.getVisitorName()),
                                "Время отмены: " + getFormattedDate(new Date()));
                        addBehaviour(new SimulationInformingBehaviour());
                    }
                });
            }
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику узнавания посетителем времени до готовности заказа
     */
    private class OrderTimeRecognitionBehaviour extends Behaviour {
        private int step = 0;
        MessageTemplate mt;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    if (initialized) {
                        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.setConversationId("Order-time");
                        msg.setReplyWith(String.valueOf(System.currentTimeMillis()));
                        msg.addReceiver(orderAgent);
                        formattedReport("yellow", String.format("Посетитель '%s' решил узнать сколько осталось времени до приготовления заказа", visitorOrder.getVisitorName()));
                        send(msg);
                        mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.MatchConversationId("Order-time"));
                        mt = MessageTemplate.and(mt, MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                        step = 1;
                    }
                }
                case 1 -> {
                    ACLMessage reply = receive(mt);
                    if (reply != null) {
                        formattedReport("yellow", String.format("Посетитель '%s' узнал, что до приготовления заказа осталось %sс.",
                                visitorOrder.getVisitorName(), formatTime(Double.parseDouble(reply.getContent()))));
                        int delay = rand.nextInt((MAX_ORDER_TIME_RECOGNITION_DELAY - MIN_ORDER_TIME_RECOGNITION_DELAY) + 1) + MIN_ORDER_TIME_RECOGNITION_DELAY;
                        addBehaviour(new WakerBehaviour(myAgent, delay) {
                            @Override
                            protected void onWake() {
                                addBehaviour(new OrderTimeRecognitionBehaviour());
                            }
                        });
                        step = 2;
                    } else {
                        block();
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
     * Поведение описывает логику ухода посетителя по истечению заданного времени,
     * если заказ не был к этому моменту готов
     */
    private class WaitingBehaviour extends WakerBehaviour {
        public WaitingBehaviour(Agent a, long wakeupDate) {
            super(a, wakeupDate);
        }

        @Override
        protected void onWake() {
            addBehaviour(new OrderCancellationBehaviour(true));
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику оповещения агента симуляции о своём завершении работы
     */
    private class SimulationInformingBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            addVisitorOrderReport(visitorOrder);
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setConversationId("Visitor-end");
            setObjectToMsg(msg, visitorOrder);
            msg.addReceiver(find("Simulation")[0]);
            send(msg);
            doDelete();
        }
    }
}
