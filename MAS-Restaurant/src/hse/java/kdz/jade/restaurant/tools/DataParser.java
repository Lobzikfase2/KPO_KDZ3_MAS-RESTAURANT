package hse.java.kdz.jade.restaurant.tools;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import hse.java.kdz.jade.restaurant.types.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static hse.java.kdz.jade.restaurant.RestaurantSimulationTest.getInputPath;
import static hse.java.kdz.jade.restaurant.RestaurantSimulationTest.getOutputPath;
import static hse.java.kdz.jade.restaurant.tools.AgentTools.CHECK_OPERATION_TYPE_AVAILABILITY;


/**
 * Класс для работы с входными и выходными файлами JSON файлами
 */
public class DataParser {
    private static final Gson g = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getAnnotation(Expose.class) != null;
        }

        @Override
        public boolean shouldSkipClass(Class<?> c) {
            return false;
        }
    }).create();

    private static JSONArray getJSONArray(String fileName) {
        String fullPath = getInputPath() + fileName + ".json";
        try {
            JSONObject j = (JSONObject) new JSONParser().parse(new FileReader(fullPath));
            return (JSONArray) j.get(fileName);
        } catch (IOException e) {
            System.out.printf("Не удалось открыть файл '%s'%n", fullPath);
            System.exit(1);
        } catch (ParseException e) {
            System.out.printf("Ошибка при обработке файла '%s.json'%n", fileName);
            System.exit(1);
        }
        return null;
    }

    private static <T> ArrayList<T> readJSON(String fileName, Class<T> tClass) {
        ArrayList<T> array = new ArrayList<>();
        for (var element : getJSONArray(fileName)) {
            array.add(g.fromJson(element.toString(), tClass));
        }
        return array;
    }

    /**
     * Метод позволяет получить JSON объект настроек из файла options.json по входному пути,
     * переданному в аргументах командной
     *
     * @return - JSON объект настроек
     */
    public static JSONObject getOptionsAsJSON() {
        String fullPath = getInputPath() + "options.json";
        try {
            return (JSONObject) new JSONParser().parse(new FileReader(fullPath));
        } catch (IOException e) {
            System.out.printf("Не удалось открыть файл '%s'%n", fullPath);
            System.exit(1);
        } catch (ParseException e) {
            System.out.println("Ошибка при обработке файла 'options.json'");
            System.exit(1);
        }
        return null;
    }

    /**
     * Метод позволяет создать лог файл отчета заданного типа
     *
     * @param fileName     - имя файла/отчёта
     * @param reportsArray - массив объектов отчета
     * @param <T>          - тип объекта отчета
     */
    public static <T> void writeJSON(String fileName, ArrayList<T> reportsArray) {
        JsonElement json = g.toJsonTree(reportsArray);
        JsonObject finalOutput = new JsonObject();
        finalOutput.add(fileName, json);
        File file = new File(getOutputPath() + fileName + ".json");
        try {
            FileWriter fileWriter = new FileWriter(file, false);
            fileWriter.write(String.valueOf(finalOutput));
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            String fullPath = new File("").getAbsolutePath() + "/" + getOutputPath() + fileName + ".json";
            System.out.printf("Не удалось выполнить запись в файл '%s%n'", fullPath);
        }
    }

    /**
     * Класс, содержащий все входные объекты симуляции работы ресторана
     */
    public static class Data {
        public final static ArrayList<MenuDish> menuDishes = readJSON("menu_dishes", MenuDish.class);
        public final static ArrayList<Cook> cooks = readJSON("cookers", Cook.class);
        public final static ArrayList<OperationType> operationTypes;

        static {
            if (CHECK_OPERATION_TYPE_AVAILABILITY) {
                operationTypes = readJSON("operation_types", OperationType.class);
            } else {
                operationTypes = new ArrayList<>();
            }
        }

        public final static ArrayList<EquipmentType> equipmentTypes = readJSON("equipment_type", EquipmentType.class);
        public final static ArrayList<Equipment> equipments = readJSON("equipment", Equipment.class);
        public final static ArrayList<ProductType> productTypes = readJSON("product_types", ProductType.class);
        public final static ArrayList<Product> products = readJSON("products", Product.class);
        public final static ArrayList<DishCard> dishCards = readJSON("dish_cards", DishCard.class);
        public final static ArrayList<VisitorOrder> visitorOrders = readJSON("visitors_orders", VisitorOrder.class);

        public static void init() {
        }
    }
}
