package model;

public class Log {
    private String processName;
    private int remainingTime;
    private Status status;
    private Filter filter;
    private int cycleCount;
    private long timestamp;

    public Log(Process process, Filter filter) {
        this.processName = process.getName();
        this.remainingTime = process.getRemainingTime();
        this.status = process.getStatus();
        this.cycleCount = process.getCycleCount();
        this.filter = filter;
        this.timestamp = System.currentTimeMillis();
    }

  
    public String getProcessName() {
        return processName;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public Status getStatus() {
        return status;
    }

    public String getStatusString() {
        return status == Status.BLOQUEADO ? "Bloqueado" : "No Bloqueado";
    }

    public Filter getFilter() {
        return filter;
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Log{" +
                "processName='" + processName + '\'' +
                ", remainingTime=" + remainingTime +
                ", status=" + status +
                ", filter=" + filter +
                ", cycleCount=" + cycleCount +
                '}';
    }
}