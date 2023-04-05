package hse.java.kdz.jade.restaurant.tools;

import hse.java.kdz.jade.restaurant.agents.SimulationAgent;
import hse.java.kdz.jade.restaurant.types.*;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.ProfileImpl;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;
import org.json.simple.JSONObject;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static hse.java.kdz.jade.restaurant.RestaurantSimulationTest.isOptionsFromFile;
import static hse.java.kdz.jade.restaurant.tools.DataParser.getOptionsAsJSON;
import static java.util.Comparator.comparingInt;

/**
 * Класс вспомогательных инструментов для агентов ресторана.
 * Также содержит в себе все глобальные константы проекта
 * и методы управления системой JADE
 */
public class AgentTools {
    /**
     * Коэффициент замедления всех систем, участвующих в симуляции
     * Рекомендуемые значения от 1 до 8
     * Пороговые значения: 1-100+
     * В случаях заморозок всей симуляции посреди работы,
     * или критических ошибок платформы JADE,
     * изменение этого параметра может помочь исправить проблему
     */
    public final static int SIMULATION_DECELERATION_FACTOR;
    /**
     * Порог теоретического времени приготовления блюда, после которого оно исключается из меню
     * Все нижеуказанные временные константы умножаются на этот коэффициент!
     */
    public final static double DISH_COOKING_TIME_THRESHOLD;
    /**
     * Время, в течение которого информация, полученная супервизором от всех агентов на кухне,
     * перестает считаться актуальной
     */
    public final static int KITCHEN_ACTUALIZED_STATUS_THRESHOLD;
    /**
     * Минимальное время ожидания между приходом очередного посетителя (мс)
     */
    public final static int MIN_NEW_VISITOR_DELAY;
    /**
     * Максимальное время ожидания между приходом очередного посетителя (мс)
     */
    public final static int MAX_NEW_VISITOR_DELAY;
    /**
     * Вероятность отмены заказа посетителем (в процентах) [0-100]
     */
    public final static int ORDER_CANCELLATION_PROBABILITY;
    /**
     * Минимальное время ожидания после которого клиент может сделать отмену заказа (мс)
     */
    public final static int MIN_ORDER_CANCELLATION_DELAY;
    /**
     * Максимальное время ожидания после которого клиент может сделать отмену заказа (мс)
     */
    public final static int MAX_ORDER_CANCELLATION_DELAY;
    /**
     * Минимальное время ожидания между узнаваниями посетителем времени до готовности заказа (мс)
     */
    public final static int MIN_ORDER_TIME_RECOGNITION_DELAY;
    /**
     * Максимальное время ожидания между узнаваниями посетителем времени до готовности заказа (мс)
     */
    public final static int MAX_ORDER_TIME_RECOGNITION_DELAY;
    /**
     * Время, после которого клиент уйдет, не дождавшись заказа (мс)
     */
    public final static int VISITOR_WAITING_TIME;
    /**
     * Проверять ли наличие типа операции в файле operation_types для каждой операции?
     * Если значение параметра равно false, то данный файл не требуется для запуска программы
     */
    public final static boolean CHECK_OPERATION_TYPE_AVAILABILITY;
    /**
     * Включить печать цветными символами?
     * НЕ РАБОТАЕТ НА WINDOWS
     * (если в консольном выводе присутствуют нечитаемые символы, её нужно отключить)
     */
    public final static boolean PRINT_COLORED_REPORTS;
    //--------------------------------КОНФИГУРАЦИЯ ПЛАТФОРМЫ JADE--------------------------------
    /**
     * Отображать ли GUI платформы JADE?
     */
    public final static boolean SHOW_GUI;
    /**
     * Количество потоков, которые будут обрабатывать сообщения
     */
    public final static long JADE_MESSAGE_MANAGER_POOL_SIZE;
    /**
     * Максимальный размер очереди сообщений агента
     */
    public final static long JADE_MESSAGE_MANAGER_MAX_QUEUE_SIZE;
    /**
     * Порог доставки сообщений между агентами (мс)
     */
    public final static long JADE_MESSAGE_MANAGER_DELIVERY_TIME_THRESHOLD;
    /**
     * Порог доставки сообщений между агентами 2 (мс)
     */
    public final static long JADE_MESSAGE_MANAGER_DELIVERY_TIME_THRESHOLD2;
    /**
     * Порог поиска агента в сервисе желтых страниц (мс)
     */
    public final static long JADE_DF_SERVICE_SEARCH_TIMEOUT;
    /**
     * Порт, на котором запускается платформа
     */
    public final static long JADE_PLATFORM_PORT;

    static {
        if (isOptionsFromFile()) {
            JSONObject options = getOptionsAsJSON();
            SIMULATION_DECELERATION_FACTOR = Integer.parseInt(options.get("SIMULATION_DECELERATION_FACTOR").toString());
            DISH_COOKING_TIME_THRESHOLD = Double.parseDouble(options.get("DISH_COOKING_TIME_THRESHOLD").toString()) * SIMULATION_DECELERATION_FACTOR;
            KITCHEN_ACTUALIZED_STATUS_THRESHOLD = Integer.parseInt(options.get("KITCHEN_ACTUALIZED_STATUS_THRESHOLD").toString()) * SIMULATION_DECELERATION_FACTOR;
            MIN_NEW_VISITOR_DELAY = Integer.parseInt(options.get("MIN_NEW_VISITOR_DELAY").toString()) * SIMULATION_DECELERATION_FACTOR;
            MAX_NEW_VISITOR_DELAY = Integer.parseInt(options.get("MAX_NEW_VISITOR_DELAY").toString()) * SIMULATION_DECELERATION_FACTOR;
            ORDER_CANCELLATION_PROBABILITY = Integer.parseInt(options.get("ORDER_CANCELLATION_PROBABILITY").toString());
            MIN_ORDER_CANCELLATION_DELAY = Integer.parseInt(options.get("MIN_ORDER_CANCELLATION_DELAY").toString()) * SIMULATION_DECELERATION_FACTOR;
            MAX_ORDER_CANCELLATION_DELAY = Integer.parseInt(options.get("MAX_ORDER_CANCELLATION_DELAY").toString()) * SIMULATION_DECELERATION_FACTOR;
            MIN_ORDER_TIME_RECOGNITION_DELAY = Integer.parseInt(options.get("MIN_ORDER_TIME_RECOGNITION_DELAY").toString()) * SIMULATION_DECELERATION_FACTOR;
            MAX_ORDER_TIME_RECOGNITION_DELAY = Integer.parseInt(options.get("MAX_ORDER_TIME_RECOGNITION_DELAY").toString()) * SIMULATION_DECELERATION_FACTOR;
            VISITOR_WAITING_TIME = Integer.parseInt(options.get("VISITOR_WAITING_TIME").toString()) * SIMULATION_DECELERATION_FACTOR;
            CHECK_OPERATION_TYPE_AVAILABILITY = (boolean) options.get("CHECK_OPERATION_TYPE_AVAILABILITY");
            PRINT_COLORED_REPORTS = (boolean) options.get("PRINT_COLORED_REPORTS");
            SHOW_GUI = (boolean) options.get("SHOW_GUI");
            JADE_MESSAGE_MANAGER_POOL_SIZE = (long) options.get("JADE_MESSAGE_MANAGER_POOL_SIZE");
            JADE_MESSAGE_MANAGER_MAX_QUEUE_SIZE = (long) options.get("JADE_MESSAGE_MANAGER_MAX_QUEUE_SIZE");
            JADE_MESSAGE_MANAGER_DELIVERY_TIME_THRESHOLD = (long) options.get("JADE_MESSAGE_MANAGER_DELIVERY_TIME_THRESHOLD");
            JADE_MESSAGE_MANAGER_DELIVERY_TIME_THRESHOLD2 = (long) options.get("JADE_MESSAGE_MANAGER_DELIVERY_TIME_THRESHOLD2");
            JADE_DF_SERVICE_SEARCH_TIMEOUT = (long) options.get("JADE_DF_SERVICE_SEARCH_TIMEOUT");
            JADE_PLATFORM_PORT = Integer.parseInt(options.get("JADE_PLATFORM_PORT").toString());
        } else {
            SIMULATION_DECELERATION_FACTOR = 1;
            DISH_COOKING_TIME_THRESHOLD = 3 * SIMULATION_DECELERATION_FACTOR;
            KITCHEN_ACTUALIZED_STATUS_THRESHOLD = 500 * SIMULATION_DECELERATION_FACTOR;
            MIN_NEW_VISITOR_DELAY = 250 * SIMULATION_DECELERATION_FACTOR;
            MAX_NEW_VISITOR_DELAY = 1000 * SIMULATION_DECELERATION_FACTOR;
            ORDER_CANCELLATION_PROBABILITY = 25;
            MIN_ORDER_CANCELLATION_DELAY = 500 * SIMULATION_DECELERATION_FACTOR;
            MAX_ORDER_CANCELLATION_DELAY = 1000 * SIMULATION_DECELERATION_FACTOR;
            MIN_ORDER_TIME_RECOGNITION_DELAY = 300 * SIMULATION_DECELERATION_FACTOR;
            MAX_ORDER_TIME_RECOGNITION_DELAY = 2000 * SIMULATION_DECELERATION_FACTOR;
            VISITOR_WAITING_TIME = 10000 * SIMULATION_DECELERATION_FACTOR;
            CHECK_OPERATION_TYPE_AVAILABILITY = true;
            PRINT_COLORED_REPORTS = false;
            SHOW_GUI = true;
            JADE_MESSAGE_MANAGER_POOL_SIZE = 1000;
            JADE_MESSAGE_MANAGER_MAX_QUEUE_SIZE = 500000000;
            JADE_MESSAGE_MANAGER_DELIVERY_TIME_THRESHOLD = 5000;
            JADE_MESSAGE_MANAGER_DELIVERY_TIME_THRESHOLD2 = 5000;
            JADE_DF_SERVICE_SEARCH_TIMEOUT = 10000;
            JADE_PLATFORM_PORT = 8858;
        }
    }

    public final static Random rand = new Random();
    /**
     * Общее количество процессов
     */
    private static int processesCount = 0;
    /**
     * Общее количество операций
     */
    private static int operationsCount = 0;
    private static jade.core.Runtime rt;
    private static AgentContainer agentContainer;
    /**
     * Отчеты посетителей
     */
    private final static ArrayList<VisitorOrder> visitorsOrdersReports = new ArrayList<>();
    /**
     * Отчеты процессов
     */
    private final static ArrayList<ProcessReport> processesReports = new ArrayList<>();
    /**
     * Отчеты операций
     */
    private final static ArrayList<OperationReport> operationsReports = new ArrayList<>();

    /**
     * Метод запускает платформу JADE и создаёт в ней агента симуляции
     */
    public static void startSimulation() {
        rt = jade.core.Runtime.instance();
        Properties properties = new Properties();
        properties.put("local-port", String.valueOf(JADE_PLATFORM_PORT));
        properties.put("port", String.valueOf(JADE_PLATFORM_PORT));
        properties.put("host", "127.0.0.1");
        properties.put("local-host", "127.0.0.1");
        properties.put("jade_core_messaging_MessageManager_poolsize", String.valueOf(JADE_MESSAGE_MANAGER_POOL_SIZE));
        properties.put("jade_core_messaging_MessageManager_maxqueuesize", String.valueOf(JADE_MESSAGE_MANAGER_MAX_QUEUE_SIZE));
        properties.put("jade_core_messaging_MessageManager_deliverytimethreshold", String.valueOf(JADE_MESSAGE_MANAGER_DELIVERY_TIME_THRESHOLD));
        properties.put("jade_core_messaging_MessageManager_deliverytimethreshold2", String.valueOf(JADE_MESSAGE_MANAGER_DELIVERY_TIME_THRESHOLD2));
        properties.put("jade_domain_dfservice_searchtimeout", String.valueOf(JADE_DF_SERVICE_SEARCH_TIMEOUT));
        properties.put("gui", String.valueOf(SHOW_GUI));
        ProfileImpl p = new ProfileImpl(properties);
        rt.setCloseVM(true);
        agentContainer = rt.createMainContainer(p);
        System.out.println("------------------------запуск симуляции!------------------------");
        try {
            agentContainer.createNewAgent("SimulationAgent", SimulationAgent.class.getName(), null).start();
        } catch (StaleProxyException e) {
            System.out.println("Не удалось создать агента симуляции:");
            e.printStackTrace();
            System.out.println("Завершение работы...");
            System.exit(0);
        }
    }

    /**
     * Метод завершает работу платформы JADE и самой программы
     * @param myAgent - любой агент платформы
     */
    public static void stopSimulation(Agent myAgent) {
        if (rt != null) {
            Codec codec = new SLCodec();
            Ontology jmo = JADEManagementOntology.getInstance();
            ContentManager contentManager = myAgent.getContentManager();
            contentManager.registerLanguage(codec);
            contentManager.registerOntology(jmo);
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(myAgent.getAMS());
            msg.setLanguage(codec.getName());
            msg.setOntology(jmo.getName());
            String report = "----------------------симуляция завершена!-----------------------";
            try {
                contentManager.fillContent(msg, new Action(myAgent.getAID(), new ShutdownPlatform()));
                myAgent.send(msg);
            } catch (Exception e) {
                System.out.println(report);
                rt.shutDown();
                System.exit(0);
            }
            System.out.println(report);
            rt.shutDown();
        }
        System.exit(0);
    }

    /**
     * Метод записывает все отчёты в соответственные файлы
     */
    public static void writeLogData() {
        visitorsOrdersReports.sort(Comparator.comparing(VisitorOrder::getStarted));
        processesReports.sort(Comparator.comparing(ProcessReport::getStarted));
        operationsReports.sort(Comparator.comparing(OperationReport::getStarted));
        DataParser.writeJSON("visitor_order_log", visitorsOrdersReports);
        DataParser.writeJSON("process_log", processesReports);
        DataParser.writeJSON("operation_log", operationsReports);
    }

    /**
     * Метод для поиска карточки блюда по его id в меню
     * @param menuID - id блюда в меню
     */
    public static DishCard findDishCardByMenuId(int menuID) {
        for (var menuDish : DataParser.Data.menuDishes) {
            if (menuDish.getId() == menuID) {
                for (var dishCard : DataParser.Data.dishCards) {
                    if (dishCard.getId() == menuDish.getCard()) {
                        return dishCard;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Метод для поиска названия типа операции по его id
     * @param typeId - id типа операции
     * @return - название типа операции
     */
    public static String findOperationNameByType(int typeId) {
        for (var operationType : DataParser.Data.operationTypes) {
            if (operationType.getId() == typeId) {
                return operationType.getName();
            }
        }
        return "";
    }

    /**
     * Метод для подсчёта итоговой стоимости заказа
     * @param visitorOrderedDishes - заказанные блюда
     * @return - итоговая цена
     */
    public static int calculatePriceByVisitorOrderedDishes(ArrayList<VisitorOrderedDish> visitorOrderedDishes) {
        int totalCost = 0;
        for (var visitorOrderedDish : visitorOrderedDishes) {
            for (var menuDish : DataParser.Data.menuDishes) {
                if (visitorOrderedDish.getDish() == menuDish.getId()) {
                    totalCost += menuDish.getPrice();
                    break;
                }
            }
        }
        return totalCost;
    }

    /**
     * Метод, копирующий структуру для хранения времени всего оборудования на кухне
     */
    public static HashMap<Integer, ArrayList<Double>> copyEquipmentHashMap(HashMap<Integer, ArrayList<Double>> source) {
        HashMap<Integer, ArrayList<Double>> copy = new HashMap<>();
        for (Map.Entry<Integer, ArrayList<Double>> entry : source.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Метод для форматирования времени для вывода в консоль
     * @param time - время
     * @return - отформатированное время
     */
    public static String formatTime(double time) {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(time);
    }

    /**
     * Метод для преобразования даты в строку заданного формата
     * @param date - объект даты
     * @return - отформатированный результат
     */
    public static String getFormattedDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd").format(date) + "T" + new SimpleDateFormat("HH:mm:ss").format(date);
    }

    /**
     * Метод для отформатированного консольного вывода заданного цвета
     * @param color - цвет символов
     * @param texts - выводимый массив строк
     */
    public static void formattedReport(String color, String... texts) {
        StringBuilder result = new StringBuilder();
        int max = Collections.max(Arrays.stream(texts).toList(), comparingInt(String::length)).length() + 1;
        result.append("╔").append("═".repeat(max + 1)).append("╗").append("\n");
        for (var text : texts) {
            String row = " " + text;
            result.append("║").append(row).append(" ".repeat(max - row.length())).append(" ║").append("\n");
        }
        result.append("╚").append("═".repeat(max + 1)).append("╝");

        String code = switch (color) {
            case "red" -> "\u001B[31m";
            case "yellow" -> "\u001B[33m";
            case "blue" -> "\u001B[36m";
            case "green" -> "\u001B[32m";
            default -> "";
        };
        if (PRINT_COLORED_REPORTS) {
            System.out.println(code + result + "\u001B[0m");
        } else {
            System.out.println(result);
        }
    }

    public static AgentContainer getAgentContainer() {
        return agentContainer;
    }

    public static synchronized int getAndIncrementProcessesCount() {
        return processesCount++;
    }

    public static synchronized int getAndIncrementOperationsCount() {
        return operationsCount++;
    }

    public static synchronized void addVisitorOrderReport(VisitorOrder visitorOrderReport) {
        visitorOrderReport.formatDates();
        visitorsOrdersReports.add(visitorOrderReport);
    }

    public static synchronized void addProcessReport(ProcessReport processReport) {
        processReport.formatDates();
        processesReports.add(processReport);
    }

    public static synchronized void addOperationReport(OperationReport operationReport) {
        operationReport.formatDates();
        operationsReports.add(operationReport);
    }
}
