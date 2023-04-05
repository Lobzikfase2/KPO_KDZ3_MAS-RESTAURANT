package hse.java.kdz.jade.restaurant.agents;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.SIMULATION_DECELERATION_FACTOR;
import static hse.java.kdz.jade.restaurant.tools.AgentTools.formatTime;

/**
 * Абстрактный класс агента ресурса.
 * Поведения агента:
 * 1) Зарезервировать представляемый агентом ресурс на заданное время
 * 2) Отменить резервацию ресурса по запросу
 * 3) Сообщить о том, сколько осталось времени до конца резервации
 * 4) Освободить ресурс спустя заданное время
 */
public abstract class ResourceAgent extends RestaurantAgent {
    /**
     * Перечисление - статус, в котором может находиться ресурс
     */
    protected enum Status {
        RESERVED,
        NOT_RESERVED
    }

    /**
     * Время, оставшееся до конца резервации
     */
    protected double reservationTime;
    /**
     * Агент, зарезервировавший ресурс
     */
    protected AID reservedBy;
    /**
     * Статус ресурса
     */
    protected Status status;

    /**
     * Класс поведения агента.
     * Поведение описывает всю логику резервации оборудования,
     * логику её отмены, проверки времени окончания
     * и её самостоятельного завершения
     */
    protected class ReservationBehaviour extends CyclicBehaviour {
        private final int resourceID;
        private final String conversationID;
        private final String ontology;

        public ReservationBehaviour(int resourceID, String conversationID, String ontology) {
            this.resourceID = resourceID;
            this.conversationID = conversationID;
            this.ontology = ontology;
        }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchConversationId(conversationID);
            ACLMessage msg = receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setOntology(ontology);
                reply.setContent(String.valueOf(reservationTime));
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    if (status == Status.NOT_RESERVED) {
                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        reply.setContent(String.valueOf(resourceID));
                        status = Status.RESERVED;
                        reservedBy = msg.getSender();
                        reservationTime = Double.parseDouble(msg.getContent()) * SIMULATION_DECELERATION_FACTOR;
                        report(String.format("зарезервирован на %sc.", formatTime(reservationTime)));
                        myAgent.send(reply);
                        addBehaviour(new TickerBehaviour(myAgent, 10) {
                            @Override
                            protected void onTick() {
                                reservationTime -= 0.01;
                                if (reservationTime <= 0) {
                                    reservationTime = 0;
                                    status = Status.NOT_RESERVED;
                                    report("резервация окончена");
                                    myAgent.removeBehaviour(this);
                                }
                            }
                        });
                    } else {
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        report("запрос на резервацию от " + msg.getSender().getLocalName() + " отклонен");
                        myAgent.send(reply);
                    }
                } else if (msg.getPerformative() == ACLMessage.REQUEST) {
                    reply.setPerformative(ACLMessage.INFORM);
                    myAgent.send(reply);
                } else if (msg.getPerformative() == ACLMessage.CANCEL && msg.getSender().getName().equals(reservedBy.getName())) {
                    status = Status.NOT_RESERVED;
                    reservationTime = 0;
                    report("резервация была отменена");
                }
            } else {
                block();
            }
        }
    }
}
