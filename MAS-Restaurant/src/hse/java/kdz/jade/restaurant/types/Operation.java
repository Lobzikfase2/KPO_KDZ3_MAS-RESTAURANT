package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Класс операции из карточки блюда для десериализации объектов из файла 'dish_cards.json'
 */
public class Operation implements Serializable {
    @SerializedName("oper_type")
    private final int type;
    @SerializedName("equip_type")
    private final int equipmentType;
    @SerializedName("oper_time")
    private final double time;
    @SerializedName("oper_async_point")
    private final int asyncPoint;
    @SerializedName("oper_products")
    private final ArrayList<OperationProduct> products;

    public Operation(int type, int equipmentType, double time, int asyncPoint, ArrayList<OperationProduct> products) {
        this.type = type;
        this.equipmentType = equipmentType;
        this.time = time;
        this.asyncPoint = asyncPoint;
        this.products = products;
    }

    public int getType() {
        return type;
    }

    public int getEquipmentType() {
        return equipmentType;
    }

    public double getTime() {
        return time;
    }

    public int getAsyncPoint() {
        return asyncPoint;
    }

    public ArrayList<OperationProduct> getProducts() {
        return products;
    }

    @Override
    public String toString() {
        return "Operation[" +
                "type=" + type + ", " +
                "equipmentType=" + equipmentType + ", " +
                "time=" + time + ", " +
                "asyncPoint=" + asyncPoint + ", " +
                "products=" + products + ']';
    }
}
