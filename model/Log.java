package model;

public class Log {
    private String processName;
    private long remainingTime;
    private Status status;
    private Filter filter;
    private int cycleCount;
    private long timestamp;
    private int priority; 

    public Log(Process process, Filter filter) {
        this.processName = process.getName();
        this.remainingTime = process.getRemainingTime();
        this.status = process.getStatus();
        this.cycleCount = process.getCycleCount();
        this.filter = filter;
        this.timestamp = System.currentTimeMillis();
        this.priority = process.getFinalPriority();
    }

    public String getProcessName() {
        return processName;
    }

    public long getRemainingTime() {
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

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "Log{" +
                "processName='" + processName + '\'' +
                ", remainingTime=" + remainingTime +
                ", status=" + status +
                ", filter=" + filter +
                ", cycleCount=" + cycleCount +
                ", priority=" + priority +
                '}';
    }
}