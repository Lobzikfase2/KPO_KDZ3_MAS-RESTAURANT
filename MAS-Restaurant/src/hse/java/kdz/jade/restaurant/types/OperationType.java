package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

/**
 * Класс типа операции для десериализации объектов из файла 'operation_types.json'
 */
public class OperationType {
    @SerializedName("oper_type_id")
    private final int id;
    @SerializedName("oper_type_name")
    private final String name;

    public OperationType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "OperationType[" +
                "id=" + id + ", " +
                "name=" + name + ']';
    }
}