package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

/**
 * Класс повара для десериализации объектов из файла 'cookers.json'
 */
public class Cook {
    @SerializedName("cook_id")
    private final int id;
    @SerializedName("cook_name")
    private final String name;
    @SerializedName("cook_active")
    private boolean active;

    public Cook(int id, String name, boolean active) {
        this.id = id;
        this.name = name;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "Cook[" +
                "id=" + id + ", " +
                "name=" + name + ", " +
                "active=" + active + ']';
    }
}
