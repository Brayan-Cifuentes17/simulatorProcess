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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProcessSimulatorGUI extends JFrame implements ActionListener {
    private ProcessManager processManager;
    
    // Input components
    private JTextField txtProcessName;
    private JTextField txtProcessTime;
    private JTextField txtPriority;
    private JTextField txtPriorityChange;
    private JComboBox<String> cmbStatus;
    private JComboBox<String> cmbSuspended;
    private JComboBox<String> cmbResumed;
    private JComboBox<String> cmbDestroyed;
    private JList<String> listReferencedProcesses;  // Cambio de JComboBox a JList
    private DefaultListModel<String> referencedProcessesModel;
    
    // Process table
    private DefaultTableModel processTableModel;
    private JTable processTable;
    
    // Results panel
    private JPanel resultsPanel;
    private CardLayout cardLayout;
    
    // Result tables
    private DefaultTableModel[] resultTableModels;
    private String[] tableNames = {
        "Inicial", "Listos", "Despachados", "En Ejecución", 
        "Tiempo Expirado", "Bloqueados", "Despertar", "Finalizados",
        "Prioridad Cambiada", "Suspendidos", "Reanudados", "Destruidos", "Relacion-Comunicacion"
    };
    private Filter[] filters = {
        Filter.INICIAL, Filter.LISTO, Filter.DESPACHADO, Filter.EN_EJECUCION,
        Filter.TIEMPO_EXPIRADO, Filter.BLOQUEADO, Filter.DESPERTAR, Filter.FINALIZADO,
        Filter.PRIORIDAD_CAMBIADA, Filter.SUSPENDIDO, Filter.REANUDADO, Filter.DESTRUIDO,
        Filter.TODO 
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

        // Input components
        txtProcessName = new JTextField(15);
        txtProcessTime = new JTextField(15);
        txtPriority = new JTextField(15);
        txtPriorityChange = new JTextField(15);
        cmbStatus = new JComboBox<>(new String[]{"No Bloqueado", "Bloqueado"});
        cmbSuspended = new JComboBox<>(new String[]{"No", "Si"});
        cmbResumed = new JComboBox<>(new String[]{"No", "Si"});
        cmbDestroyed = new JComboBox<>(new String[]{"No", "Si"});
        
        // Lista múltiple para procesos referenciados
        referencedProcessesModel = new DefaultListModel<>();
        listReferencedProcesses = new JList<>(referencedProcessesModel);
        listReferencedProcesses.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listReferencedProcesses.setVisibleRowCount(3);

        setupTimeField();
        setupPriorityFields();
        updateReferencedProcessList();

        // Process table
        processTableModel = new DefaultTableModel(
            new String[]{"Nombre", "Tiempo", "Prioridad", "Estado", "Suspendido", "Reanudado", "Destruido", "Comunicacion"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        processTable = new JTable(processTableModel);
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Results panel
        cardLayout = new CardLayout();
        resultsPanel = new JPanel(cardLayout);
        
        // Result tables
        resultTableModels = new DefaultTableModel[tableNames.length];
        for (int i = 0; i < tableNames.length; i++) {
            if (i < 8) {
                // Standard tables
                resultTableModels[i] = new DefaultTableModel(
                    new String[]{"Proceso", "Tiempo Restante", "Prioridad", "Estado", "Suspendido", "Reanudado", "Destruido", "Comunicacion", "Ciclos"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            } else if (i == 8 || i == 12) {
                // Special report tables
                resultTableModels[i] = new DefaultTableModel(
                    new String[]{"Proceso", "Información"}, 0) {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
            } else {
                // Status report tables
                resultTableModels[i] = new DefaultTableModel(
                    new String[]{"Proceso", "Tiempo Original", "Prioridad", "Estado", "Suspendido", "Reanudado", "Destruido", "Comunicacion", "Información"}, 0) {
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
                
                // Formatear inmediatamente después de que se agregue el carácter
                SwingUtilities.invokeLater(() -> formatTimeField());
            }

            @Override
            public void keyPressed(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                // Mantener el formateo también aquí como respaldo
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
                // Limitar a 18 dígitos para evitar overflow
                if (text.length() > 18) {
                    text = text.substring(0, 18);
                }
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
                // Manejar error silenciosamente
            }
        }
    }

    private long parseTimeWithTrick(String timeText) throws NumberFormatException {
        String numbersOnly = timeText.replaceAll("[^0-9]", "");
        if (numbersOnly.isEmpty()) {
            throw new NumberFormatException("Campo vacío");
        }
        
        // TRAMPA: El usuario puede escribir 99999999999999999999999999...
        // Pero nosotros silenciosamente tomamos solo los primeros 18 dígitos
        if (numbersOnly.length() > 18) {
            numbersOnly = numbersOnly.substring(0, 18);
            System.out.println("DEBUG: Número truncado silenciosamente a 18 dígitos: " + numbersOnly);
        }
        
        // Intentar parsear el número, si falla intentar con menos dígitos
        while (numbersOnly.length() > 1) {
            try {
                long result = Long.parseLong(numbersOnly);
                System.out.println("DEBUG: Número parseado exitosamente: " + result);
                return result;
            } catch (NumberFormatException ex) {
                // Si falla, quitar el primer dígito e intentar de nuevo
                numbersOnly = numbersOnly.substring(1); 
                System.out.println("DEBUG: Reintentando con: " + numbersOnly);
            }
        }
        
        return Long.parseLong(numbersOnly);
    }

    private long parseTimeField() throws NumberFormatException {
        return parseTimeWithTrick(txtProcessTime.getText());
    }

    private long parseTimeFieldForDialog(JTextField timeField) throws NumberFormatException {
        return parseTimeWithTrick(timeField.getText());
    }

    private void updateReferencedProcessList() {
        referencedProcessesModel.clear();
        for (model.Process p : processManager.getInitialProcesses()) {
            referencedProcessesModel.addElement(p.getName());
        }
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("SIMULADOR DE PROCESOS");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        // Left panel
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

        // Right panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Resultados de la Simulación"));

        JPanel buttonPanel = createResultButtonPanel();
        rightPanel.add(buttonPanel, BorderLayout.NORTH);
        
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
        
        // Name
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessName, gbc);
        row++;

        // Time
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Tiempo:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessTime, gbc);
        row++;

        // Priority
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Prioridad:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPriority, gbc);
        row++;

        // Priority change
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Cambio Prioridad:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPriorityChange, gbc);
        row++;

        // Status
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbStatus, gbc);
        row++;

        // Suspended
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Suspendido:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbSuspended, gbc);
        row++;

        // Resumed
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Reanudado:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbResumed, gbc);
        row++;

        // Destroyed
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Destruido:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbDestroyed, gbc);
        row++;

        // Referenced processes (multiple selection)
        gbc.gridx = 0; gbc.gridy = row;
        panel.add(new JLabel("Comunicar procesos:"), gbc);
        gbc.gridx = 1;
        JScrollPane scrollPane = new JScrollPane(listReferencedProcesses);
        scrollPane.setPreferredSize(new Dimension(200, 60));
        panel.add(scrollPane, gbc);

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

    private void setupEventHandlers() {
        // Event handlers setup if needed
    }

    private void addProcess() {
        String name = txtProcessName.getText().trim();
        String timeText = txtProcessTime.getText().trim();
        String priorityText = txtPriority.getText().trim();

        // Validation
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
            long time = parseTimeField(); 
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

            // Validation for resumed without suspended
            if (resumed == Status.REANUDADO && suspended == Status.NO_SUSPENDIDO) {
                showError("Un proceso no puede ser reanudado sin estar suspendido");
                return;
            }

            // Get selected referenced processes
            List<String> selectedProcesses = listReferencedProcesses.getSelectedValuesList();
            String referencedProcesses = selectedProcesses.isEmpty() ? null : String.join(",", selectedProcesses);

            int finalPriority = priority;
            
            // Priority change
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

            // Add process
            processManager.addProcess(name, time, status, priority, finalPriority, 
                                    suspended, resumed, destroyed, referencedProcesses);
            
            updateProcessTable();
            updateReferencedProcessList();
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

        // Create edit dialog
        JDialog editDialog = createEditDialog(selectedProcess, selectedRow);
        editDialog.setVisible(true);
    }

    private JDialog createEditDialog(model.Process process, int selectedRow) {
        JDialog dialog = new JDialog(this, "Modificar Proceso", true);
        dialog.setLayout(new GridBagLayout());
        dialog.setSize(450, 600);  // Ajustado para incluir lista múltiple
        dialog.setLocationRelativeTo(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10); 

        // Name (read-only)
        JTextField txtEditName = new JTextField(process.getName(), 20); 
        txtEditName.setEditable(false);
        txtEditName.setBackground(Color.LIGHT_GRAY);

        // Time
        JTextField txtEditTime = new JTextField(String.valueOf(process.getOriginalTime()), 20);
        
        // Solo un campo de prioridad que muestre la prioridad final
        JTextField txtEditPriority = new JTextField(String.valueOf(process.getFinalPriority()), 20);

        // Status combo boxes
        JComboBox<String> cmbEditStatus = new JComboBox<>(new String[]{"No Bloqueado", "Bloqueado"});
        cmbEditStatus.setSelectedIndex(process.isBlocked() ? 1 : 0);
        cmbEditStatus.setPreferredSize(new Dimension(200, 25)); 

        JComboBox<String> cmbEditSuspended = new JComboBox<>(new String[]{"No", "Si"});
        cmbEditSuspended.setSelectedIndex(process.isSuspended() ? 1 : 0);
        cmbEditSuspended.setPreferredSize(new Dimension(200, 25));

        JComboBox<String> cmbEditResumed = new JComboBox<>(new String[]{"No", "Si"});
        cmbEditResumed.setSelectedIndex(process.isResumed() ? 1 : 0);
        cmbEditResumed.setPreferredSize(new Dimension(200, 25));

        JComboBox<String> cmbEditDestroyed = new JComboBox<>(new String[]{"No", "Si"});
        cmbEditDestroyed.setSelectedIndex(process.isDestroyed() ? 1 : 0);
        cmbEditDestroyed.setPreferredSize(new Dimension(200, 25));

        // Lista múltiple para procesos referenciados en edición
        DefaultListModel<String> editReferencedModel = new DefaultListModel<>();
        JList<String> listEditReferences = new JList<>(editReferencedModel);
        listEditReferences.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listEditReferences.setVisibleRowCount(3);
        
        // Poblar lista y seleccionar procesos actuales
        for (model.Process p : processManager.getInitialProcesses()) {
            if (!p.getName().equals(process.getName())) {
                editReferencedModel.addElement(p.getName());
            }
        }
        
        // Seleccionar procesos referenciados actuales
        if (process.hasReference()) {
            String[] currentRefs = process.getReferencedProcess().split(",");
            for (String ref : currentRefs) {
                int index = editReferencedModel.indexOf(ref.trim());
                if (index >= 0) {
                    listEditReferences.addSelectionInterval(index, index);
                }
            }
        }

        // Add components to dialog
        int row = 0;
        addDialogComponent(dialog, gbc, "Nombre:", txtEditName, row++);
        addDialogComponent(dialog, gbc, "Tiempo:", txtEditTime, row++);
        addDialogComponent(dialog, gbc, "Prioridad:", txtEditPriority, row++);  // Solo un campo
        addDialogComponent(dialog, gbc, "Estado:", cmbEditStatus, row++);
        addDialogComponent(dialog, gbc, "Suspendido:", cmbEditSuspended, row++);
        addDialogComponent(dialog, gbc, "Reanudado:", cmbEditResumed, row++);
        addDialogComponent(dialog, gbc, "Destruido:", cmbEditDestroyed, row++);
        
        // Referencias múltiples
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST; 
        gbc.fill = GridBagConstraints.NONE;
        
        JLabel lblReference = new JLabel("Comunicacion:");
        lblReference.setPreferredSize(new Dimension(120, 25)); 
        dialog.add(lblReference, gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST; 
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JScrollPane editScrollPane = new JScrollPane(listEditReferences);
        editScrollPane.setPreferredSize(new Dimension(200, 60));
        dialog.add(editScrollPane, gbc);
        row++;

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        
        Dimension buttonSize = new Dimension(100, 30);
        btnSave.setPreferredSize(buttonSize);
        btnCancel.setPreferredSize(buttonSize);

        btnSave.addActionListener(e -> {
            if (saveEditedProcess(dialog, process, selectedRow, txtEditTime, txtEditPriority, 
                                cmbEditStatus, cmbEditSuspended, 
                                cmbEditResumed, cmbEditDestroyed, listEditReferences)) {
                dialog.dispose();
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10); 
        dialog.add(buttonPanel, gbc);

        // Key bindings
        dialog.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnSave.doClick(); 
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dialog.dispose(); 
                }
            }
            
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyReleased(KeyEvent e) {}
        });
        
        dialog.setFocusable(true);
        dialog.requestFocus();

        return dialog;
    }

    private void addDialogComponent(JDialog dialog, GridBagConstraints gbc, String label, JComponent component, int row) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST; 
        gbc.fill = GridBagConstraints.NONE;
        
        JLabel lblComponent = new JLabel(label);
        lblComponent.setPreferredSize(new Dimension(120, 25)); 
        dialog.add(lblComponent, gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST; 
        gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(component, gbc);
    }

    private boolean saveEditedProcess(JDialog dialog, model.Process originalProcess, int selectedRow,
                                JTextField txtTime, JTextField txtPriority,
                                JComboBox<String> cmbStatus, JComboBox<String> cmbSuspended,
                                JComboBox<String> cmbResumed, JComboBox<String> cmbDestroyed,
                                JList<String> listReferences) {
        try {
            // Parse time
            long newTime = parseTimeFieldForDialog(txtTime);
            if (newTime <= 0) {
                showError("El tiempo debe ser mayor a 0");
                return false;
            }

            // Parse priority
            int newPriority = Integer.parseInt(txtPriority.getText().trim());
            if (newPriority <= 0) {
                showError("La prioridad debe ser mayor a 0");
                return false;
            }

            // Get status values
            Status newStatus = cmbStatus.getSelectedIndex() == 0 ? Status.NO_BLOQUEADO : Status.BLOQUEADO;
            Status newSuspended = cmbSuspended.getSelectedIndex() == 0 ? Status.NO_SUSPENDIDO : Status.SUSPENDIDO;
            Status newResumed = cmbResumed.getSelectedIndex() == 0 ? Status.NO_REANUDADO : Status.REANUDADO;
            Status newDestroyed = cmbDestroyed.getSelectedIndex() == 0 ? Status.NO_DESTRUIDO : Status.DESTRUIDO;

            // Validation
            if (newResumed == Status.REANUDADO && newSuspended == Status.NO_SUSPENDIDO) {
                showError("Un proceso no puede ser reanudado sin estar suspendido");
                return false;
            }

            // Get multiple references
            List<String> selectedRefs = listReferences.getSelectedValuesList();
            String newReferences = selectedRefs.isEmpty() ? null : String.join(",", selectedRefs);

            // Update process
            processManager.editProcess(selectedRow, originalProcess.getName(), newTime, newStatus,
                                     newPriority, newSuspended, newResumed, newDestroyed, newReferences);
            
            updateProcessTable();
            updateReferencedProcessList();
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
        
        // Check if process is referenced by others
        if (processManager.isProcessReferenced(processName)) {
            showError("No se puede eliminar el proceso '" + processName + "' porque está siendo comunicado por otros procesos.");
            return;
        }
        
        currentAction = "DELETE_PROCESS:" + processName;
        new CustomDialog(this, "¿Está seguro de que desea eliminar el proceso '" + processName + "'?", CustomDialog.CONFIRM_TYPE);
    }

    private void runSimulation() {
        if (processManager.isEmpty()) {
            showError("No hay procesos para simular");
            return;
        }

        // Debug: Check priority changes before simulation
        List<model.Process> priorityChangesBeforeSim = processManager.getProcessesWithPriorityChanges();
        System.out.println("Procesos con cambio de prioridad antes de simulación: " + priorityChangesBeforeSim.size());
        for (model.Process p : priorityChangesBeforeSim) {
            System.out.println("- " + p.getName() + ": " + p.getInitialPriority() + " -> " + p.getFinalPriority());
        }

        processManager.runSimulation();
        
        // Debug: Check priority logs after simulation
        List<Log> priorityLogs = processManager.getLogsByFilter(Filter.PRIORIDAD_CAMBIADA);
        System.out.println("Logs de prioridad cambiada después de simulación: " + priorityLogs.size());
        
        // Update all result tables
        for (int i = 0; i < tableNames.length; i++) {
            updateResultTable(i);
        }
        
        cardLayout.show(resultsPanel, tableNames[0]); 
        showInfo("Simulación ejecutada exitosamente." );
    }

    private void updateProcessTable() {
        processTableModel.setRowCount(0);
        
        // NO ordenar por prioridad, mantener orden de inserción original
        for (model.Process p : processManager.getInitialProcesses()) {
            String formattedTime = numberFormatter.format(p.getOriginalTime());
            String reference = p.hasReference() ? p.getReferencedProcess() : "Ninguno";
            
            // Solo mostrar prioridad final, sin paréntesis
            String priorityDisplay = String.valueOf(p.getFinalPriority());
            
            processTableModel.addRow(new Object[]{
                p.getName(),
                formattedTime,
                priorityDisplay,  // Sin mostrar prioridad anterior entre paréntesis
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
            // Initial table - show processes in insertion order (not sorted by priority)
            resultTableModels[0].setRowCount(0);
            
            // No sorting, maintain original insertion order
            for (model.Process p : processManager.getInitialProcesses()) {
                String formattedTime = numberFormatter.format(p.getOriginalTime());
                String reference = p.hasReference() ? p.getReferencedProcess() : "Ninguno";
                
                resultTableModels[0].addRow(new Object[]{
                    p.getName(),                    
                    formattedTime,                  
                    p.getFinalPriority(),          
                    p.getStatusString(),           
                    p.getSuspendedString(),       
                    p.getResumedString(),          
                    p.getDestroyedString(),        
                    reference,                      
                    0                             
                });
            }
        } else if (tableIndex < 8) {
            // Standard execution logs
            List<Log> logs = processManager.getLogsByFilter(filters[tableIndex]);
            resultTableModels[tableIndex].setRowCount(0);
            for (Log log : logs) {
                String formattedTime = numberFormatter.format(log.getRemainingTime());
                
                // Get original process info
                model.Process originalProcess = null;
                for (model.Process p : processManager.getInitialProcesses()) {
                    if (p.getName().equals(log.getProcessName())) {
                        originalProcess = p;
                        break;
                    }
                }
                
                String reference = "Ninguno";
                String suspended = "No";
                String resumed = "No";
                String destroyed = "No";
                
                if (originalProcess != null) {
                    reference = originalProcess.hasReference() ? originalProcess.getReferencedProcess() : "Ninguno";
                    suspended = originalProcess.getSuspendedString();
                    resumed = originalProcess.getResumedString();
                    destroyed = originalProcess.getDestroyedString();
                }
                
                resultTableModels[tableIndex].addRow(new Object[]{
                    log.getProcessName(),         
                    formattedTime,                
                    log.getPriority(),            
                    log.getStatusString(),        
                    suspended,                    
                    resumed,                       
                    destroyed,                    
                    reference,                    
                    log.getCycleCount()          
                });
            }
        } else {
            // Special reports
            updateSpecialReportTable(tableIndex);
        }
    }

    private void updateSpecialReportTable(int tableIndex) {
        resultTableModels[tableIndex].setRowCount(0);
        
        switch (tableIndex) {
            case 8: // Priority changes
                List<model.Process> priorityChanges = processManager.getProcessesWithPriorityChanges();
                for (model.Process p : priorityChanges) {
                    resultTableModels[tableIndex].addRow(new Object[]{
                        p.getName(),
                        "Prioridad: " + p.getInitialPriority() + " → " + p.getFinalPriority()
                    });
                }
                break;
                
            case 9: // Suspended
                List<model.Process> suspended = processManager.getSuspendedProcesses();
                if (suspended.isEmpty()) {
                    resultTableModels[tableIndex].addRow(new Object[]{
                        "", ""
                    });
                } else {
                    // Ensure full columns for suspended processes
                    if (resultTableModels[tableIndex].getColumnCount() == 2) {
                        String[] fullColumns = {"Proceso", "Tiempo Original", "Prioridad", "Estado", "Suspendido", "Reanudado", "Destruido", "Comunicacion", "Información"};
                        resultTableModels[tableIndex].setColumnIdentifiers(fullColumns);
                    }
                    
                    for (model.Process p : suspended) {
                        String formattedTime = numberFormatter.format(p.getOriginalTime());
                        String reference = p.hasReference() ? p.getReferencedProcess() : "Ninguno";
                        
                        resultTableModels[tableIndex].addRow(new Object[]{
                            p.getName(),                    
                            formattedTime,                  
                            p.getFinalPriority(),          
                            p.getStatusString(),           
                            p.getSuspendedString(),        
                            p.getResumedString(),          
                            p.getDestroyedString(),        
                            reference,                    
                            "Proceso suspendido"          
                        });
                    }
                }
                break;
                
            case 10: // Resumed
                List<model.Process> resumed = processManager.getResumedProcesses();
                if (resumed.isEmpty()) {
                    resultTableModels[tableIndex].addRow(new Object[]{
                        "", ""
                    });
                } else {
                    // Ensure full columns for resumed processes
                    if (resultTableModels[tableIndex].getColumnCount() == 2) {
                        String[] fullColumns = {"Proceso", "Tiempo Original", "Prioridad", "Estado", "Suspendido", "Reanudado", "Destruido", "Comunicacion", "Información"};
                        resultTableModels[tableIndex].setColumnIdentifiers(fullColumns);
                    }
                    
                    for (model.Process p : resumed) {
                        String formattedTime = numberFormatter.format(p.getOriginalTime());
                        String reference = p.hasReference() ? p.getReferencedProcess() : "Ninguno";
                        
                        resultTableModels[tableIndex].addRow(new Object[]{
                            p.getName(),                  
                            formattedTime,                  
                            p.getFinalPriority(),        
                            p.getStatusString(),           
                            p.getSuspendedString(),       
                            p.getResumedString(),          
                            p.getDestroyedString(),        
                            reference,                    
                            "Proceso reanudado"           
                        });
                    }
                }
                break;
                
            case 11: // Destroyed
                List<model.Process> destroyed = processManager.getDestroyedProcesses();
                if (destroyed.isEmpty()) {
                    resultTableModels[tableIndex].addRow(new Object[]{
                        "", ""
                    });
                } else {
                    // Ensure full columns for destroyed processes
                    if (resultTableModels[tableIndex].getColumnCount() == 2) {
                        String[] fullColumns = {"Proceso", "Tiempo Original", "Prioridad", "Estado", "Suspendido", "Reanudado", "Destruido", "Comunicacion", "Información"};
                        resultTableModels[tableIndex].setColumnIdentifiers(fullColumns);
                    }
                    
                    for (model.Process p : destroyed) {
                        String formattedTime = numberFormatter.format(p.getOriginalTime());
                        String reference = p.hasReference() ? p.getReferencedProcess() : "Ninguno";
                        
                        resultTableModels[tableIndex].addRow(new Object[]{
                            p.getName(),                   
                            formattedTime,                  
                            p.getFinalPriority(),          
                            p.getStatusString(),           
                            p.getSuspendedString(),        
                            p.getResumedString(),         
                            p.getDestroyedString(),        
                            reference,                      
                            "Proceso destruido"          
                        });
                    }
                }
                break;
                
            case 12: // Relations/Communication
                List<String> relations = processManager.getProcessRelationsReport();
                if (relations.isEmpty()) {
                    resultTableModels[tableIndex].addRow(new Object[]{
                        "Sin Comunicacion", "No hay procesos con referencias"
                    });
                } else {
                    for (String relation : relations) {
                        String[] parts = relation.split(" -> ");
                        if (parts.length == 2) {
                            resultTableModels[tableIndex].addRow(new Object[]{
                                parts[0], "Comunica con: " + parts[1]
                            });
                        }
                    }
                }
                break;
        }
    }

    private void clearAll() {
        currentAction = "CLEAR_ALL";
        new CustomDialog(this, "¿Está seguro de que desea eliminar todos los procesos?", CustomDialog.CONFIRM_TYPE);
    }

    private void clearForm() {
        txtProcessName.setText("");
        txtProcessTime.setText("");
        txtPriority.setText("");
        txtPriorityChange.setText("");
        cmbStatus.setSelectedIndex(0);
        cmbSuspended.setSelectedIndex(0);
        cmbResumed.setSelectedIndex(0);
        cmbDestroyed.setSelectedIndex(0);
        listReferencedProcesses.clearSelection();  // Limpiar selección de lista múltiple
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
                updateReferencedProcessList();
                showInfo("Proceso eliminado");
            } else if (currentAction.equals("CLEAR_ALL")) {
                processManager.clearAll();
                updateProcessTable();
                updateReferencedProcessList();
                
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