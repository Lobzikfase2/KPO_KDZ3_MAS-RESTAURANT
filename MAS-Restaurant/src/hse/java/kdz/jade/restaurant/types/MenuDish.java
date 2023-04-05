package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Класс блюда в меню для десериализации объектов из файла 'menu_dishes.json'
 */
public class MenuDish implements Serializable {
    @SerializedName("menu_dish_id")
    private final int id;
    @SerializedName("menu_dish_card")
    private final int card;
    @SerializedName("menu_dish_price")
    private final double price;
    @SerializedName("menu_dish_active")
    private final boolean active;

    public MenuDish(int id, int card, double price, boolean active) {
        this.id = id;
        this.card = card;
        this.price = price;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public int getCard() {
        return card;
    }

    public double getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "MenuDish[" +
                "id=" + id + ", " +
                "card=" + card + ", " +
                "price=" + price + ", " +
                "active=" + active + ']';
    }
}
