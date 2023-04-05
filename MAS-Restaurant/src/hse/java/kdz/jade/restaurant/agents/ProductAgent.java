package hse.java.kdz.jade.restaurant.agents;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPAAgentManagement.Property;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Класс агента продукта.
 * Поведения агента:
 * 1) Отменить резервацию представляемого агентом продукта
 */
public class ProductAgent extends RestaurantAgent {
    /**
     * Id зарезервированного продукта
     */
    private int productID;
    /**
     * Количество зарезервированного продукта
     */
    private double quantity;
    /**
     * Агент склада
     */
    private AID warehouseAgent;

    @Override
    protected void setup() {
        productID = (int) getArguments()[1];
        quantity = (double) getArguments()[2];
        warehouseAgent = find("Warehouse")[0];
        register("Product", new Property("orderedDishID", getArguments()[0]));
        report(String.format("зарезервирован для блюда (%s)", getArguments()[0]));
        addBehaviour(new CancelReservationBehaviour());
        addBehaviour(new DeleteBehaviour());
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику отмены резервации продукта и его возвращение на склад
     */
    private class CancelReservationBehaviour extends Behaviour {
        private boolean isDone = false;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("cancellation"));
            ACLMessage msg = receive(mt);
            if (msg != null) {
                isDone = true;
                System.out.println("резервация отменена");
                msg = new ACLMessage(ACLMessage.CANCEL);
                msg.setConversationId(String.valueOf(productID));
                msg.setContent(String.valueOf(quantity));
                msg.addReceiver(warehouseAgent);
                myAgent.send(msg);
                doDelete();
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
