package hse.java.kdz.jade.restaurant.agents;

import hse.java.kdz.jade.restaurant.tools.AgentTools;
import hse.java.kdz.jade.restaurant.types.Operation;
import hse.java.kdz.jade.restaurant.types.ProcessReport;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPAAgentManagement.Property;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Date;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.getAndIncrementOperationsCount;

/**
 * Класс агента процесса.
 * Поведения агента:
 * 1) Выполнить весь процесс приготовления блюда
 * 2) Сообщить агенту блюда о завершении процесса приготовления
 * 3) Отменить процесс приготовления (по запросу от агента блюда)
 */
public class ProcessAgent extends RestaurantAgent {
    /**
     * Операции процесса
     */
    private ArrayList<Operation> operations;
    /**
     * Id заказанного блюда
     */
    private int orderedDishID;
    /**
     * Id карточки блюда
     */
    private int dishCardID;
    /**
     * Id процесса
     */
    private int processID;
    /**
     * Индекс текущей операции в процессе
     */
    private int currentOperation = 0;
    /**
     * Отменен ли процесс
     */
    private boolean cancelled = false;
    /**
     * Отчёт о процессе (сохраняется в process_log.json)
     */
    private ProcessReport processReport;

    @Override
    protected void setup() {
        operations = (ArrayList<Operation>) getArguments()[0];
        orderedDishID = (int) getArguments()[1];
        dishCardID = (int) getArguments()[2];
        processID = (int) getArguments()[3];
        register("Process", new Property("orderedDishID", orderedDishID), new Property("processID", processID));
        report("был запущен");
        processReport = new ProcessReport(processID, orderedDishID);
        processReport.setStarted(new Date());
        processReport.setActive(true);
        addBehaviour(new ProcessBehaviour());
        addBehaviour(new ProcessCancellationBehaviour());
        addBehaviour(new ProcessTimeRecognitionBehaviour());
        addBehaviour(new DeleteBehaviour());
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику процесса приготовления блюда.
     * Все операции выполняются последовательно их соответственными агентами
     * (Параллельность выполнения всех операций процесс легко реализуема
     * путем создания нескольких агентов операций одновременно)
     */
    private class ProcessBehaviour extends Behaviour {
        private int step = 0;

        @Override
        public void action() {
            if (cancelled) {
                step = 3;
            }
            switch (step) {
                case 0 -> {
                    int operationsCount = getAndIncrementOperationsCount();
                    processReport.addProcessOperation(operationsCount);
                    create(String.format("OperationAgent[dish:%d-%d]", orderedDishID, currentOperation), OperationAgent.class,
                            operations.get(currentOperation), processID, orderedDishID, dishCardID, currentOperation, operationsCount);
                    step = 1;
                }
                case 1 -> {
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                            MessageTemplate.MatchConversationId(String.valueOf(orderedDishID)));
                    ACLMessage msg = receive(mt);
                    if (msg != null) {
                        currentOperation++;
                        if (currentOperation == operations.size()) {
                            step = 2;
                        } else {
                            step = 0;
                        }
                    } else {
                        block();
                    }
                }
                case 2 -> {
                    processReport.setEnded(new Date());
                    processReport.setActive(false);
                    report("процесс приготовления блюда завершен");
                    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(find("Dish", new Property("orderedDishID", orderedDishID))[0]);
                    msg.setConversationId(String.valueOf(orderedDishID));
                    myAgent.send(msg);
                    step = 3;
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
     * Поведение описывает логику отмены процесса приготовления блюда
     */
    private class ProcessCancellationBehaviour extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("Process-cancellation"));
            ACLMessage msg = receive(mt);
            if (msg != null) {
                cancelled = true;
                processReport.setEnded(new Date());
                processReport.setActive(false);
                report("процесс был отменен!");
                AID[] operationAgent = find("Operation", new Property("orderedDishID", orderedDishID),
                        new Property("currentOperation", currentOperation));
                if (operationAgent.length > 0) {
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setConversationId("Operation-cancellation");
                    msg.addReceiver(operationAgent[0]);
                    myAgent.send(msg);
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
     * Поведение описывает логику распознавания оставшегося времени до завершения процесса
     */
    private class ProcessTimeRecognitionBehaviour extends CyclicBehaviour {
        private int step = 0;
        private double time = 0;
        MessageTemplate mt;
        ACLMessage msg;
        ACLMessage reply;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Process-time"));
                    msg = receive(mt);
                    if (msg != null) {
                        reply = msg.createReply();
                        AID[] currentOperationAgent = find("Operation", new Property("orderedDishID", orderedDishID), new Property("currentOperation", currentOperation));
                        if (currentOperationAgent.length > 0) {
                            msg = new ACLMessage(ACLMessage.REQUEST);
                            msg.setConversationId("Operation-time");
                            msg.addReceiver(currentOperationAgent[0]);
                            send(msg);
                            step = 1;
                            break;
                        }
                        step = 2;
                    } else {
                        block();
                    }
                }
                case 1 -> {
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Operation-time"));
                    msg = receive(mt);
                    if (msg != null) {
                        time += Double.parseDouble(msg.getContent());
                        if (currentOperation >= operations.size() - 1) {
                            reply.setContent(String.valueOf(time));
                            send(reply);
                            time = 0;
                            step = 0;
                            break;
                        }
                        step = 2;
                    } else {
                        block();
                    }
                }
                case 2 -> {
                    if (currentOperation == operations.size()) {
                        reply.setContent(String.valueOf(time));
                        send(reply);
                        time = 0;
                        step = 0;
                        break;
                    }
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setConversationId("Time-calculation");
                    setObjectToMsg(msg, new ArrayList<>(operations.subList(currentOperation + 1, operations.size())));
                    msg.setOntology("Operations");
                    msg.addReceiver(find("Menu")[0]);
                    send(msg);
                    step = 2;
                }
                case 3 -> {
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Time-calculation"));
                    msg = receive(mt);
                    if (msg != null) {
                        time += Double.parseDouble(msg.getContent());
                        reply.setContent(String.valueOf(time));
                        send(reply);
                        time = 0;
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
                if (processReport.getEnded() == null) {
                    processReport.setEnded(new Date());
                }
                AgentTools.addProcessReport(processReport);
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
