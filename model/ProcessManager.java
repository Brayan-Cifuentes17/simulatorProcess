package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProcessManager {
    private ArrayList<Process> initialProcesses;
    private ArrayList<Log> executionLogs;
    private Map<String, List<String>> processRelations; 

    public ProcessManager() {
        initialProcesses = new ArrayList<>();
        executionLogs = new ArrayList<>();
        processRelations = new HashMap<>();
    }

    // Agregar proceso básico
    public void addProcess(String name, int time, Status status) {
        Process process = new Process(name, time, status);
        initialProcesses.add(process);
    }

    // Agregar proceso con parámetros avanzados
    public void addProcess(String name, int time, Status status, int finalPriority, 
                          Status suspended, Status resumed, Status destroyed, String referencedProcess) {
        
        // Crear proceso con prioridad inicial 1
        Process process = new Process(name, time, status, 1, suspended, resumed, destroyed, referencedProcess);
        
        // Si la prioridad final es diferente de 1, establecer el cambio
        if (finalPriority != 1) {
            process.setFinalPriority(finalPriority);
            System.out.println("DEBUG: Proceso " + name + " creado con cambio de prioridad: 1 -> " + finalPriority);
        }
        
        initialProcesses.add(process);
        
        // Manejar referencias entre procesos
        if (referencedProcess != null && !referencedProcess.trim().isEmpty()) {
            addProcessRelation(name, referencedProcess);
        }
    }

    // Agregar proceso con prioridades inicial y final específicas
    public void addProcess(String name, int time, Status status, int initialPriority, int finalPriority, 
                          Status suspended, Status resumed, Status destroyed, String referencedProcess) {
        
        Process process = new Process(name, time, status, initialPriority, suspended, resumed, destroyed, referencedProcess);
        
        // Si hay cambio de prioridad, establecerlo
        if (finalPriority != initialPriority) {
            process.setFinalPriority(finalPriority);
            System.out.println("DEBUG: Proceso " + name + " creado con cambio de prioridad: " + initialPriority + " -> " + finalPriority);
        }
        
        initialProcesses.add(process);
        
        // Manejar referencias entre procesos
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

    // Verificar si un proceso está siendo referenciado por otros
    public boolean isProcessReferenced(String processName) {
        return initialProcesses.stream()
                .anyMatch(p -> p.hasReference() && 
                         p.getReferencedProcess().equalsIgnoreCase(processName.trim()));
    }

    public void removeProcess(String name) {
        initialProcesses.removeIf(p -> p.getName().equalsIgnoreCase(name.trim()));
        
        // Limpiar referencias
        processRelations.remove(name);
        processRelations.values().forEach(list -> list.removeIf(ref -> ref.equalsIgnoreCase(name.trim())));
    }

    public void editProcess(int position, String processName, int newTime, Status newStatus) {
        if (position >= 0 && position < initialProcesses.size()) {
            Process existingProcess = initialProcesses.get(position);
            if (existingProcess.getName().equalsIgnoreCase(processName)) {
                // Crear proceso básico actualizado
                Process updatedProcess = new Process(processName, newTime, newStatus);
                // Mantener la configuración del proceso original
                initialProcesses.set(position, updatedProcess);
            }
        }
    }

    public void editProcess(int position, String processName, int newTime, Status newStatus, 
                           int finalPriority, Status suspended, Status resumed, Status destroyed, String referencedProcess) {
        if (position >= 0 && position < initialProcesses.size()) {
            Process existingProcess = initialProcesses.get(position);
            if (existingProcess.getName().equalsIgnoreCase(processName)) {
                
                // Limpiar referencias anteriores del proceso
                processRelations.remove(processName);
                
                // Mantener la prioridad inicial original
                int originalInitialPriority = existingProcess.getInitialPriority();
                
                // Crear proceso actualizado
                Process updatedProcess = new Process(processName, newTime, newTime, newStatus, 0,
                                                   originalInitialPriority, finalPriority, suspended, resumed, destroyed, referencedProcess);
                
                // Reemplazar en la lista
                initialProcesses.set(position, updatedProcess);
                
                // Agregar nueva referencia si existe
                if (referencedProcess != null && !referencedProcess.trim().isEmpty()) {
                    addProcessRelation(processName, referencedProcess);
                }
                
                System.out.println("DEBUG: Proceso " + processName + " editado. Prioridad: " + originalInitialPriority + " -> " + finalPriority + " (Cambio: " + updatedProcess.hasPriorityChange() + ")");
            }
        }
    }

    public boolean isEmpty() {
        return initialProcesses.isEmpty();
    }

    public void runSimulation() {
        executionLogs.clear();
        
        // Clonar procesos para la simulación
        ArrayList<Process> processQueue = cloneProcesses();
        
        // ORDENAR UNA SOLA VEZ POR PRIORIDAD AL INICIO
        // Menor número = mayor prioridad (1 es más prioritario que 2)
        processQueue.sort((a, b) -> Integer.compare(a.getFinalPriority(), b.getFinalPriority()));
        
        // Agregar logs de cambio de prioridad al inicio
        for (Process p : processQueue) {
            if (p.hasPriorityChange()) {
                addLog(p, Filter.PRIORIDAD_CAMBIADA);
                System.out.println("DEBUG: Agregando log de prioridad cambiada para " + p.getName());
            }
        }
        
        // EJECUTAR ROUND ROBIN PURO (SIN reordenar por prioridad)
        while (!processQueue.isEmpty()) {
            // Tomar el primer proceso de la cola (ya están ordenados por prioridad inicial)
            Process currentProcess = processQueue.remove(0);
            
            // Ejecutar el proceso por un quantum
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
        // Proceso listo
        addLog(process, Filter.LISTO);

        // Verificar suspensión
        if (process.isSuspended()) {
            addLog(process, Filter.SUSPENDIDO);
        }

        // Verificar reanudación
        if (process.isResumed()) {
            addLog(process, Filter.REANUDADO);
        }

        // Despachar proceso
        addLog(process, Filter.DESPACHADO);

        // Ejecutar por quantum de tiempo
        process.subtractTime(Constants.QUANTUM_TIME);
        addLog(process, Filter.EN_EJECUCION);

        // Verificar si fue destruido
        if (process.isDestroyed()) {
            addLog(process, Filter.DESTRUIDO);
            return; // El proceso destruido no continúa
        }

        // Verificar si terminó
        if (process.isFinished()) {
            addLog(process, Filter.FINALIZADO);
        } else {
            // El proceso NO terminó, debe volver a la cola
            process.incrementCycle();
            
            if (process.isBlocked()) {
                addLog(process, Filter.BLOQUEADO);
                addLog(process, Filter.DESPERTAR);
            } else {
                addLog(process, Filter.TIEMPO_EXPIRADO);
            }
            
            // AGREGAR AL FINAL DE LA COLA (Round Robin normal)
            // NO reordenar por prioridad, mantener el orden de llegada
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

    // Obtener procesos con cambio de prioridad
    public List<Process> getProcessesWithPriorityChanges() {
        System.out.println("DEBUG: Buscando procesos con cambio de prioridad...");
        List<Process> result = new ArrayList<>();
        for (Process p : initialProcesses) {
            System.out.println("DEBUG: Proceso " + p.getName() + " - Inicial: " + p.getInitialPriority() + ", Final: " + p.getFinalPriority() + ", Tiene cambio: " + p.hasPriorityChange());
            if (p.hasPriorityChange()) {
                result.add(p);
            }
        }
        System.out.println("DEBUG: Encontrados " + result.size() + " procesos con cambio de prioridad");
        return result;
    }

    // Generar reporte de relaciones entre procesos
    public List<String> getProcessRelationsReport() {
        List<String> report = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : processRelations.entrySet()) {
            String process = entry.getKey();
            List<String> references = entry.getValue();
            
            if (!references.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append(process).append(" -> ");
                for (int i = 0; i < references.size(); i++) {
                    sb.append(references.get(i));
                    if (i < references.size() - 1) {
                        sb.append(", ");
                    }
                }
                report.add(sb.toString());
            }
        }
        
        return report;
    }

    // Obtener procesos suspendidos
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