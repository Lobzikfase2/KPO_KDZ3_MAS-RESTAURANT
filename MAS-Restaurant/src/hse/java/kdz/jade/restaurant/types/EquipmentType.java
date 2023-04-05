package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

/**
 * Класс типа оборудования для десериализации объектов из файла 'equipment_type.json'
 */
public class EquipmentType {
    @SerializedName("equip_type_id")
    private final int id;
    @SerializedName("equip_type_name")
    private final String name;

    public EquipmentType(int id, String name) {
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
        return "EquipmentType[" +
                "id=" + id + ", " +
                "name=" + name + ']';
    }
}
