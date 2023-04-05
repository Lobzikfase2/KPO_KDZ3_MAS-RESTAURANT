package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Класс продукта в операции из карточки блюда для десериализации объектов из файла 'dish_cards.json'
 */
public class OperationProduct implements Serializable {
    @SerializedName("prod_type")
    private final int type;
    @SerializedName("prod_quantity")
    private final double quantity;

    public OperationProduct(int type, double quantity) {
        this.type = type;
        this.quantity = quantity;
    }

    public int getType() {
        return type;
    }

    public double getQuantity() {
        return quantity;
    }

    @Override
    public String toString() {
        return "OperationProduct[" +
                "type=" + type + ", " +
                "quantity=" + quantity + ']';
    }
}
