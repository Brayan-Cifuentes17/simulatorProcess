package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProcessManager {
    private ArrayList<Process> initialProcesses;
    private ArrayList<Log> executionLogs;
    private Map<String, List<String>> processRelations; 

    public ProcessManager() {
        initialProcesses = new ArrayList<>();
        executionLogs = new ArrayList<>();
        processRelations = new HashMap<>();
    }

   
    public void addProcess(String name, long time, Status status) {
        Process process = new Process(name, time, status);
        initialProcesses.add(process);
    }

 
    public void addProcess(String name, long time, Status status, int finalPriority, 
                          Status suspended, Status resumed, Status destroyed, String referencedProcess) {
        
      
        Process process = new Process(name, time, status, 1, suspended, resumed, destroyed, referencedProcess);
        
        
        if (finalPriority != 1) {
            process.setFinalPriority(finalPriority);
            
        }
        
        initialProcesses.add(process);
        
        
        if (referencedProcess != null && !referencedProcess.trim().isEmpty()) {
            String[] refs = referencedProcess.split(",");
            for (String ref : refs) {
                addProcessRelation(name, ref.trim());
            }
        }
    }

    
    public void addProcess(String name, long time, Status status, int initialPriority, int finalPriority, 
                          Status suspended, Status resumed, Status destroyed, String referencedProcess) {
        
        Process process = new Process(name, time, status, initialPriority, suspended, resumed, destroyed, referencedProcess);
        
        
        if (finalPriority != initialPriority) {
            process.setFinalPriority(finalPriority);
            
        }
        
        initialProcesses.add(process);
        
       
        if (referencedProcess != null && !referencedProcess.trim().isEmpty()) {
            addProcessRelation(name, referencedProcess);
        }
    }

    private void addProcessRelation(String process, String referencedProcess) {
        processRelations.computeIfAbsent(process, k -> new ArrayList<>()).add(referencedProcess);
    }

    public boolean processExists(String name) {
        return initialProcesses.stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name.trim()));
    }

    public boolean priorityExists(int priority) {
        return initialProcesses.stream()
                .anyMatch(p -> p.getFinalPriority() == priority);
    }

    
    public boolean isProcessReferenced(String processName) {
        return initialProcesses.stream()
                .anyMatch(p -> p.hasReference() && 
                         p.getReferencedProcess().equalsIgnoreCase(processName.trim()));
    }

    public void removeProcess(String name) {
        initialProcesses.removeIf(p -> p.getName().equalsIgnoreCase(name.trim()));
        
        
        processRelations.remove(name);
        processRelations.values().forEach(list -> list.removeIf(ref -> ref.equalsIgnoreCase(name.trim())));
    }

    public void editProcess(int position, String processName, long newTime, Status newStatus) {
        if (position >= 0 && position < initialProcesses.size()) {
            Process existingProcess = initialProcesses.get(position);
            if (existingProcess.getName().equalsIgnoreCase(processName)) {
               
                Process updatedProcess = new Process(processName, newTime, newStatus);
                
                initialProcesses.set(position, updatedProcess);
            }
        }
    }

    public void editProcess(int position, String processName, long newTime, Status newStatus, 
                           int finalPriority, Status suspended, Status resumed, Status destroyed, String referencedProcess) {
        if (position >= 0 && position < initialProcesses.size()) {
            Process existingProcess = initialProcesses.get(position);
            if (existingProcess.getName().equalsIgnoreCase(processName)) {
                
             
                processRelations.remove(processName);
                
               
                int originalInitialPriority = existingProcess.getInitialPriority();
                
           
                Process updatedProcess = new Process(processName, newTime, newTime, newStatus, 0,
                                                   originalInitialPriority, finalPriority, suspended, resumed, destroyed, referencedProcess);
                
            
                initialProcesses.set(position, updatedProcess);
                
              
                if (referencedProcess != null && !referencedProcess.trim().isEmpty()) {
                    String [] refs = referencedProcess.split(",");
                    for (String ref : refs) {
                        addProcessRelation(processName, ref.trim());
                    }
                }
            }
        }
    }

    public boolean isEmpty() {
        return initialProcesses.isEmpty();
    }

    public void runSimulation() {
        executionLogs.clear();
        
       
        ArrayList<Process> processQueue = cloneProcesses();
        

        processQueue.sort((a, b) -> Integer.compare(a.getFinalPriority(), b.getFinalPriority()));
        
       
        for (Process p : processQueue) {
            if (p.hasPriorityChange()) {
                addLog(p, Filter.PRIORIDAD_CAMBIADA);
                
            }
        }
        
      
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

       
        if (process.isSuspended()) {
            addLog(process, Filter.SUSPENDIDO);
        }

        
        if (process.isResumed()) {
            addLog(process, Filter.REANUDADO);
        }

      
        addLog(process, Filter.DESPACHADO);

        
        process.subtractTime(Constants.QUANTUM_TIME);
        addLog(process, Filter.EN_EJECUCION);

      
        if (process.isDestroyed()) {
            addLog(process, Filter.DESTRUIDO);
            return; 
        }

        
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

    public List<Log> getLogsByFilter(Filter filter) {
        if (filter == Filter.TODO) {
            return new ArrayList<>(executionLogs);
        }
        
        return executionLogs.stream()
                .filter(log -> log.getFilter() == filter)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    
    public List<Process> getProcessesWithPriorityChanges() {
        
        List<Process> result = new ArrayList<>();
        for (Process p : initialProcesses) {
          
            if (p.hasPriorityChange()) {
                result.add(p);
            }
        }
     
        return result;
    }

   
    public List<String> getProcessRelationsReport() {
        List<String> report = new ArrayList<>();
        Set<String> addedRelations = new HashSet<>(); 
        
     
        
        // PASO 1: Agregar todas las relaciones directas (A -> B)
        for (Process process : initialProcesses) {
            if (process.hasReference() && process.getReferencedProcess() != null) {
                String[] references = process.getReferencedProcess().split(",");
                
                for (String reference : references) {
                    String cleanRef = reference.trim();
                    
                    // Verificar que el proceso referenciado existe
                    if (processExists(cleanRef)) {
                        String relation = process.getName() + " -> " + cleanRef;
                        
                        if (!addedRelations.contains(relation)) {
                            report.add(relation);
                            addedRelations.add(relation);
                            
                        }
                    }
                }
            }
    }
    
    // PASO 2: Agregar todas las relaciones inversas (B -> A) para hacer bidireccional
    for (Process process : initialProcesses) {
        if (process.hasReference() && process.getReferencedProcess() != null) {
            String[] references = process.getReferencedProcess().split(",");
            
            for (String reference : references) {
                String cleanRef = reference.trim();
                
                // Verificar que el proceso referenciado existe
                if (processExists(cleanRef)) {
                    // Crear relación inversa: referenciado -> proceso que lo referencia
                    String inverseRelation = cleanRef + " -> " + process.getName();
                    
                    if (!addedRelations.contains(inverseRelation)) {
                        report.add(inverseRelation);
                        addedRelations.add(inverseRelation);
                      
                    }
                }
            }
        }
    }
    
    // PASO 3: Ordenar alfabéticamente para mejor presentación
    report.sort(String::compareTo);
    

    return report;
}

   
    public List<Process> getSuspendedProcesses() {
        return initialProcesses.stream()
                .filter(Process::isSuspended)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<Process> getResumedProcesses() {
        return initialProcesses.stream()
                .filter(Process::isResumed)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public List<Process> getDestroyedProcesses() {
        return initialProcesses.stream()
                .filter(Process::isDestroyed)
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
        processRelations.clear();
    }

    public void clearLogs() {
        executionLogs.clear();
    }
}