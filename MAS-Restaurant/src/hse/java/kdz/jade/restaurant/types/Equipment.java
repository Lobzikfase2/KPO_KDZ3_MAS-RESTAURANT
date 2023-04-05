package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

/**
 * Класс оборудования для десериализации объектов из файла 'equipment.json'
 */
public class Equipment {
    @SerializedName("equip_id")
    private final int id;
    @SerializedName("equip_type")
    private final int type;
    @SerializedName("equip_name")
    private final String name;
    @SerializedName("equip_active")
    private final boolean active;

    public Equipment(int id, int type, String name, boolean active) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.active = active;
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

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "Equipment[" +
                "id=" + id + ", " +
                "type=" + type + ", " +
                "name=" + name + ", " +
                "active=" + active + ']';
    }
}
