package hse.java.kdz.jade.restaurant.agents;

import hse.java.kdz.jade.restaurant.tools.DataParser;
import hse.java.kdz.jade.restaurant.types.VisitorOrder;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.FIPAAgentManagement.Property;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.*;

/**
 * Класс агента симуляции.
 * Отвечает за проведение всей симуляции работы ресторана
 * Поведения агента:
 * 1) Создание всех агентов ресторана за исключением агентов заказов и агентов производных от них
 * 2) Управление потоком посетителей
 * 3) Отслеживание завершения обслуживания посетителей
 * 4) Удаление всех оставшихся в системе агентов
 * 5) Завершение симуляции
 */
public class SimulationAgent extends RestaurantAgent {
    /**
     * Заказы посетителей
     */
    private final ArrayList<VisitorOrder> visitorOrders = new ArrayList<>(DataParser.Data.visitorOrders);

    @Override
    protected void setup() {
        int activeResources = 0;
        register("Simulation");
        report("был создан");
        report("создаю агентов ресторана...");
        for (var cook : DataParser.Data.cooks) {
            if (cook.isActive()) {
                activeResources++;
                create(String.format("CookAgent[%d](%s)", cook.getId(), cook.getName()), CookAgent.class, cook);
            }
        }
        for (var equipment : DataParser.Data.equipments) {
            if (equipment.isActive()) {
                activeResources++;
                create(String.format("EquipmentAgent[%d](%s)", equipment.getId(), equipment.getName()), EquipmentAgent.class, equipment);
            }
        }
        create("WarehouseAgent", WarehouseAgent.class);
        while (find("Cook").length + find("Equipment").length + find("Warehouse").length < activeResources + 1) {
            sleep(250);
        }
        create("MenuAgent", MenuAgent.class);
        create("SupervisorAgent", SupervisorAgent.class);
        while (find("Menu").length + find("Supervisor").length < 2) {
            sleep(250);
        }
        sleep(1000);
        System.out.println("=====================НАЧАЛО РАБОТЫ РЕСТОРАНА=====================");
        addBehaviour(new VisitorsFlowSimulationBehaviour());
        addBehaviour(new SimulationCompletionBehaviour());
    }

    /**
     * Класс поведения агента.
     * Поведение описывает логику потока посетителей.
     * Создает агентов посетителей одного за другим с произвольной задержкой между итерациями
     */
    private class VisitorsFlowSimulationBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            if (visitorOrders.size() > 0) {
                report("===новый посетитель!===");
                VisitorOrder visitorOrder = visitorOrders.remove(0);
                create("Visitor(" + visitorOrder.getVisitorName() + ")", VisitorAgent.class, visitorOrder);
                int newVisitorIn = rand.nextInt((MAX_NEW_VISITOR_DELAY - MIN_NEW_VISITOR_DELAY) + 1) + MIN_NEW_VISITOR_DELAY;
                addBehaviour(new WakerBehaviour(myAgent, newVisitorIn) {
                    @Override
                    protected void onWake() {
                        addBehaviour(new VisitorsFlowSimulationBehaviour());
                    }
                });
            }
        }
    }


    /**
     * Класс поведения агента.
     * Поведение описывает логику отслеживания завершения работы ресторана
     * и последующего завершения симуляции
     */
    private class SimulationCompletionBehaviour extends Behaviour {
        private int step = 0;
        private int repliesCount = 0;
        MessageTemplate mt;
        ACLMessage msg;

        @Override
        public void action() {
            switch (step) {
                case 0 -> {
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                            MessageTemplate.MatchConversationId("Visitor-end"));
                    msg = receive(mt);
                    if (msg != null) {
                        VisitorOrder visitorOrder = getObjectFromMsg(msg);
                        AID[] orderAgent = find("Order", new Property("visitorName", visitorOrder.getVisitorName()));
                        if (orderAgent.length > 0) {
                            msg = new ACLMessage(ACLMessage.REQUEST);
                            msg.setConversationId("delete");
                            msg.addReceiver(orderAgent[0]);
                            for (var visitorOrderedDish : visitorOrder.getDishes()) {
                                for (var dishAgent : find("Dish", new Property("orderedDishID", visitorOrderedDish.getId()))) {
                                    msg.addReceiver(dishAgent);
                                }
                                AID[] processAgent = find("Process", new Property("orderedDishID", visitorOrderedDish.getId()));
                                if (processAgent.length > 0) {
                                    msg.addReceiver(processAgent[0]);
                                }
                                for (var operationAgent : find("Operation", new Property("orderedDishID", visitorOrderedDish.getId()))) {
                                    msg.addReceiver(operationAgent);
                                }
                                for (var productAgent : find("Product", new Property("orderedDishID", visitorOrderedDish.getId()))) {
                                    msg.addReceiver(productAgent);
                                }
                            }
                            send(msg);
                        }
                        repliesCount++;
                        if (repliesCount >= DataParser.Data.visitorOrders.size()) {
                            System.out.println("====================РАБОТА РЕСТОРАНА ЗАВЕРШЕНА===================");
                            step = 1;
                        }
                    } else {
                        block();
                    }
                }
                case 1 -> {
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setConversationId("delete");
                    for (var agents : find("Order")) {
                        msg.addReceiver(agents);
                    }
                    for (var agents : find("Dish")) {
                        msg.addReceiver(agents);
                    }
                    for (var agents : find("Process")) {
                        msg.addReceiver(agents);
                    }
                    for (var agents : find("Operation")) {
                        msg.addReceiver(agents);
                    }
                    for (var agents : find("Cook")) {
                        msg.addReceiver(agents);
                    }
                    for (var agents : find("Equipment")) {
                        msg.addReceiver(agents);
                    }
                    for (var agents : find("Product")) {
                        msg.addReceiver(agents);
                    }
                    for (var agents : find("Warehouse")) {
                        msg.addReceiver(agents);
                    }
                    for (var agents : find("Menu")) {
                        msg.addReceiver(agents);
                    }
                    for (var agents : find("Supervisor")) {
                        msg.addReceiver(agents);
                    }
                    send(msg);
                    sleep(2000);
                    int activeAgentsCount = 0;
                    activeAgentsCount += find("Cook").length;
                    activeAgentsCount += find("Equipment").length;
                    activeAgentsCount += find("Warehouse").length;
                    activeAgentsCount += find("Menu").length;
                    activeAgentsCount += find("Supervisor").length;
                    activeAgentsCount += find("Order").length;
                    activeAgentsCount += find("Dish").length;
                    activeAgentsCount += find("Process").length;
                    activeAgentsCount += find("Operation").length;
                    activeAgentsCount += find("Product").length;
                    if (activeAgentsCount == 0) {
                        writeLogData();
                        step = 2;
                        doDelete();
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
     * Метод переопределяет базовое поведение агента при его удалении.
     * Агент симуляции завершает работу платформы JADE
     */
    @Override
    protected void takeDown() {
        super.takeDown();
        stopSimulation(this);
    }
}
