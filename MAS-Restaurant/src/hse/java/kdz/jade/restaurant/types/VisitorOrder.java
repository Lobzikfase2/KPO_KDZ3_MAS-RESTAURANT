package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.getFormattedDate;

/**
 * Класс заказа посетителя для десериализации объектов из файла 'visitors_orders.json'
 * Выступает также в роли класса отчета о заказе посетителя для последующей сериализации в файл 'visitor_order_log.json'
 */
public class VisitorOrder extends Report implements Serializable {
    @SerializedName("vis_name")
    private final String visitorName;
    @SerializedName("vis_ord_started")
    private String startedAsString;
    @SerializedName("vis_ord_ended")
    private String endedAsString;
    @SerializedName("vis_ord_total")
    private double totalCost;
    @SerializedName("vis_ord_dishes")
    private ArrayList<VisitorOrderedDish> dishes;

    public VisitorOrder(String visitorName, String started, String ended, double totalCost,
                        ArrayList<VisitorOrderedDish> dishes) {
        this.visitorName = visitorName;
        startedAsString = started;
        endedAsString = ended;
        this.totalCost = totalCost;
        this.dishes = dishes;
    }

    @Override
    public void formatDates() {
        startedAsString = getFormattedDate(getStarted());
        endedAsString = getFormattedDate(getEnded());
    }

    public String getVisitorName() {
        return visitorName;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public ArrayList<VisitorOrderedDish> getDishes() {
        return dishes;
    }

    public void setDishes(ArrayList<VisitorOrderedDish> dishes) {
        this.dishes = dishes;
    }

    public String getStartedAsString() {
        return startedAsString;
    }

    public void setStartedAsString(String startedAsString) {
        this.startedAsString = startedAsString;
    }

    public String getEndedAsString() {
        return endedAsString;
    }

    public void setEndedAsString(String endedAsString) {
        this.endedAsString = endedAsString;
    }

    @Override
    public String toString() {
        return "VisitorOrder[" +
                "visitorName=" + visitorName + ", " +
                "started=" + getStarted() + ", " +
                "ended=" + getEnded() + ", " +
                "totalCost=" + totalCost + ", " +
                "dishes=" + dishes + ']';
    }
}
