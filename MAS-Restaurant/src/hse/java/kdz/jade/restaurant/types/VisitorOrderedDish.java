package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;


/**
 * Класс блюда в заказе посетителя для десериализации объектов из файла 'visitors_orders.json'
 */
public class VisitorOrderedDish implements Serializable {
    @SerializedName("ord_dish_id")
    private final int id;

    @SerializedName("menu_dish")
    private final int dish;

    public VisitorOrderedDish(int id, int dish) {
        this.id = id;
        this.dish = dish;
    }

    public int getId() {
        return id;
    }

    public int getDish() {
        return dish;
    }

    @Override
    public String toString() {
        return "VisitorOrderedDish[" +
                "id=" + id + ", " +
                "dish=" + dish + ']';
    }
}
