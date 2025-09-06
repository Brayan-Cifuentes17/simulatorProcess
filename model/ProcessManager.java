package model;

import java.util.ArrayList;
import java.util.List;

public class ProcessManager {
    private ArrayList<Process> initialProcesses;
    private ArrayList<Log> executionLogs;

    public ProcessManager() {
        initialProcesses = new ArrayList<>();
        executionLogs = new ArrayList<>();
    }

    public void addProcess(String name, int time, Status status) {
        Process process = new Process(name, time, status);
        initialProcesses.add(process);
    }

    public boolean processExists(String name) {
        return initialProcesses.stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name.trim()));
    }

    public void removeProcess(String name) {
        initialProcesses.removeIf(p -> p.getName().equalsIgnoreCase(name.trim()));
    }

    public void editProcess(int position, String processName, int newTime, Status newStatus) {
        if (position >= 0 && position < initialProcesses.size()) {
            Process existingProcess = initialProcesses.get(position);
            if (existingProcess.getName().equalsIgnoreCase(processName)) {
                // Crear nuevo proceso con los datos actualizados pero manteniendo el nombre
                Process updatedProcess = new Process(processName, newTime, newStatus);
                // Reemplazar en la misma posición
                initialProcesses.set(position, updatedProcess);
            }
        }
    }

    public boolean isEmpty() {
        return initialProcesses.isEmpty();
    }

    
    public void runSimulation() {
        executionLogs.clear();
        ArrayList<Process> processQueue = cloneProcesses();
        
        while (!processQueue.isEmpty()) {
            Process currentProcess = processQueue.remove(0);
            executeProcessCycle(currentProcess, processQueue);
        }
    }

    private ArrayList<Process> cloneProcesses() {
        ArrayList<Process> clones = new ArrayList<>();
        for (Process p : initialProcesses) {
            clones.add(p.clone());
        }
        return clones;
    }

    private void executeProcessCycle(Process process, ArrayList<Process> queue) {
        
        addLog(process, Filter.LISTO);

       
        addLog(process, Filter.DESPACHADO);

     
        process.subtractTime(Constants.QUANTUM_TIME);
        addLog(process, Filter.EN_EJECUCION);

        
        if (process.isFinished()) {
            addLog(process, Filter.FINALIZADO);
        } else {
            process.incrementCycle();
            if (process.isBlocked()) {
                addLog(process, Filter.BLOQUEADO);
                addLog(process, Filter.DESPERTAR);
            } else {
                addLog(process, Filter.TIEMPO_EXPIRADO);
            }
            queue.add(process);
        }
    }

    private void addLog(Process process, Filter filter) {
        Log log = new Log(process, filter);
        executionLogs.add(log);
    }

    //Filtrar los logs
    public List<Log> getLogsByFilter(Filter filter) {
        if (filter == Filter.TODO) {
            return new ArrayList<>(executionLogs);
        }
        
        return executionLogs.stream()
                .filter(log -> log.getFilter() == filter)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

   
    public ArrayList<Process> getInitialProcesses() {
        return new ArrayList<>(initialProcesses);
    }

    public ArrayList<Log> getAllLogs() {
        return new ArrayList<>(executionLogs);
    }

    
    public void clearAll() {
        initialProcesses.clear();
        executionLogs.clear();
    }

    public void clearLogs() {
        executionLogs.clear();
    }
}