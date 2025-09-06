package model;

public class Process {
    private String name;
    private int originalTime;
    private int remainingTime;
    private Status status;
    private int cycleCount;

    public Process(String name, int time, Status status) {
        this.name = name;
        this.originalTime = time;
        this.remainingTime = time;
        this.status = status;
        this.cycleCount = 0;
    }

    
    public Process(String name, int originalTime, int remainingTime, Status status, int cycleCount) {
        this.name = name;
        this.originalTime = originalTime;
        this.remainingTime = remainingTime;
        this.status = status;
        this.cycleCount = cycleCount;
    }

    public void subtractTime(int time) {
        this.remainingTime -= time;
        if (remainingTime < 0) {
            remainingTime = 0;
        }
    }

    public void incrementCycle() {
        this.cycleCount++;
    }

    public boolean isFinished() {
        return remainingTime <= 0;
    }

    public boolean isBlocked() {
        return status == Status.BLOQUEADO;
    }

 
    public String getName() {
        return name;
    }

    public int getOriginalTime() {
        return originalTime;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public Status getStatus() {
        return status;
    }

    public int getCycleCount() {
        return cycleCount;
    }

    public String getStatusString() {
        return status == Status.BLOQUEADO ? "Bloqueado" : "No Bloqueado";
    }

  
    public void setName(String name) {
        this.name = name;
    }

    public void setOriginalTime(int originalTime) {
        this.originalTime = originalTime;
        this.remainingTime = originalTime; 
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setCycleCount(int cycleCount) {
        this.cycleCount = cycleCount;
    }

   
    public Process clone() {
        return new Process(name, originalTime, remainingTime, status, cycleCount);
    }

    @Override
    public String toString() {
        return "Process{" +
                "name='" + name + '\'' +
                ", originalTime=" + originalTime +
                ", remainingTime=" + remainingTime +
                ", status=" + status +
                ", cycleCount=" + cycleCount +
                '}';
    }
}