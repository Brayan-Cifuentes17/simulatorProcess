package model;

public class Process {
    private String name;
    private int originalTime;
    private int remainingTime;
    private Status status;
    private int cycleCount;
    
  
    private int initialPriority;
    private int finalPriority;
    private Status suspended;
    private Status resumed;
    private Status destroyed;
    private int priorityChange;
    private String referencedProcess; 

    public Process(String name, int time, Status status) {
        this.name = name;
        this.originalTime = time;
        this.remainingTime = time;
        this.status = status;
        this.cycleCount = 0;
        
        this.initialPriority = 1;
        this.finalPriority = 1;
        this.suspended = Status.NO_SUSPENDIDO;
        this.resumed = Status.NO_REANUDADO;
        this.destroyed = Status.NO_DESTRUIDO;
        this.priorityChange = -1; 
        this.referencedProcess = null;
    }
    
    
    public Process(String name, int time, Status status, int initialPriority, 
                   Status suspended, Status resumed, Status destroyed, String referencedProcess) {
        this.name = name;
        this.originalTime = time;
        this.remainingTime = time;
        this.status = status;
        this.cycleCount = 0;
        this.initialPriority = initialPriority;
        this.finalPriority = initialPriority; 
        this.suspended = suspended;
        this.resumed = resumed;
        this.destroyed = destroyed;
        this.priorityChange = -1; 
        this.referencedProcess = referencedProcess;
    }

    public Process(String name, int originalTime, int remainingTime, Status status, int cycleCount,
                   int initialPriority, int finalPriority, Status suspended, Status resumed, 
                   Status destroyed, String referencedProcess) {
        this.name = name;
        this.originalTime = originalTime;
        this.remainingTime = remainingTime;
        this.status = status;
        this.cycleCount = cycleCount;
        this.initialPriority = initialPriority;
        this.finalPriority = finalPriority;
        this.suspended = suspended;
        this.resumed = resumed;
        this.destroyed = destroyed;
    
        this.priorityChange = (finalPriority != initialPriority) ? finalPriority : -1;
        this.referencedProcess = referencedProcess;
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

    public boolean isSuspended() {
        return suspended == Status.SUSPENDIDO;
    }

    public boolean isResumed() {
        return resumed == Status.REANUDADO;
    }

    public boolean isDestroyed() {
        return destroyed == Status.DESTRUIDO;
    }

    public boolean hasPriorityChange() {
       
        return finalPriority != initialPriority;
    }

    public boolean hasReference() {
        return referencedProcess != null && !referencedProcess.trim().isEmpty();
    }

    public void resetTime() {
        remainingTime = originalTime;
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

    public int getInitialPriority() {
        return initialPriority;
    }

    public int getFinalPriority() {
        return finalPriority;
    }

    public Status getSuspended() {
        return suspended;
    }

    public Status getResumed() {
        return resumed;
    }

    public Status getDestroyed() {
        return destroyed;
    }

    public int getPriorityChange() {
        return priorityChange;
    }

    public String getReferencedProcess() {
        return referencedProcess;
    }

    public String getSuspendedString() {
        return suspended == Status.SUSPENDIDO ? "Si" : "No";
    }

    public String getResumedString() {
        return resumed == Status.REANUDADO ? "Si" : "No";
    }

    public String getDestroyedString() {
        return destroyed == Status.DESTRUIDO ? "Si" : "No";
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

    public void setInitialPriority(int initialPriority) {
        this.initialPriority = initialPriority;
        
    }

    public void setFinalPriority(int finalPriority) {
        this.finalPriority = finalPriority;
   
        this.priorityChange = (finalPriority != initialPriority) ? finalPriority : -1;
    }

    public void setSuspended(Status suspended) {
        this.suspended = suspended;
    }

    public void setResumed(Status resumed) {
        this.resumed = resumed;
    }

    public void setDestroyed(Status destroyed) {
        this.destroyed = destroyed;
    }

    public void setPriorityChange(int priorityChange) {
        this.priorityChange = priorityChange;
        this.finalPriority = priorityChange;
    }

    public void setReferencedProcess(String referencedProcess) {
        this.referencedProcess = referencedProcess;
    }

    public Process clone() {
        return new Process(name, originalTime, remainingTime, status, cycleCount,
                          initialPriority, finalPriority, suspended, resumed, destroyed, referencedProcess);
    }

    @Override
    public String toString() {
        return "Process{" +
                "name='" + name + '\'' +
                ", originalTime=" + originalTime +
                ", remainingTime=" + remainingTime +
                ", status=" + status +
                ", cycleCount=" + cycleCount +
                ", initialPriority=" + initialPriority +
                ", finalPriority=" + finalPriority +
                ", hasPriorityChange=" + hasPriorityChange() +
                ", suspended=" + suspended +
                ", resumed=" + resumed +
                ", destroyed=" + destroyed +
                ", referencedProcess='" + referencedProcess + '\'' +
                '}';
    }
}