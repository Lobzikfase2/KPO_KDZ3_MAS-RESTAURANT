package hse.java.kdz.jade.restaurant.agents;

import hse.java.kdz.jade.restaurant.tools.AgentTools;
import hse.java.kdz.jade.restaurant.types.Operation;
import hse.java.kdz.jade.restaurant.types.OperationReport;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAAgentManagement.Property;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Date;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.*;

/**
 * Класс агента операции.
 * Поведения агента:
 * 1) Выполнить заданную операцию, сделав для этого все необходимое
 * 2) Отменить выполнение текущей операции (по запросу от агента процесса)
 * 3) Сообщить сколько времени осталось до завершения операции
 */
public class OperationAgent extends RestaurantAgent {
    /**
     * Выполняемая операция
     */
    private Operation operation;
    /**
     * Id заказанного блюда
     */
    private int orderedDishID;
    /**
     * Id операции
     */
    private int operationID;
    /**
     * Время, оставшееся до завершения операции
     */
    private double timeLeft;
    /**
     * Агент склада
     */
    private AID warehouseAgent;
    /**
     * Зарезервирован ли повар
     */
    private boolean isCookReserved = false;
    /**
     * Зарезервировано ли оборудование
     */
    private boolean isEquipmentReserved = false;
    /**
     * Отменена ли операция
     */
    private boolean cancelled = false;
    /**
     * Агент зарезервированного оборудования
     */
    private AID reservedEquipmentAgent;
    /**
     * Агент зарезервированного повара
     */
    private AID reservedCookAgent;
    /**
     * Выполняется ли операция
     */
    private boolean executing = false;
    /**
     * Отчёт об операции (сохраняется в operation_log.json)
     */
    private OperationReport operationReport;

    @Override
    protected void setup() {
        operation = (Operation) getArguments()[0];
        orderedDishID = (int) getArguments()[2];
        operationID = (int) getArguments()[5];
        warehouseAgent = find("Warehouse")[0];
        register("Operation", new Property("orderedDishID", orderedDishID),
                new Property("currentOperation", getArguments()[4]),
                new Property("operationID", operationID));
        report("был создан");
        operationReport = new OperationReport(operationID, (int) getArguments()[1], (int) getArguments()[3]);
        operationReport.setStarted(new Date());
        addBehaviour(new OperationExecutionBehaviour());
        addBehaviour(new OperationCancellationBehaviour());
        addBehaviour(new OperationTimeRecognitionBehaviour());
        addBehaviour(new DeleteBehaviour());
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику выполнения операции.
     * Агент резервирует необходимые продукты через агента склада.
     * Агент резервирует повара через агента повара и оборудование через агента оборудования.
     * По истечению заданного времени операция считается завершенной и сообщает об этом своему агенту процесса
     */
    private class OperationExecutionBehaviour extends Behaviour {
        private int step = 0;
        ACLMessage msg;

        @Override
        public void action() {
            if (cancelled) {
                step = 5;
            }

            switch (step) {
                case 0 -> {
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(warehouseAgent);
                    msg.setConversationId("Reserving");
                    msg.setOntology(String.valueOf(orderedDishID));
                    for (var product : operation.getProducts()) {
                        setObjectToMsg(msg, product);
                        send(msg);
                    }
                    step = 1;
                }
                case 1 -> {
                    if (operation.getEquipmentType() == -1) {
                        isEquipmentReserved = true;
                        step = 2;
                        break;
                    }
                    addBehaviour(new ResourceReservationBehaviour("Equipment"));
                    step = 2;
                }
                case 2 -> {
                    if (isEquipmentReserved) {
                        addBehaviour(new ResourceReservationBehaviour("Cook"));
                        step = 3;
                    }
                }
                case 3 -> {
                    if (isCookReserved) {
                        timeLeft = operation.getTime() * SIMULATION_DECELERATION_FACTOR + 0.1;
                        if (CHECK_OPERATION_TYPE_AVAILABILITY) {
                            report(String.format("приступил к выполнению операции '%s'", findOperationNameByType(operation.getType())));
                        } else {
                            report("приступил к выполнению операции");
                        }
                        operationReport.setStarted(new Date());
                        operationReport.setActive(true);
                        step = 4;
                    }
                }
                case 4 -> {
                    if (!executing) {
                        addBehaviour(new ExecutionTimerBehaviour(myAgent));
                    }
                    if (timeLeft == 0) {
                        operationReport.setEnded(new Date());
                        operationReport.setActive(false);
                        msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(find("Process", new Property("orderedDishID", orderedDishID))[0]);
                        msg.setConversationId(String.valueOf(orderedDishID));
                        myAgent.send(msg);
                        report("завершил операцию");
                        step = 5;
                    }
                }
            }
        }

        @Override
        public boolean done() {
            return step == 5;
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику работы таймера завершения операции
     */
    private class ExecutionTimerBehaviour extends TickerBehaviour {
        public ExecutionTimerBehaviour(Agent a) {
            super(a, 10);
            executing = true;
        }

        @Override
        protected void onTick() {
            timeLeft -= 0.01;
            if (timeLeft <= 0) {
                timeLeft = 0;
                executing = false;
                myAgent.removeBehaviour(this);
            }
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику отмены выполнения операции
     * Агент информирует об этом агентов продуктов, агента повара и агента оборудования
     */
    private class OperationCancellationBehaviour extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("Operation-cancellation"));
            ACLMessage msg = receive(mt);
            if (msg != null) {
                operationReport.setEnded(new Date());
                operationReport.setActive(false);
                cancelled = true;
                timeLeft = 0;
                report("операция была отменена");
                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setConversationId("cancellation");
                for (var productAgent : find("Product", new Property("orderedDishID", orderedDishID))) {
                    msg.addReceiver(productAgent);
                }
                myAgent.send(msg);
                msg = new ACLMessage(ACLMessage.CANCEL);
                if (reservedEquipmentAgent != null) {
                    msg.addReceiver(reservedEquipmentAgent);
                    msg.setConversationId("Equipment-reserving");
                    send(msg);
                }
                if (reservedCookAgent != null) {
                    msg.addReceiver(reservedCookAgent);
                    msg.setConversationId("Cook-reserving");
                    send(msg);
                }
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
     * Поведение описывает логику резервации конкретного ресурса
     */
    private class ResourceReservationBehaviour extends Behaviour {
        private int step = 0;
        private ArrayList<ACLMessage> replies;
        private MessageTemplate mt;
        private AID[] agents;
        private final String resourceType;
        ACLMessage msg;
        ACLMessage reply;

        public ResourceReservationBehaviour(String resourceType) {
            this.resourceType = resourceType;
        }

        @Override
        public void action() {
            if (cancelled) {
                step = 5;
            }

            switch (step) {
                case 0 -> {
                    if (resourceType.equals("Equipment")) {
                        agents = find("Equipment", new Property("equipmentType", operation.getEquipmentType()));
                    } else {
                        agents = find("Cook");
                    }
                    replies = new ArrayList<>();
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setConversationId(resourceType + "-reserving");
                    msg.setReplyWith(String.valueOf(operationID));
                    for (var agent : agents) {
                        msg.addReceiver(agent);
                    }
                    myAgent.send(msg);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(resourceType + "-reserving"), MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    step = 1;
                }
                case 1 -> {
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        replies.add(reply);
                        if (replies.size() >= agents.length) {
                            step = 2;
                        }
                    } else {
                        block();
                    }
                }
                case 2 -> {
                    replies.sort((o1, o2) -> {
                        try {
                            return Double.compare(Double.parseDouble(o1.getContent()), Double.parseDouble(o2.getContent()));
                        } catch (Exception e) {
                            return 0;
                        }
                    });
                    try {
                        if (Double.parseDouble(replies.get(0).getContent()) > 0) {
                            step = 4;
                        } else {
                            msg = new ACLMessage(ACLMessage.PROPOSE);
                            msg.setConversationId(resourceType + "-reserving");
                            msg.setContent(String.valueOf(operation.getTime()));
                            msg.setReplyWith(String.valueOf(operationID));
                            msg.addReceiver(replies.get(0).getSender());
                            myAgent.send(msg);
                            step = 3;
                        }
                    } catch (NumberFormatException e) {
                        step = 0;
                    }
                }
                case 3 -> {
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                            step = 4;
                        } else {
                            if (resourceType.equals("Equipment")) {
                                operationReport.setCookId(Integer.parseInt(reply.getContent()));
                                isEquipmentReserved = true;
                                reservedEquipmentAgent = reply.getSender();
                            } else {
                                operationReport.setEquipmentId(Integer.parseInt(reply.getContent()));
                                isCookReserved = true;
                                reservedCookAgent = reply.getSender();
                            }
                            step = 5;
                        }
                    } else {
                        block();
                    }
                }
                case 4 -> {
                    addBehaviour(new WakerBehaviour(myAgent, 50) {
                        @Override
                        protected void onWake() {
                            addBehaviour(new ResourceReservationBehaviour(resourceType));
                        }
                    });
                    step = 5;
                }
            }
        }

        @Override
        public boolean done() {
            return step == 5;
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику вычисления оставшегося времени до завершения операции
     */
    private class OperationTimeRecognitionBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("Operation-time"));
            ACLMessage msg = receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setContent(String.valueOf(timeLeft));
                send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику удаления агента
     */
    private class DeleteBehaviour extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("delete"));
            ACLMessage msg = receive(mt);
            if (msg != null) {
                if (operationReport.getEnded() == null) {
                    operationReport.setEnded(new Date());
                }
                AgentTools.addOperationReport(operationReport);
                doDelete();
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
