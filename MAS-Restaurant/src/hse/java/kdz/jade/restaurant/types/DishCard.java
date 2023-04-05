package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Класс карточки блюда для десериализации объектов из файла 'dish_cards.json'
 */
public class DishCard implements Serializable {
    @SerializedName("card_id")
    private final int id;
    @SerializedName("dish_name")
    private final String name;
    @SerializedName("card_descr")
    private final String description;
    @SerializedName("card_time")
    private final double time;
    private final ArrayList<Operation> operations;

    public DishCard(int id, String name, String description, double time,
                    ArrayList<Operation> operations) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.time = time;
        this.operations = operations;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getTime() {
        return time;
    }

    public ArrayList<Operation> getOperations() {
        return operations;
    }

    @Override
    public String toString() {
        return "DishCard[" +
                "id=" + id + ", " +
                "name=" + name + ", " +
                "description=" + description + ", " +
                "time=" + time + ", " +
                "operations=" + operations + ']';
    }
}