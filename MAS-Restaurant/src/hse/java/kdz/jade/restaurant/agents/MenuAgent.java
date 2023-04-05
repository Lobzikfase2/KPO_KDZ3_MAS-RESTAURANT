package hse.java.kdz.jade.restaurant.agents;

import hse.java.kdz.jade.restaurant.tools.DataParser;
import hse.java.kdz.jade.restaurant.types.DishCard;
import hse.java.kdz.jade.restaurant.types.DishData;
import hse.java.kdz.jade.restaurant.types.MenuDish;
import hse.java.kdz.jade.restaurant.types.Operation;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAAgentManagement.Property;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.*;

/**
 * Класс агента меню.
 * Поведения агента:
 * 1) Предоставить актуальное меню
 * 2) Рассчитать оставшееся время приготовления цепочки из блюд/операций (по запросу от агента заказа/процесса)
 * 3) Рассчитать общее время простоя, необходимое для выполнения цепочки операций (по запросу от агента супервизора)
 */
public class MenuAgent extends RestaurantAgent {
    /**
     * Перечисление - статус, в котором может находиться меню ресторана
     */
    private enum Status {
        NOT_ACTUALIZED,
        ACTUALIZING,
        ACTUALIZED
    }

    /**
     * Доступные для приготовления на данный момент блюда из меню
     */
    private ArrayList<MenuDish> activeMenuDishes;
    /**
     * Время, оставшееся до конца резервации каждого отдельного повара
     */
    private ArrayList<Double> cookReservationTimes;
    /**
     * Время, оставшееся до конца резервации каждой отдельной единицы оборудования
     * Ключ - тип оборудования
     * Значение - массив времен оборудований заданного типа
     */
    private HashMap<Integer, ArrayList<Double>> equipmentReservationTimes;
    /**
     * Агент склада
     */
    private AID warehouseAgent;
    /**
     * Агенты поваров
     */
    private AID[] cookAgents;
    /**
     * Агенты оборудования
     */
    private AID[] equipmentAgents;
    /**
     * Количество блюд на проверке на возможность приготовления
     */
    private int checkingDishesCount;
    /**
     * Статус актуальности меню
     */
    private Status menuStatus;
    private Status resourcesReservationTimeStatus;

    @Override
    protected void setup() {
        warehouseAgent = find("Warehouse")[0];
        cookAgents = find("Cook");
        equipmentAgents = find("Equipment");
        menuStatus = Status.NOT_ACTUALIZED;
        resourcesReservationTimeStatus = Status.NOT_ACTUALIZED;
        register("Menu");
        report("был создан");
        addBehaviour(new TimeCalculationBehavior());
        addBehaviour(new ActualMenuProvidingBehaviour());
        addBehaviour(new DeleteBehaviour());
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику рассчитывания времени.
     * 1) Рассчитывает общее время простоя необходимое для приготовления блюда.
     * Данные расчеты используются агентом супервизора при выборе очередного блюда для готовки.
     * 2) Рассчитывает время до конца приготовления либо массива операций, либо цепочки из блюд
     * Данные расчеты используются агентами процесса и заказа в момент, когда посетитель запрашивает
     * оставшееся время до приготовления всего заказа
     */
    private class TimeCalculationBehavior extends CyclicBehaviour {
        private int step = 0;
        ACLMessage reply;
        private ACLMessage msg;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Time-calculation"));
                    msg = receive(mt);
                    if (msg != null) {
                        reply = msg.createReply();
                        if (resourcesReservationTimeStatus == Status.NOT_ACTUALIZED) {
                            addBehaviour(new ResourcePollingBehavior());
                        }
                        step = 1;
                    } else {
                        block();
                    }
                }
                case 1 -> {
                    if (resourcesReservationTimeStatus == Status.ACTUALIZED) {
                        if (msg.getOntology().equals("Operations")) {
                            ArrayList<Operation> operations = getObjectFromMsg(msg);
                            reply.setContent(String.valueOf(calculateOperationsTime(operations, false)));
                        } else if (msg.getOntology().equals("DishCards")) {
                            ArrayList<DishCard> dishCards = getObjectFromMsg(msg);
                            reply.setContent(String.valueOf(calculateDishesTime(dishCards)));
                        } else if (msg.getOntology().equals("DishData")) {
                            ArrayList<DishData> waitingDishesDataArray = getObjectFromMsg(msg);
                            for (var dishData : waitingDishesDataArray) {
                                double waitingTime = calculateOperationsTime(dishData.getDishCard().getOperations(), true);
                                waitingTime /= dishData.getPriority();
                                dishData.setWaitingTime(waitingTime);
                            }
                            setObjectToMsg(reply, waitingDishesDataArray);
                        }
                        send(reply);
                        step = 0;
                    }
                }
            }
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику опроса всех ресурсов ресторана на предмет оставшегося времени резервации
     */
    private class ResourcePollingBehavior extends Behaviour {
        private int step = 0;
        private int cookRepliesCount = 0;
        private int equipmentRepliesCount = 0;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    resourcesReservationTimeStatus = Status.ACTUALIZING;
                    cookReservationTimes = new ArrayList<>();
                    equipmentReservationTimes = new HashMap<>();
                    String replyWith = String.valueOf(System.currentTimeMillis());
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setConversationId("Cook-reserving");
                    msg.setReplyWith(replyWith);
                    for (var cookAgent : cookAgents) {
                        msg.addReceiver(cookAgent);
                    }
                    send(msg);
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setConversationId("Equipment-reserving");
                    msg.setReplyWith(replyWith);
                    for (var equipmentAgent : equipmentAgents) {
                        msg.addReceiver(equipmentAgent);
                    }
                    send(msg);
                    step = 1;
                }
                case 1 -> {
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                            MessageTemplate.or(MessageTemplate.MatchConversationId("Cook-reserving"), MessageTemplate.MatchConversationId("Equipment-reserving")));
                    ACLMessage msg = receive(mt);
                    if (msg != null) {
                        if (msg.getConversationId().equals("Cook-reserving")) {
                            cookReservationTimes.add(Double.parseDouble(msg.getContent()));
                            cookRepliesCount++;
                        } else {
                            int equipmentType = Integer.parseInt(msg.getOntology());
                            if (!equipmentReservationTimes.containsKey(equipmentType)) {
                                equipmentReservationTimes.put(equipmentType, new ArrayList<>());
                            }
                            equipmentReservationTimes.get(equipmentType).add(Double.parseDouble(msg.getContent()));
                            equipmentRepliesCount++;
                        }
                        if (cookRepliesCount >= cookAgents.length && equipmentRepliesCount >= equipmentAgents.length) {
                            step = 2;
                        }
                    } else {
                        block();
                    }
                }
                case 2 -> {
                    cookReservationTimes.sort(Comparator.naturalOrder());
                    for (var equipmentTimesArray : equipmentReservationTimes.values()) {
                        equipmentTimesArray.sort(Comparator.naturalOrder());
                    }
                    resourcesReservationTimeStatus = Status.ACTUALIZED;
                    addBehaviour(new WakerBehaviour(myAgent, KITCHEN_ACTUALIZED_STATUS_THRESHOLD) {
                        @Override
                        protected void onWake() {
                            resourcesReservationTimeStatus = Status.NOT_ACTUALIZED;
                        }
                    });
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
     * Метод для расчета времени операций, работающий в двух режимах:
     * 1) Метод рассчитывает общее время простоя кухни при выполнении серии операций
     * 2) Метод рассчитывает оставшееся время до конца выполнения серии операции
     * @param operations - массив операций
     * @param calculateWaitingTime - режим работы
     * @return - рассчитанное время
     */
    private double calculateOperationsTime(ArrayList<Operation> operations, boolean calculateWaitingTime) {
        if (operations.size() == 0) {
            return 0;
        }

        double waitingTime;
        if (operations.get(0).getEquipmentType() == -1) {
            waitingTime = cookReservationTimes.get(0);
        } else {
            waitingTime = Math.max(cookReservationTimes.get(0), equipmentReservationTimes.get(operations.get(0).getEquipmentType()).get(0));
        }

        double totalTime = waitingTime + operations.get(0).getTime() * SIMULATION_DECELERATION_FACTOR;
        double totalWaitingTime = waitingTime;

        for (int i = 1; i < operations.size(); i++) {
            double equipmentTime;
            if (operations.get(i).getEquipmentType() == -1) {
                equipmentTime = 0;
            } else {
                equipmentTime = equipmentReservationTimes.get(operations.get(i).getEquipmentType()).get(0);
            }
            waitingTime = equipmentTime - totalTime;
            if (waitingTime < 0) {
                waitingTime = 0;
            }
            totalTime += waitingTime + operations.get(i).getTime() * SIMULATION_DECELERATION_FACTOR;
            totalWaitingTime += waitingTime;
        }

        if (calculateWaitingTime) {
            return totalWaitingTime;
        }
        return totalTime;
    }

    /**
     * Метод для расчёта общего времени приготовления цепочки из блюд
     * @param dishCards - массив карточек блюд
     * @return - рассчитанное время
     */
    private double calculateDishesTime(ArrayList<DishCard> dishCards) {
        if (dishCards.size() == 0) {
            return 0;
        }

        double waitingTime;
        ArrayList<Double> cookReservationTimesLocal = new ArrayList<>(cookReservationTimes);
        HashMap<Integer, ArrayList<Double>> equipmentReservationTimesLocal = copyEquipmentHashMap(equipmentReservationTimes);
        ArrayList<Double> totalTimes = new ArrayList<>();

        for (int i = 0; i < dishCards.size(); i++) {
            ArrayList<Operation> operations = dishCards.get(i).getOperations();
            for (int j = 0; j < operations.size(); j++) {
                double equipmentTime;
                if (operations.get(j).getEquipmentType() == -1) {
                    equipmentTime = 0;
                } else {
                    equipmentTime = equipmentReservationTimesLocal.get(operations.get(j).getEquipmentType()).get(0);
                }
                if (j == 0) {
                    waitingTime = Math.max(cookReservationTimesLocal.get(0), equipmentTime);
                    totalTimes.add(waitingTime + operations.get(j).getTime() * SIMULATION_DECELERATION_FACTOR);
                } else {
                    waitingTime = equipmentTime - totalTimes.get(i);
                    if (waitingTime < 0) {
                        waitingTime = 0;
                    }
                    totalTimes.set(i, totalTimes.get(i) + waitingTime + operations.get(j).getTime() * SIMULATION_DECELERATION_FACTOR);
                }
                cookReservationTimesLocal.set(0, cookReservationTimesLocal.get(0) + operations.get(j).getTime() * SIMULATION_DECELERATION_FACTOR);
                if (operations.get(j).getEquipmentType() != -1) {
                    equipmentReservationTimesLocal.get(operations.get(j).getEquipmentType()).set(0,
                            equipmentReservationTimesLocal.get(operations.get(j).getEquipmentType()).get(0) + operations.get(j).getTime() * SIMULATION_DECELERATION_FACTOR);
                }
            }
            cookReservationTimesLocal.sort(Comparator.naturalOrder());
            for (var equipmentTimesArray : equipmentReservationTimesLocal.values()) {
                equipmentTimesArray.sort(Comparator.naturalOrder());
            }
        }
        return Collections.max(totalTimes);
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику предоставления актуального меню
     */
    private class ActualMenuProvidingBehaviour extends CyclicBehaviour {
        private int step = 0;
        ACLMessage reply;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                            MessageTemplate.MatchConversationId("Menu-actualization"));
                    ACLMessage msg = receive(mt);
                    if (msg != null) {
                        reply = msg.createReply();
                        if (menuStatus != Status.ACTUALIZING) {
                            addBehaviour(new MenuActualizationBehaviour());
                        }
                        step = 1;
                    } else {
                        block();
                    }
                }
                case 1 -> {
                    if (menuStatus == Status.ACTUALIZED) {
                        setObjectToMsg(reply, activeMenuDishes);
                        myAgent.send(reply);
                        step = 0;
                    }
                }
            }
        }
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику актуализации меню
     */
    private class MenuActualizationBehaviour extends Behaviour {
        private int step = 0;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    report("начинаю актуализацию меню...");
                    activeMenuDishes = new ArrayList<>();
                    checkingDishesCount = DataParser.Data.menuDishes.size();
                    menuStatus = Status.ACTUALIZING;
                    for (var menuDish : DataParser.Data.menuDishes) {
                        myAgent.addBehaviour(new DishCheckingBehaviour(menuDish));
                    }
                    step = 1;
                }
                case 1 -> {
                    if (checkingDishesCount == 0) {
                        report("актуализация меню завершена");
                        menuStatus = Status.ACTUALIZED;
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
     * Поведение описывает логику проверку блюда на возможность его приготовить в данный момент
     */
    private class DishCheckingBehaviour extends Behaviour {
        private final MenuDish menuDish;
        private final DishCard dishCard;
        private int step = 0;
        private int msgCount = 0;
        private int repliesCount = 0;
        boolean failure = false;
        MessageTemplate mt;

        public DishCheckingBehaviour(MenuDish menuDish) {
            this.menuDish = menuDish;
            dishCard = findDishCardByMenuId(menuDish.getId());
            if (!menuDish.isActive() || dishCard == null) {
                step = 5;
                checkingDishesCount--;
            }
        }

        @Override
        public void action() {
            switch (step) {
                case 0:
                    if (CHECK_OPERATION_TYPE_AVAILABILITY) {
                        for (var operation : dishCard.getOperations()) {
                            boolean success = false;
                            for (var operationType : DataParser.Data.operationTypes) {
                                if (operation.getType() == operationType.getId()) {
                                    success = true;
                                    break;
                                }
                            }
                            if (!success) {
                                report(String.format("блюдо '%s' было исключено из меню. Причина: не все типы операций присутствуют на кухне", dishCard.getName()));
                                checkingDishesCount--;
                                step = 5;
                                break;
                            }
                        }
                    }
                case 1:
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(warehouseAgent);
                    msg.setConversationId("Checking");
                    msg.setReplyWith(String.valueOf(dishCard.getId()));
                    for (var operation : dishCard.getOperations()) {
                        for (var product : operation.getProducts()) {
                            setObjectToMsg(msg, product);
                            send(msg);
                            msgCount++;
                        }
                    }
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchConversationId("Checking"));
                    mt = MessageTemplate.and(mt, MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                    step = 2;
                    break;
                case 2:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getContent().equals("failure")) {
                            failure = true;
                        }
                        repliesCount++;
                        if (repliesCount >= msgCount) {
                            step = 3;
                        }
                    } else {
                        block();
                    }
                    break;
                case 3:
                    if (failure) {
                        report(String.format("блюдо '%s' было исключено из меню. Причина: не все продукты есть на складе", dishCard.getName()));
                        checkingDishesCount--;
                        step = 5;
                    } else {
                        for (var operation : dishCard.getOperations()) {
                            if (operation.getEquipmentType() >= 0) {
                                AID[] agents = find("Equipment", new Property("equipmentType", operation.getEquipmentType()));
                                if (agents.length == 0) {
                                    failure = true;
                                }
                            }
                        }
                        if (failure) {
                            report(String.format("блюдо '%s' было исключено из меню. Причина: не все типы оборудования есть на кухне", dishCard.getName()));
                            checkingDishesCount--;
                            step = 5;
                        } else {
                            if (resourcesReservationTimeStatus == Status.NOT_ACTUALIZED) {
                                addBehaviour(new ResourcePollingBehavior());
                            }
                            step = 4;
                        }
                    }
                    break;
                case 4:
                    if (resourcesReservationTimeStatus == Status.ACTUALIZED) {
                        ArrayList<DishCard> wrappedDishCard = new ArrayList<>(1);
                        wrappedDishCard.add(dishCard);
                        if (calculateDishesTime(wrappedDishCard) < DISH_COOKING_TIME_THRESHOLD) {
                            activeMenuDishes.add(menuDish);
                        } else {
                            report(String.format("блюдо '%s' было исключено из меню. Причина: максимально допустимое время готовки превысило порог", dishCard.getName()));
                            System.out.println("Отменяю добавление блюда");
                        }
                        checkingDishesCount--;
                        step = 5;
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            return step == 5;
        }
    }
}
