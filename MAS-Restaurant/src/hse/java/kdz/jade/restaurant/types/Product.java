package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

/**
 * Класс продукта на складе для десериализации объектов из файла 'products.json'
 */
public class Product {
    @SerializedName("prod_item_id")
    private final int id;
    @SerializedName("prod_item_type")
    private final int type;
    @SerializedName("prod_item_name")
    private final String name;
    @SerializedName("prod_item_company")
    private final String company;
    @SerializedName("prod_item_unit")
    private final String unit;
    @SerializedName("prod_item_quantity")
    private double quantity;
    @SerializedName("prod_item_cost")
    private final double cost;
    @SerializedName("prod_item_delivered")
    private final String delivered;
    @SerializedName("prod_item_valid_until")
    private final String validUntil;

    public Product(int id, int type, String name,
                   String company, String unit,
                   double quantity, double cost,
                   String delivered, String validUntil) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.company = company;
        this.unit = unit;
        this.quantity = quantity;
        this.cost = cost;
        this.delivered = delivered;
        this.validUntil = validUntil;
    }

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getCompany() {
        return company;
    }

    public String getUnit() {
        return unit;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        if (quantity < 0) {
            this.quantity = 0;
        } else {
            this.quantity = quantity;
        }
    }

    public void addQuantity(double quantity) {
        if (quantity > 0) {
            this.quantity += quantity;
        }
    }

    public double getCost() {
        return cost;
    }

    public String getDelivered() {
        return delivered;
    }

    public String getValidUntil() {
        return validUntil;
    }

    @Override
    public String toString() {
        return "Product[" +
                "id=" + id + ", " +
                "type=" + type + ", " +
                "name=" + name + ", " +
                "company=" + company + ", " +
                "unit=" + unit + ", " +
                "quantity=" + quantity + ", " +
                "cost=" + cost + ", " +
                "delivered=" + delivered + ", " +
                "validUntil=" + validUntil + ']';
    }
}
