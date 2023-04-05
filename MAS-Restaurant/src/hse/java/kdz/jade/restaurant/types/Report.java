package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.Expose;

import java.util.Date;

/**
 * Абстрактный класс отчета о работе ресторана
 */
public abstract class Report {
    /**
     * Дата начала
     */
    @Expose
    private Date started;
    /**
     * Дата завершения
     */
    @Expose
    private Date ended;

    /**
     * Метод форматирует объекты даты в заданный формат в виде строки
     */
    public abstract void formatDates();

    public synchronized Date getStarted() {
        return started;
    }

    public synchronized void setStarted(Date started) {
        this.started = started;
    }

    public synchronized Date getEnded() {
        return ended;
    }

    public synchronized void setEnded(Date ended) {
        this.ended = ended;
    }
}
