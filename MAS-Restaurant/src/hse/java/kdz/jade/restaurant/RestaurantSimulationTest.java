package hse.java.kdz.jade.restaurant;

import hse.java.kdz.jade.restaurant.tools.AgentTools;
import hse.java.kdz.jade.restaurant.tools.DataParser;

import java.io.File;

/**
 * Конструирование программного обеспечения
 * КДЗ №3: «Мультиагентная система управления рестораном»
 * @author Маринченко Игорь Игоревич
 * Группа: БПИ218
 */
public class RestaurantSimulationTest {
    private static String inputPath;
    private static String outputPath;
    private static boolean optionsFromFile;

    public static String getInputPath() {
        return inputPath;
    }

    public static String getOutputPath() {
        return outputPath;
    }

    public static boolean isOptionsFromFile() {
        return optionsFromFile;
    }

    /**
     * Метод - точка входа в программу
     * @param args - аргументы командой строки
     */
    public static void main(String[] args) {
        StringBuilder about = new StringBuilder();
        about.append("Аргументы командной строки:\n");
        about.append("\t1) относительный путь до папки со входными json файлами\n");
        about.append("\t2) считать настройки симуляции из файла? [true/false]");

        if (args.length < 1) {
            System.out.println("Отсутствует первый аргумент командой строки");
            System.out.println(about);
            System.exit(1);
        } else if (args.length < 2) {
            System.out.println("Отсутствует второй аргумент командой строки");
            System.out.println(about);
            System.exit(1);
        }

        inputPath = args[0];
        if (!inputPath.startsWith("/")) {
            inputPath = new File("").getAbsolutePath() + "/" + inputPath;
        }
        if (!inputPath.endsWith("/")) {
            inputPath += "/";
        }
        outputPath = "output/";
        optionsFromFile = Boolean.parseBoolean(args[1]);

        String fullDirPath = new File("").getAbsolutePath() + "/" + getOutputPath();
        File file = new File(getOutputPath());
        if (!file.exists() && !file.mkdirs()) {
            System.out.printf("Не удалось создать папку '%s%n'", fullDirPath);
            System.exit(1);
        }

        // Инициализируем класс Data с объектами симуляции
        DataParser.Data.init();
        // Запускаем платформу JADE и начинаем симуляцию работы ресторана
        AgentTools.startSimulation();
    }
}
