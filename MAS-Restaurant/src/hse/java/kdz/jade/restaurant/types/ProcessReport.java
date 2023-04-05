package hse.java.kdz.jade.restaurant.types;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

import static hse.java.kdz.jade.restaurant.tools.AgentTools.getFormattedDate;

/**
 * Класс отчета о процессе для последующей сериализации в файл 'process_log.json'
 */
public class ProcessReport extends Report {
    private record ProcessOperation(@SerializedName("proc_oper") int operationId) {
    }
    @SerializedName("proc_id")
    private final int id;
    @SerializedName("ord_dish")
    private final int dish;
    @SerializedName("proc_started")
    private String startedAsString;
    @SerializedName("proc_ended")
    private String endedAsString;
    @SerializedName("proc_active")
    private boolean active;
    @SerializedName("proc_operations")
    private ArrayList<ProcessOperation> processOperations;

    public ProcessReport(int id, int dish) {
        this.id = id;
        this.dish = dish;
        startedAsString = "";
        endedAsString = "";
        processOperations = new ArrayList<>();
        active = false;
    }

    @Override
    public void formatDates() {
        startedAsString = getFormattedDate(getStarted());
        endedAsString = getFormattedDate(getEnded());
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

    public ArrayList<ProcessOperation> getProcessOperations() {
        return processOperations;
    }

    public void addProcessOperation(int operationID) {
        processOperations.add(new ProcessOperation(operationID));
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
