package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.getFormattedDate;


/**
 * Класс отчета об операции для последующей сериализации в файл 'operation_log.json'
 */
public class OperationReport extends Report {
    @SerializedName("oper_id")
    private final int id;
    @SerializedName("oper_proc")
    private final int processId;
    @SerializedName("oper_card")
    private final int dishCardId;
    @SerializedName("oper_started")
    private String startedAsString;
    @SerializedName("oper_ended")
    private String endedAsString;
    @SerializedName("oper_equip_id")
    private int equipmentId;
    @SerializedName("oper_cook_id")
    private int cookId;
    @SerializedName("oper_active")
    private boolean active;

    public OperationReport(int id, int processId, int dishCardId) {
        this.id = id;
        this.processId = processId;
        this.dishCardId = dishCardId;
        startedAsString = "";
        endedAsString = "";
        equipmentId = -1;
        cookId = -1;
        active = false;
    }

    @Override
    public void formatDates() {
        startedAsString = getFormattedDate(getStarted());
        endedAsString = getFormattedDate(getEnded());
    }

    public int getId() {
        return id;
    }

    public int getProcessId() {
        return processId;
    }

    public int getDishCardId() {
        return dishCardId;
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }

    public int getCookId() {
        return cookId;
    }

    public void setCookId(int cookId) {
        this.cookId = cookId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
}
