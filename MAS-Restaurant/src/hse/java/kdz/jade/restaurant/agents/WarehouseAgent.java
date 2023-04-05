package hse.java.kdz.jade.restaurant.agents;

import hse.java.kdz.jade.restaurant.tools.DataParser;
import hse.java.kdz.jade.restaurant.types.OperationProduct;
import hse.java.kdz.jade.restaurant.types.Product;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;

/**
 * Класс агента блюда.
 * Поведения агента:
 * 1) Резервация продукта со склада и создание соответственного агента продукта
 * 2) Возврат продукта обратно на склад, посланного агентом продукта
 */
public class WarehouseAgent extends RestaurantAgent {
    private ArrayList<Product> products;

    @Override
    protected void setup() {
        products = DataParser.Data.products;
        register("Warehouse");
        report("был создан");
        addBehaviour(new ReservationBehaviour());
        addBehaviour(new CancelReservationBehaviour());
        addBehaviour(new DeleteBehaviour());
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику резервации продуктов со склада
     */
    private class ReservationBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = receive(mt);
            if (msg != null) {
                OperationProduct product = getObjectFromMsg(msg);
                boolean success = false;
                if (msg.getConversationId().equals("Reserving")) {
                    success = reserveProduct(product, Integer.parseInt(msg.getOntology()), false);
                } else if (msg.getConversationId().equals("Checking")) {
                    success = reserveProduct(product, -1, true);
                }
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                if (success) {
                    reply.setContent("success");
                } else {
                    reply.setContent("failure");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику возврата продуктов на склад
     */
    private class CancelReservationBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
            ACLMessage msg = receive(mt);
            if (msg != null) {
                for (var product : products) {
                    if (Integer.parseInt(msg.getConversationId()) == product.getId()) {
                        product.addQuantity(Double.parseDouble(msg.getContent()));
                        report(String.format("был возвращен продукт с id=%d в количестве=%.2f", product.getId(), Double.parseDouble(msg.getContent())));
                        break;
                    }
                }
            } else {
                block();
            }
        }
    }

    /**
     * Метод циклично списывает со склада необходимое количество различных продуктов заданного типа,
     * создает соответственных агентов продуктов, до тех пор пока не будет списан заданный объем
     * переданного типа продукта. В случае если флаг checking имеет значение true,
     * метод просто наличие переданного типа продукта в заданном объеме на складе
     * @param operationProduct - продукт операции
     * @param orderedDishID - id заказанного блюда
     * @param checking - выполнить ли только проверку наличия типа продукта в заданном объеме
     * @return - возвращает результат резервации/проверки
     */
    private boolean reserveProduct(OperationProduct operationProduct, int orderedDishID, boolean checking) {
        double totalQuantity = 0;
        ArrayList<Product> equalsProducts = new ArrayList<>();
        for (var product : products) {
            if (product.getType() == operationProduct.getType() && product.getQuantity() > 0) {
                equalsProducts.add(product);
                totalQuantity += product.getQuantity();
            }
        }
        if (totalQuantity < operationProduct.getQuantity()) {
            return false;
        }
        if (checking) {
            return true;
        }
        totalQuantity = 0;
        boolean stop = false;
        double lastQuantity;
        for (var product : equalsProducts) {
            lastQuantity = product.getQuantity();
            product.setQuantity(0);
            totalQuantity += lastQuantity;
            if (totalQuantity > operationProduct.getQuantity()) {
                product.setQuantity(totalQuantity - operationProduct.getQuantity());
                stop = true;
            }
            lastQuantity -= product.getQuantity();
            create(String.format("ProductAgent[%d](%s, %.2f%s)", orderedDishID, product.getName(), lastQuantity, product.getUnit()),
                    ProductAgent.class, orderedDishID, product.getId(), lastQuantity);
            if (stop) {
                break;
            }
        }
        return true;
    }
}
