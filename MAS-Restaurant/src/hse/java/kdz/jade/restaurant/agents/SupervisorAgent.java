package hse.java.kdz.jade.restaurant.agents;

import hse.java.kdz.jade.restaurant.types.DishData;
import hse.java.kdz.jade.restaurant.types.MenuDish;
import hse.java.kdz.jade.restaurant.types.VisitorOrderedDish;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.*;
import static java.util.Comparator.comparingDouble;
import static java.util.Comparator.comparingInt;

/**
 * Класс агента супервизора.
 * Поведения агента:
 * 1) Принятие заказов от агентов посетителей
 * 2) Управления очередностью приготовления блюд (оркестрация агентов блюд)
 */
public class SupervisorAgent extends RestaurantAgent {
    /**
     * Общее количество сделанных заказов
     */
    private int ordersCount = 0;

    @Override
    protected void setup() {
        register("Supervisor");
        report("был создан");
        addBehaviour(new OrderCreationBehaviour());
        addBehaviour(new KitchenManagementBehaviour());
        addBehaviour(new DeleteBehaviour());
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику создания заказа.
     * Создает агента заказа и агентов блюд для соответственного поступившего заказа
     */
    private class OrderCreationBehaviour extends CyclicBehaviour {
        private int step = 0;
        private ArrayList<VisitorOrderedDish> visitorOrderedDishes;
        private ArrayList<MenuDish> menuDishes;
        private String msgOntology;
        private ACLMessage visitorReply;
        MessageTemplate mt;
        ACLMessage msg;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Order-creation"));
                    msg = receive(mt);
                    if (msg != null) {
                        report("получил запрос на создание заказа (" + ordersCount + ")");
                        visitorReply = msg.createReply();
                        msgOntology = msg.getOntology();
                        visitorOrderedDishes = getObjectFromMsg(msg);
                        step = 1;
                    } else {
                        block();
                    }
                }
                case 1 -> {
                    AID menuAgent = find("Menu")[0];
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setConversationId("Menu-actualization");
                    msg.addReceiver(menuAgent);
                    send(msg);
                    step = 2;
                }
                case 2 -> {
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Menu-actualization"));
                    ACLMessage reply = receive(mt);
                    if (reply != null) {
                        menuDishes = getObjectFromMsg(reply);
                        step = 3;
                    } else {
                        block();
                    }
                }
                case 3 -> {
                    ArrayList<VisitorOrderedDish> visitorOrderedDishesAfterCheck = new ArrayList<>();
                    for (var orderedDish : visitorOrderedDishes) {
                        boolean success = false;
                        for (var menuDish : menuDishes) {
                            if (orderedDish.getDish() == menuDish.getId()) {
                                visitorOrderedDishesAfterCheck.add(orderedDish);
                                success = true;
                                break;
                            }
                        }
                        if (!success) {
                            report(String.format("блюдо '%s' было исключено из заказа (%d)",
                                    Objects.requireNonNull(findDishCardByMenuId(orderedDish.getDish())).getName(), ordersCount));
                        }
                    }
                    if (visitorOrderedDishesAfterCheck.size() == 0) {
                        report(String.format("В заказе (%d) 0 блюд. Создание заказа отменено", ordersCount));
                        visitorReply.setPerformative(ACLMessage.DISCONFIRM);
                        send(visitorReply);
                        step = 0;
                        break;
                    }
                    create(String.format("OrderAgent[%d]", ordersCount), OrderAgent.class, msgOntology, ordersCount, visitorOrderedDishesAfterCheck);
                    for (var orderedDish : visitorOrderedDishesAfterCheck) {
                        create(String.format("DishAgent[%d, order:%d]", orderedDish.getId(), ordersCount),
                                DishAgent.class, orderedDish.getId(), orderedDish.getDish(), ordersCount);
                    }
                    printOrderInformation(ordersCount, msgOntology, visitorOrderedDishesAfterCheck);
                    visitorReply.setPerformative(ACLMessage.CONFIRM);
                    setObjectToMsg(visitorReply, visitorOrderedDishesAfterCheck);
                    send(visitorReply);
                    ordersCount++;
                    step = 0;
                }
            }
        }
    }

    /**
     * Метод для красивого отформатированного вывода информации о поступившем заказе
     * с поддержкой вывода в зеленом цвете
     * @param orderID - id заказа в системе
     * @param visitorName - имя посетителя
     * @param visitorOrderedDishes - блюда, заказанные посетителем
     */
    private void printOrderInformation(int orderID, String visitorName, ArrayList<VisitorOrderedDish> visitorOrderedDishes) {
        ArrayList<String> rows = new ArrayList<>();
        rows.add("║ Номер заказа: " + orderID);
        rows.add("║ Дата создания: " + getFormattedDate(new Date()));
        rows.add("║ Посетитель: " + visitorName);
        rows.add("║ Стоимость: " + calculatePriceByVisitorOrderedDishes(visitorOrderedDishes));
        rows.add("║ Блюда:");
        for (var visitorOrderedDish : visitorOrderedDishes) {
            rows.add("║   -" + Objects.requireNonNull(findDishCardByMenuId(visitorOrderedDish.getDish())).getName());
        }
        int max = Collections.max(rows, comparingInt(String::length)).length();
        rows.replaceAll(s -> s + " ".repeat(max - s.length()) + " ║");
        StringBuilder result = new StringBuilder();
        result.append("╔═════════════╗").append("\n");
        result.append("║ НОВЫЙ ЗАКАЗ ║").append("\n");
        result.append("╠═════════════╩").append("═".repeat(max - "╠═════════════╩".length())).append("═╗").append("\n");
        for (var row : rows) {
            result.append(row).append("\n");
        }
        result.append("╚").append("═".repeat(max)).append("╝");
        if (PRINT_COLORED_REPORTS) {
            System.out.println("\u001B[32m" + result + "\u001B[0m");
        } else {
            System.out.println(result);
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику управления очередностью приготовления блюд.
     * Отвечает за то, чтобы повара и оборудование как можно меньше находились в простое,
     * но чтобы при этом блюда из ранних заказов также своевременного готовились.
     * Это достигается за счёт получения всей актуальной информации о времени от агента меню и
     * с помощью системы приоритетов: каждое не готовящееся блюдо имеет свой приоритет, который
     * повышается каждый раз, когда супервизор выбрал для готовки не 'это' блюдо.
     * Этот приоритет участвует во временных расчетах агента меню.
     */
    private class KitchenManagementBehaviour extends Behaviour {
        private int step = 0;
        private int cookingDishesCount = 0;
        private int repliesCount = 0;
        private AID[] dishAgents;
        private MessageTemplate mt;
        ACLMessage msg;
        private ArrayList<DishData> waitingDishesDataArray;


        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    dishAgents = find("Dish");
                    if (dishAgents.length == 0) {
                        step = 0;
                        break;
                    }
                    waitingDishesDataArray = new ArrayList<>();
                    cookingDishesCount = 0;
                    repliesCount = 0;
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setConversationId("Management");
                    msg.setReplyWith(String.valueOf(System.currentTimeMillis()));
                    for (var dishAgent : dishAgents) {
                        msg.addReceiver(dishAgent);
                    }
                    send(msg);
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Management"));
                    mt = MessageTemplate.and(mt, MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    step = 1;
                }
                case 1 -> {
                    msg = receive(mt);
                    if (msg != null) {
                        repliesCount++;
                        if (Objects.equals(msg.getOntology(), "DishCard")) {
                            waitingDishesDataArray.add(
                                    new DishData(getObjectFromMsg(msg), msg.getSender(), Integer.parseInt(msg.getProtocol())));
                        } else {
                            if (Objects.equals(msg.getContent(), "Cooking")) {
                                cookingDishesCount++;
                            }
                        }
                        if (repliesCount >= dishAgents.length) {
                            if (waitingDishesDataArray.size() == 0) {
                                step = 3;
                                break;
                            }
                            msg = new ACLMessage(ACLMessage.REQUEST);
                            msg.setConversationId("Time-calculation");
                            msg.setOntology("DishData");
                            msg.setReplyWith(String.valueOf(System.currentTimeMillis()));
                            setObjectToMsg(msg, waitingDishesDataArray);
                            msg.addReceiver(find("Menu")[0]);
                            send(msg);
                            mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                                    MessageTemplate.MatchConversationId("Time-calculation"));
                            mt = MessageTemplate.and(mt, MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                            step = 2;
                        }
                    } else {
                        block();
                    }
                }
                case 2 -> {
                    msg = receive(mt);
                    if (msg != null) {
                        waitingDishesDataArray = getObjectFromMsg(msg);
                        waitingDishesDataArray.sort(comparingDouble(DishData::getWaitingTime));
                        int activationsCount = Math.min(find("Cook").length - cookingDishesCount, waitingDishesDataArray.size());
                        if (activationsCount != 0) {
                            int i = 0;
                            msg = new ACLMessage(ACLMessage.REQUEST);
                            msg.setConversationId("Start-cooking");
                            for (; i < activationsCount; i++) {
                                msg.addReceiver(waitingDishesDataArray.get(i).getDishAgent());
                            }
                            send(msg);
                            msg = new ACLMessage(ACLMessage.REQUEST);
                            msg.setConversationId("Increase-priority");
                            for (; i < waitingDishesDataArray.size(); i++) {
                                msg.addReceiver(waitingDishesDataArray.get(i).getDishAgent());
                            }
                            msg.setContent(String.valueOf(activationsCount));
                            send(msg);
                        }
                        step = 3;
                    } else {
                        block();
                    }
                }
                case 3 -> {
                    addBehaviour(new WakerBehaviour(myAgent, KITCHEN_ACTUALIZED_STATUS_THRESHOLD) {
                        @Override
                        protected void onWake() {
                            addBehaviour(new KitchenManagementBehaviour());
                        }
                    });
                    step = 4;
                }
            }
        }

        @Override
        public boolean done() {
            return step == 4;
        }
    }
}
