package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

/**
 * Класс типа продукта для десериализации объектов из файла 'product_types.json'
 */
public class ProductType {
    @SerializedName("prod_type_id")
    private final int id;
    @SerializedName("prod_type_name")
    private final String name;
    @SerializedName("prod_is_food")
    private final boolean isFood;

    public ProductType(int id, String name, boolean isFood) {
        this.id = id;
        this.name = name;
        this.isFood = isFood;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public boolean isFood() {
        return isFood;
    }

    @Override
    public String toString() {
        return "ProductType[" +
                "id=" + id + ", " +
                "name=" + name + ", " +
                "isFood=" + isFood + ']';
    }
}
