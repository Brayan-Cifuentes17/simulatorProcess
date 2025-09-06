package view;

import model.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProcessSimulatorGUI extends JFrame implements ActionListener {
    private ProcessManager processManager;
    
   
    private JTextField txtProcessName;
    private JTextField txtProcessTime;
    private JTextField txtPriority;
    private JTextField txtPriorityChange;
    private JComboBox<String> cmbStatus;
    private JComboBox<String> cmbSuspended;
    private JComboBox<String> cmbResumed;
    private JComboBox<String> cmbDestroyed;
    private JComboBox<String> cmbReferencedProcess;
    
  
    private DefaultTableModel processTableModel;
    private JTable processTable;
    
    
    private JPanel resultsPanel;
    private CardLayout cardLayout;
    
    
    private DefaultTableModel[] resultTableModels;
    private String[] tableNames = {
        "Inicial", "Listos", "Despachados", "En Ejecución", 
        "Tiempo Expirado", "Bloqueados", "Despertar", "Finalizados",
        "Prioridad Cambiada", "Suspendidos", "Reanudados", "Destruidos"
    };
    private Filter[] filters = {
        Filter.INICIAL, Filter.LISTO, Filter.DESPACHADO, Filter.EN_EJECUCION,
        Filter.TIEMPO_EXPIRADO, Filter.BLOQUEADO, Filter.DESPERTAR, Filter.FINALIZADO,
        Filter.PRIORIDAD_CAMBIADA, Filter.SUSPENDIDO, Filter.REANUDADO, Filter.DESTRUIDO
    };

    private String currentAction;
    private NumberFormat numberFormatter;

    public ProcessSimulatorGUI() {
        processManager = new ProcessManager();
        numberFormatter = NumberFormat.getNumberInstance(new Locale("es", "ES"));
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        setUndecorated(true); 
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void initializeComponents() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);

    
        txtProcessName = new JTextField(15);
        txtProcessTime = new JTextField(15);
        txtPriority = new JTextField("1", 15);
        txtPriorityChange = new JTextField(15);
        cmbStatus = new JComboBox<>(new String[]{"No Bloqueado", "Bloqueado"});
        cmbSuspended = new JComboBox<>(new String[]{"No", "Si"});
        cmbResumed = new JComboBox<>(new String[]{"No", "Si"});
        cmbDestroyed = new JComboBox<>(new String[]{"No", "Si"});
        cmbReferencedProcess = new JComboBox<>();

        setupTimeField();
        setupPriorityFields();
        updateReferencedProcessCombo();

       
        processTableModel = new DefaultTableModel(
            new String[]{"Nombre", "Tiempo", "Prioridad", "Estado", "Suspendido", "Reanudado", "Destruido", "Referencia"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        processTable = new JTable(processTableModel);
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

       
        cardLayout = new CardLayout();
        resultsPanel = new JPanel(cardLayout);
        
       
        resultTableModels = new DefaultTableModel[tableNames.length];
        for (int i = 0; i < tableNames.length; i++) {
            if (i < 8) {
                
                resultTableModels[i] = new DefaultTableModel(
                    new String[]{"Proceso", "Tiempo Restante", "Prioridad", "Estado", "Ciclos"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            } else {
               
                resultTableModels[i] = new DefaultTableModel(
                    new String[]{"Proceso", "Información"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            }
            
            JTable table = new JTable(resultTableModels[i]);
            table.setFont(new Font("Arial", Font.PLAIN, 14));
            JScrollPane scrollPane = new JScrollPane(table);
            resultsPanel.add(scrollPane, tableNames[i]);
        }
    }

    private void setupTimeField() {
        txtProcessTime.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                formatTimeField();
            }
        });
    }

    private void setupPriorityFields() {
        KeyListener priorityListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {}
        };
        
        txtPriority.addKeyListener(priorityListener);
        txtPriorityChange.addKeyListener(priorityListener);
    }

    private void formatTimeField() {
        String text = txtProcessTime.getText().replaceAll("[^0-9]", "");
        if (!text.isEmpty()) {
            try {
                long number = Long.parseLong(text);
                String formatted = numberFormatter.format(number);
                
                if (!txtProcessTime.getText().equals(formatted)) {
                    int caretPos = txtProcessTime.getCaretPosition();
                    txtProcessTime.setText(formatted);
                    try {
                        txtProcessTime.setCaretPosition(Math.min(caretPos, formatted.length()));
                    } catch (IllegalArgumentException ex) {
                        txtProcessTime.setCaretPosition(formatted.length());
                    }
                }
            } catch (NumberFormatException ex) {
                
            }
        }
    }

    private int parseTimeField() throws NumberFormatException {
        String text = txtProcessTime.getText().replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            throw new NumberFormatException("Campo vacío");
        }
        while (text.length() > 1) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                text = text.substring(1);
            }
        }        
        return Integer.parseInt(text);
    }

    private void updateReferencedProcessCombo() {
        String selectedItem = (String) cmbReferencedProcess.getSelectedItem();
        cmbReferencedProcess.removeAllItems();
        cmbReferencedProcess.addItem("Ninguno");
        
        for (model.Process p : processManager.getInitialProcesses()) {
            cmbReferencedProcess.addItem(p.getName());
        }
        
        if (selectedItem != null && !selectedItem.equals("Ninguno")) {
            cmbReferencedProcess.setSelectedItem(selectedItem);
        }
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

       
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("SIMULADOR DE PROCESOS AVANZADO");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

      
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        leftPanel.setPreferredSize(new Dimension(500, 0));

        JPanel formPanel = createFormPanel();
        leftPanel.add(formPanel, BorderLayout.NORTH);

        JScrollPane tableScrollPane = new JScrollPane(processTable);
        tableScrollPane.setPreferredSize(new Dimension(480, 250));
        leftPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel actionPanel = createActionPanel();
        leftPanel.add(actionPanel, BorderLayout.SOUTH);

        
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Resultados de la Simulación"));

        JPanel buttonPanel = createResultButtonPanel();
        rightPanel.add(buttonPanel, BorderLayout.NORTH);
        
    
        JPanel reportsPanel = createReportsPanel();
        rightPanel.add(reportsPanel, BorderLayout.SOUTH);
        
        rightPanel.add(resultsPanel, BorderLayout.CENTER);

        add(titlePanel, BorderLayout.NORTH);
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Crear Nuevo Proceso"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int row = 0;
        
    
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessName, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Tiempo:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessTime, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Prioridad:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPriority, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Cambio Prioridad:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPriorityChange, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbStatus, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Suspendido:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbSuspended, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Reanudado:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbResumed, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Destruido:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbDestroyed, gbc);
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Proceso Referencia:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbReferencedProcess, gbc);

        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); 
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnAdd = new JButton("Agregar");
        JButton btnEdit = new JButton("Modificar");
        JButton btnDelete = new JButton("Eliminar");
        JButton btnSimulate = new JButton("Ejecutar Simulación");
        JButton btnReset = new JButton("Limpiar Todo");
        JButton btnExit = new JButton("Salir");
        JButton btnManual = new JButton("Manual de usuario");

        btnAdd.addActionListener(e -> addProcess());
        btnEdit.addActionListener(e -> editProcess());
        btnDelete.addActionListener(e -> deleteProcess());
        btnSimulate.addActionListener(e -> runSimulation());
        btnExit.addActionListener(e -> System.exit(0));
        btnManual.addActionListener(e -> openUserManual());
        btnReset.addActionListener(e -> clearAll());  
        
        panel.add(btnAdd);
        panel.add(Box.createRigidArea(new Dimension(0, 10))); 
        panel.add(btnEdit);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnDelete);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnSimulate);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnManual);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnReset);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(btnExit);
        return panel;
    }

    private JPanel createResultButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 5, 5, 5)); 
        
        for (int i = 0; i < tableNames.length; i++) {
            JButton btn = new JButton(tableNames[i]);
            btn.setPreferredSize(new Dimension(120, 30));
            final int index = i;
            btn.addActionListener(e -> {
                cardLayout.show(resultsPanel, tableNames[index]);
                updateResultTable(index);
            });
            panel.add(btn);
        }

        return panel;
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Reportes Especiales"));
        
        JButton btnRelationsReport = new JButton("Reporte de Referencias");
        btnRelationsReport.addActionListener(e -> showRelationsReport());
        
        JButton btnPriorityReport = new JButton("Reporte de Prioridades");
        btnPriorityReport.addActionListener(e -> showPriorityReport());
        
        panel.add(btnRelationsReport);
        panel.add(btnPriorityReport);
        
        return panel;
    }

    private void setupEventHandlers() {
        
    }

    private void addProcess() {
        String name = txtProcessName.getText().trim();
        String timeText = txtProcessTime.getText().trim();
        String priorityText = txtPriority.getText().trim();

      
        if (name.isEmpty()) {
            showError("El nombre del proceso no puede estar vacío");
            return;
        }

        if (processManager.processExists(name)) {
            showError("Ya existe un proceso con ese nombre");
            return;
        }

        if (priorityText.isEmpty()) {
            showError("Debe ingresar una prioridad");
            return;
        }

        try {
            int time = parseTimeField(); 
            if (time <= 0) {
                showError("El tiempo debe ser mayor a 0");
                return;
            }

            int priority = Integer.parseInt(priorityText);
            if (priority <= 0) {
                showError("La prioridad debe ser mayor a 0");
                return;
            }

            Status status = cmbStatus.getSelectedIndex() == 0 ? 
                Status.NO_BLOQUEADO : Status.BLOQUEADO;
            
            Status suspended = cmbSuspended.getSelectedIndex() == 0 ? 
                Status.NO_SUSPENDIDO : Status.SUSPENDIDO;
            
            Status resumed = cmbResumed.getSelectedIndex() == 0 ? 
                Status.NO_REANUDADO : Status.REANUDADO;
            
            Status destroyed = cmbDestroyed.getSelectedIndex() == 0 ? 
                Status.NO_DESTRUIDO : Status.DESTRUIDO;

           
            if (resumed == Status.REANUDADO && suspended == Status.NO_SUSPENDIDO) {
                showError("Un proceso no puede ser reanudado sin estar suspendido");
                return;
            }

            String referencedProcess = null;
            if (cmbReferencedProcess.getSelectedIndex() > 0) {
                referencedProcess = (String) cmbReferencedProcess.getSelectedItem();
            }

            int finalPriority = priority;
            
       
            String priorityChangeText = txtPriorityChange.getText().trim();
            if (!priorityChangeText.isEmpty()) {
                try {
                    int newPriority = Integer.parseInt(priorityChangeText);
                    if (newPriority != priority && newPriority > 0) {
                        finalPriority = newPriority;
                    }
                } catch (NumberFormatException ex) {
                    showError("Ingrese un valor válido para el cambio de prioridad");
                    return;
                }
            }

          
            processManager.addProcess(name, time, status, priority, finalPriority, 
                                    suspended, resumed, destroyed, referencedProcess);
            
            updateProcessTable();
            updateReferencedProcessCombo();
            clearForm();
            showInfo("Proceso agregado exitosamente");

        } catch (NumberFormatException ex) {
            showError("Ingrese valores numéricos válidos");
        }
    }

    private void editProcess() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Seleccione un proceso para modificar");
            return;
        }

        String oldName = (String) processTableModel.getValueAt(selectedRow, 0);
        model.Process selectedProcess = null;
        
        for (model.Process p : processManager.getInitialProcesses()) {
            if (p.getName().equals(oldName)) {
                selectedProcess = p;
                break;
            }
        }

        if (selectedProcess == null) return;

        
        JDialog editDialog = createEditDialog(selectedProcess, selectedRow);
        editDialog.setVisible(true);
    }

    private JDialog createEditDialog(model.Process process, int selectedRow) {
        JDialog dialog = new JDialog(this, "Modificar Proceso", true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(400, 600);
        dialog.setLocationRelativeTo(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

       
        JTextField txtEditName = new JTextField(process.getName(), 15);
        txtEditName.setEditable(false);
        txtEditName.setBackground(Color.LIGHT_GRAY);

        JTextField txtEditTime = new JTextField(String.valueOf(process.getOriginalTime()), 15);
        JTextField txtEditPriority = new JTextField(String.valueOf(process.getInitialPriority()), 15);
        JTextField txtEditPriorityChange = new JTextField();
        if (process.hasPriorityChange()) {
            txtEditPriorityChange.setText(String.valueOf(process.getFinalPriority()));
        }

        JComboBox<String> cmbEditStatus = new JComboBox<>(new String[]{"No Bloqueado", "Bloqueado"});
        cmbEditStatus.setSelectedIndex(process.isBlocked() ? 1 : 0);

        JComboBox<String> cmbEditSuspended = new JComboBox<>(new String[]{"No", "Si"});
        cmbEditSuspended.setSelectedIndex(process.isSuspended() ? 1 : 0);

        JComboBox<String> cmbEditResumed = new JComboBox<>(new String[]{"No", "Si"});
        cmbEditResumed.setSelectedIndex(process.isResumed() ? 1 : 0);

        JComboBox<String> cmbEditDestroyed = new JComboBox<>(new String[]{"No", "Si"});
        cmbEditDestroyed.setSelectedIndex(process.isDestroyed() ? 1 : 0);

        JComboBox<String> cmbEditReference = new JComboBox<>();
        cmbEditReference.addItem("Ninguno");
        for (model.Process p : processManager.getInitialProcesses()) {
            if (!p.getName().equals(process.getName())) {
                cmbEditReference.addItem(p.getName());
            }
        }
        if (process.hasReference()) {
            cmbEditReference.setSelectedItem(process.getReferencedProcess());
        }

       
        int row = 0;
        addDialogComponent(dialog, gbc, "Nombre:", txtEditName, row++);
        addDialogComponent(dialog, gbc, "Tiempo:", txtEditTime, row++);
        addDialogComponent(dialog, gbc, "Prioridad:", txtEditPriority, row++);
        addDialogComponent(dialog, gbc, "Cambio Prioridad:", txtEditPriorityChange, row++);
        addDialogComponent(dialog, gbc, "Estado:", cmbEditStatus, row++);
        addDialogComponent(dialog, gbc, "Suspendido:", cmbEditSuspended, row++);
        addDialogComponent(dialog, gbc, "Reanudado:", cmbEditResumed, row++);
        addDialogComponent(dialog, gbc, "Destruido:", cmbEditDestroyed, row++);
        addDialogComponent(dialog, gbc, "Referencia:", cmbEditReference, row++);

      
        JPanel buttonPanel = new JPanel();
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");

        btnSave.addActionListener(e -> {
            if (saveEditedProcess(dialog, process, selectedRow, txtEditTime, txtEditPriority, 
                                txtEditPriorityChange, cmbEditStatus, cmbEditSuspended, 
                                cmbEditResumed, cmbEditDestroyed, cmbEditReference)) {
                dialog.dispose();
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        dialog.add(buttonPanel, gbc);

        return dialog;
    }

    private void addDialogComponent(JDialog dialog, GridBagConstraints gbc, String label, JComponent component, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        dialog.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        dialog.add(component, gbc);
    }

    private boolean saveEditedProcess(JDialog dialog, model.Process originalProcess, int selectedRow,
                                    JTextField txtTime, JTextField txtPriority, JTextField txtPriorityChange,
                                    JComboBox<String> cmbStatus, JComboBox<String> cmbSuspended,
                                    JComboBox<String> cmbResumed, JComboBox<String> cmbDestroyed,
                                    JComboBox<String> cmbReference) {
        try {
            int newTime = Integer.parseInt(txtTime.getText().trim());
            if (newTime <= 0) {
                showError("El tiempo debe ser mayor a 0");
                return false;
            }

            int newPriority = Integer.parseInt(txtPriority.getText().trim());
            if (newPriority <= 0) {
                showError("La prioridad debe ser mayor a 0");
                return false;
            }

            Status newStatus = cmbStatus.getSelectedIndex() == 0 ? Status.NO_BLOQUEADO : Status.BLOQUEADO;
            Status newSuspended = cmbSuspended.getSelectedIndex() == 0 ? Status.NO_SUSPENDIDO : Status.SUSPENDIDO;
            Status newResumed = cmbResumed.getSelectedIndex() == 0 ? Status.NO_REANUDADO : Status.REANUDADO;
            Status newDestroyed = cmbDestroyed.getSelectedIndex() == 0 ? Status.NO_DESTRUIDO : Status.DESTRUIDO;

            if (newResumed == Status.REANUDADO && newSuspended == Status.NO_SUSPENDIDO) {
                showError("Un proceso no puede ser reanudado sin estar suspendido");
                return false;
            }

            String newReference = null;
            if (cmbReference.getSelectedIndex() > 0) {
                newReference = (String) cmbReference.getSelectedItem();
            }

            int finalPriority = newPriority;
            String priorityChangeText = txtPriorityChange.getText().trim();
            if (!priorityChangeText.isEmpty()) {
                try {
                    int changePriority = Integer.parseInt(priorityChangeText);
                    if (changePriority != newPriority && changePriority > 0) {
                        finalPriority = changePriority;
                    }
                } catch (NumberFormatException ex) {
                    showError("Ingrese un valor válido para el cambio de prioridad");
                    return false;
                }
            }

            processManager.editProcess(selectedRow, originalProcess.getName(), newTime, newStatus,
                                     finalPriority, newSuspended, newResumed, newDestroyed, newReference);
            
            updateProcessTable();
            updateReferencedProcessCombo();
            showInfo("Proceso editado exitosamente");
            return true;

        } catch (NumberFormatException ex) {
            showError("Ingrese valores numéricos válidos");
            return false;
        }
    }

    private void deleteProcess() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Seleccione un proceso para eliminar");
            return;
        }

        String processName = (String) processTableModel.getValueAt(selectedRow, 0);
        currentAction = "DELETE_PROCESS:" + processName;
        
        new CustomDialog(this, "¿Está seguro de que desea eliminar el proceso '" + processName + "'?", CustomDialog.CONFIRM_TYPE);
    }

    private void runSimulation() {
        if (processManager.isEmpty()) {
            showError("No hay procesos para simular");
            return;
        }

        
        List<model.Process> priorityChangesBeforeSim = processManager.getProcessesWithPriorityChanges();
        System.out.println("Procesos con cambio de prioridad antes de simulación: " + priorityChangesBeforeSim.size());
        for (model.Process p : priorityChangesBeforeSim) {
            System.out.println("- " + p.getName() + ": " + p.getInitialPriority() + " -> " + p.getFinalPriority());
        }

        processManager.runSimulation();
        
       
        List<Log> priorityLogs = processManager.getLogsByFilter(Filter.PRIORIDAD_CAMBIADA);
        System.out.println("Logs de prioridad cambiada después de simulación: " + priorityLogs.size());
        
      
        for (int i = 0; i < tableNames.length; i++) {
            updateResultTable(i);
        }
        
        cardLayout.show(resultsPanel, tableNames[1]); 
        showInfo("Simulación ejecutada exitosamente. Procesos con cambio de prioridad: " + priorityChangesBeforeSim.size());
    }

    private void updateProcessTable() {
        processTableModel.setRowCount(0);
        for (model.Process p : processManager.getInitialProcesses()) {
            String formattedTime = numberFormatter.format(p.getOriginalTime());
            String reference = p.hasReference() ? p.getReferencedProcess() : "Ninguno";
            String priorityDisplay = String.valueOf(p.getFinalPriority());
            if (p.hasPriorityChange()) {
                priorityDisplay += " (" + p.getInitialPriority() + ")";
            }
            
            processTableModel.addRow(new Object[]{
                p.getName(),
                formattedTime,
                priorityDisplay,
                p.getStatusString(),
                p.getSuspendedString(),
                p.getResumedString(),
                p.getDestroyedString(),
                reference
            });
        }
    }

    private void updateResultTable(int tableIndex) {
        if (tableIndex == 0) {
           
            resultTableModels[0].setRowCount(0);
            for (model.Process p : processManager.getInitialProcesses()) {
                String formattedTime = numberFormatter.format(p.getOriginalTime());
                resultTableModels[0].addRow(new Object[]{
                    p.getName(), formattedTime, p.getFinalPriority(), p.getStatusString(), 0
                });
            }
        } else if (tableIndex < 8) {
            
            List<Log> logs = processManager.getLogsByFilter(filters[tableIndex]);
            resultTableModels[tableIndex].setRowCount(0);
            for (Log log : logs) {
                String formattedTime = numberFormatter.format(log.getRemainingTime());
                resultTableModels[tableIndex].addRow(new Object[]{
                    log.getProcessName(),
                    formattedTime,
                    log.getPriority(),
                    log.getStatusString(),
                    log.getCycleCount()
                });
            }
        } else {
           
            updateSpecialReportTable(tableIndex);
        }
    }

    private void updateSpecialReportTable(int tableIndex) {
        resultTableModels[tableIndex].setRowCount(0);
        
        switch (tableIndex) {
            case 8: 
                List<model.Process> priorityChanges = processManager.getProcessesWithPriorityChanges();
                for (model.Process p : priorityChanges) {
                    resultTableModels[tableIndex].addRow(new Object[]{
                        p.getName(),
                        "Prioridad: " + p.getInitialPriority() + " → " + p.getFinalPriority()
                    });
                }
                break;
            case 9: 
                List<model.Process> suspended = processManager.getSuspendedProcesses();
                for (model.Process p : suspended) {
                    resultTableModels[tableIndex].addRow(new Object[]{
                        p.getName(), "Proceso suspendido"
                    });
                }
                break;
            case 10: 
                List<model.Process> resumed = processManager.getResumedProcesses();
                for (model.Process p : resumed) {
                    resultTableModels[tableIndex].addRow(new Object[]{
                        p.getName(), "Proceso reanudado"
                    });
                }
                break;
            case 11: 
                List<model.Process> destroyed = processManager.getDestroyedProcesses();
                for (model.Process p : destroyed) {
                    resultTableModels[tableIndex].addRow(new Object[]{
                        p.getName(), "Proceso destruido"
                    });
                }
                break;
        }
    }

    private void showRelationsReport() {
        List<String> relations = processManager.getProcessRelationsReport();
        
        if (relations.isEmpty()) {
            showInfo("No hay referencias entre procesos");
            return;
        }

        StringBuilder report = new StringBuilder("REPORTE DE REFERENCIAS:\n\n");
        for (String relation : relations) {
            report.append(relation).append("\n");
        }

        JDialog dialog = new JDialog(this, "Reporte de Referencias", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);
        
        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        dialog.add(new JScrollPane(textArea));
        dialog.setVisible(true);
    }

    private void showPriorityReport() {
        List<model.Process> priorityChanges = processManager.getProcessesWithPriorityChanges();
        
        if (priorityChanges.isEmpty()) {
            showInfo("No hay cambios de prioridad");
            return;
        }

        StringBuilder report = new StringBuilder("REPORTE DE CAMBIOS DE PRIORIDAD:\n\n");
        for (model.Process p : priorityChanges) {
            report.append("Proceso: ").append(p.getName())
                  .append(" | Prioridad inicial: ").append(p.getInitialPriority())
                  .append(" | Prioridad final: ").append(p.getFinalPriority())
                  .append("\n");
        }

        JDialog dialog = new JDialog(this, "Reporte de Prioridades", true);
        dialog.setSize(500, 300);
        dialog.setLocationRelativeTo(this);
        
        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        dialog.add(new JScrollPane(textArea));
        dialog.setVisible(true);
    }

    private void clearAll() {
        currentAction = "CLEAR_ALL";
        new CustomDialog(this, "¿Está seguro de que desea eliminar todos los procesos?", CustomDialog.CONFIRM_TYPE);
    }

    private void clearForm() {
        txtProcessName.setText("");
        txtProcessTime.setText("");
        txtPriority.setText("1");
        txtPriorityChange.setText("");
        cmbStatus.setSelectedIndex(0);
        cmbSuspended.setSelectedIndex(0);
        cmbResumed.setSelectedIndex(0);
        cmbDestroyed.setSelectedIndex(0);
        cmbReferencedProcess.setSelectedIndex(0);
    }

    private void openUserManual() {
        try {
            File manualFile = new File("Manual_Usuario.pdf");
            
            if (!manualFile.exists()) {
                showError("No se encontró el archivo del manual de usuario.<br>" +
                         "Asegúrese de que el archivo 'Manual_Usuario.pdf'<br>" +
                         "esté en la misma carpeta que el programa.");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(manualFile);
                } else {
                    showError("Su sistema no permite abrir archivos PDF automáticamente.<br>" +
                             "Por favor, abra manualmente el archivo:<br>" +
                             "Manual_Usuario_Simulador_Procesos.pdf");
                }
            } else {
                showError("Su sistema no permite abrir archivos automáticamente.<br>" +
                         "Por favor, abra manualmente el archivo:<br>" +
                         manualFile.getAbsolutePath());
            }
            
        } catch (IOException ex) {
            showError("Error al abrir el manual de usuario:<br>" + ex.getMessage());
        } catch (Exception ex) {
            showError("Error inesperado al abrir el manual:<br>" + ex.getMessage());
        }
    }

    private void showError(String message) {
        new CustomDialog(this, message, CustomDialog.WARNING_TYPE);
    }

    private void showInfo(String message) {
        new CustomDialog(this, message, CustomDialog.INFO_TYPE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        
        switch(command) {
            case Constants.CLOSE_WARNING:
            case Constants.CLOSE_INFO:
                ((JDialog)((JButton)e.getSource()).getTopLevelAncestor()).dispose();
                break;
                
            case Constants.CONFIRM_YES:
                handleConfirmYes();
                ((JDialog)((JButton)e.getSource()).getTopLevelAncestor()).dispose();
                break;
                
            case Constants.CONFIRM_NO:
                ((JDialog)((JButton)e.getSource()).getTopLevelAncestor()).dispose();
                break;
        }
    }
    
    private void handleConfirmYes() {
        if (currentAction != null) {
            if (currentAction.startsWith("DELETE_PROCESS:")) {
                String processName = currentAction.substring("DELETE_PROCESS:".length());
                processManager.removeProcess(processName);
                updateProcessTable();
                updateReferencedProcessCombo();
                showInfo("Proceso eliminado");
            } else if (currentAction.equals("CLEAR_ALL")) {
                processManager.clearAll();
                updateProcessTable();
                updateReferencedProcessCombo();
                
                for (DefaultTableModel model : resultTableModels) {
                    model.setRowCount(0);
                }
                
                clearForm();
                showInfo("Todos los datos han sido eliminados");
            }
            currentAction = null;
        }
    }
}