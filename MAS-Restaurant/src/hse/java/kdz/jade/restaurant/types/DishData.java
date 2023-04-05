package hse.java.kdz.jade.restaurant.types;

import jade.core.AID;

import java.io.Serializable;

/**
 * Вспомогательный класс для межагентного общения посредством его объектов
 */
public final class DishData implements Serializable {
    private final DishCard dishCard;
    private final AID dishAgent;
    private final int priority;
    private double waitingTime;

    public DishData(DishCard dishCard, AID dishAgent, int priority) {
        this.dishCard = dishCard;
        this.dishAgent = dishAgent;
        this.priority = priority;
    }

    public DishCard getDishCard() {
        return dishCard;
    }

    public AID getDishAgent() {
        return dishAgent;
    }

    public double getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(double waitingTime) {
        this.waitingTime = waitingTime;
    }

    public int getPriority() {
        return priority;
    }
};