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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProcessSimulatorGUI extends JFrame implements ActionListener {
    private ProcessManager processManager;
    
   
    private JTextField txtProcessName;
    private JTextField txtProcessTime;
    private JComboBox<String> cmbStatus;
    
    
    private DefaultTableModel processTableModel;
    private JTable processTable;
    
   
    private JPanel resultsPanel;
    private CardLayout cardLayout;
    
    
    private DefaultTableModel[] resultTableModels;
    private String[] tableNames = {
        "Inicial", "Listos", "Despachados", "En Ejecución", 
        "Tiempo Expirado", "Bloqueados", "Despertar", "Finalizados"
    };
    private Filter[] filters = {
        Filter.INICIAL, Filter.LISTO, Filter.DESPACHADO, Filter.EN_EJECUCION,
        Filter.TIEMPO_EXPIRADO, Filter.BLOQUEADO, Filter.DESPERTAR, Filter.FINALIZADO
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
/* 
    setSize(1200, 700);          
    setLocationRelativeTo(null); 
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);*/

}

    private void initializeComponents() {
    
        setExtendedState(JFrame.MAXIMIZED_BOTH);

      
        txtProcessName = new JTextField(15);
        txtProcessTime = new JTextField(15);
        cmbStatus = new JComboBox<>(new String[]{"No Bloqueado", "Bloqueado"});

        setupTimeField();

        
        processTableModel = new DefaultTableModel(
            new String[]{"Nombre", "Tiempo", "Estado"}, 0) {
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
            resultTableModels[i] = new DefaultTableModel(
                new String[]{"Proceso", "Tiempo Restante", "Estado", "Ciclos"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
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
                // Solo permitir dígitos y teclas de control
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

    private void formatTimeField() {
        String text = txtProcessTime.getText().replaceAll("[^0-9]", ""); // Remover todo excepto números
        if (!text.isEmpty()) {
            try {
                long number = Long.parseLong(text);
                String formatted = numberFormatter.format(number);
                
                // Evitar bucle infinito
                if (!txtProcessTime.getText().equals(formatted)) {
                    int caretPos = txtProcessTime.getCaretPosition();
                    txtProcessTime.setText(formatted);
                    // Ajustar posición del cursor
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
                // Quitar el primer dígito y intentar de nuevo
                text = text.substring(1);
            }
        }        
        return Integer.parseInt(text);
    }    

    private void setupLayout() {
        setLayout(new BorderLayout());

      
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("SIMULADOR DE PROCESOS");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        // Panel izquierdoo
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        leftPanel.setPreferredSize(new Dimension(400, 0));

        
        JPanel formPanel = createFormPanel();
        leftPanel.add(formPanel, BorderLayout.NORTH);

      
        JScrollPane tableScrollPane = new JScrollPane(processTable);
        tableScrollPane.setPreferredSize(new Dimension(380, 300));
        leftPanel.add(tableScrollPane, BorderLayout.CENTER);

     
        JPanel actionPanel = createActionPanel();
        leftPanel.add(actionPanel, BorderLayout.SOUTH);

        // Panel derecho
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Resultados de la Simulación"));

        // Botones para cambiar entre tablas de resultados
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

      
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessName, gbc);

        
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Tiempo:"), gbc);
        gbc.gridx = 1;
        panel.add(txtProcessTime, gbc);

    
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        panel.add(cmbStatus, gbc);

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
    JButton btnExit = new JButton("Salir ");
    JButton btnManual = new JButton("Manual de usuario");


    
  //llamar los metodos
    btnAdd.addActionListener(e -> addProcess());
    btnEdit.addActionListener(e -> editProcess());
    btnDelete.addActionListener(e -> deleteProcess());
    btnSimulate.addActionListener(e -> runSimulation());
    btnExit.addActionListener(e -> System.exit(0));
    btnManual.addActionListener(e -> openUserManual());
    btnReset.addActionListener(e -> clearAll() );  
    
    
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
        JPanel panel = new JPanel(new FlowLayout());
        
        for (int i = 0; i < tableNames.length; i++) {
            JButton btn = new JButton(tableNames[i]);
            btn.setPreferredSize(new Dimension(130, 30));
            final int index = i;
            btn.addActionListener(e -> {
                cardLayout.show(resultsPanel, tableNames[index]);
                updateResultTable(index);
            });
            panel.add(btn);
        }

        return panel;
    }

    private void openUserManual() {
        try {
            
            File manualFile = new File("Manual_Usuario.pdf");
            
            if (!manualFile.exists()) {
                
                showError("No se encontró el archivo del manual de usuario.<br>" +
                         "Asegúrese de que el archivo 'Manual_Usuario.pdf'<br>" +
                         "este en la misma carpeta que el programa.");
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

    private void setupEventHandlers() {
      
    }

    private void addProcess() {
        String name = txtProcessName.getText().trim();
        String timeText = txtProcessTime.getText().trim();

        // Validaciones
        if (name.isEmpty()) {
            showError("El nombre del proceso no puede estar vacio");
            return;
        }

        if (processManager.processExists(name)) {
            showError("Ya existe un proceso con ese nombre");
            return;
        }

        try {
            int time = parseTimeField(); 
            if (time <= 0) {
                showError("El tiempo debe ser mayor a 0");
                return;
            }

            Status status = cmbStatus.getSelectedIndex() == 0 ? 
                Status.NO_BLOQUEADO : Status.BLOQUEADO;

            processManager.addProcess(name, time, status);
            updateProcessTable();
            clearForm();
            showInfo("Proceso agregado ");

        } catch (NumberFormatException ex) {
            showError("Ingrese un tiempo valido");
        }
    }

    private void editProcess() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Seleccione un proceso para modificar");
            return;
        }

        String oldName = (String) processTableModel.getValueAt(selectedRow, 0);
        
       
        JDialog editDialog = new JDialog(this, "Modificar Proceso", true);
        editDialog.setLayout(new GridBagLayout());
        editDialog.setSize(300, 200);
        editDialog.setLocationRelativeTo(this);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

     
        JTextField txtEditName = new JTextField(oldName, 15);
        txtEditName.setEditable(false);
        txtEditName.setBackground(Color.LIGHT_GRAY); 

        JTextField txtEditTime = new JTextField(15);
        // Obtener tiempo original sin formato
        String originalTimeStr = processTableModel.getValueAt(selectedRow, 1).toString().replaceAll("[^0-9]", "");
        txtEditTime.setText(originalTimeStr);
        
        // Configurar formato para el campo de tiempo en el diálogo
        setupEditTimeField(txtEditTime);

        JComboBox<String> cmbEditStatus = new JComboBox<>(new String[]{"No Bloqueado", "Bloqueado"});
        String currentStatus = (String) processTableModel.getValueAt(selectedRow, 2);
        cmbEditStatus.setSelectedIndex(currentStatus.equals("Bloqueado") ? 1 : 0);

        
        gbc.gridx = 0; gbc.gridy = 0;
        editDialog.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        editDialog.add(txtEditName, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        editDialog.add(new JLabel("Tiempo:"), gbc);
        gbc.gridx = 1;
        editDialog.add(txtEditTime, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        editDialog.add(new JLabel("Estado:"), gbc);
        gbc.gridx = 1;
        editDialog.add(cmbEditStatus, gbc);

       
        JPanel buttonPanel = new JPanel();
        JButton btnSave = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");


        btnSave.addActionListener(e -> {
            String newTimeText = txtEditTime.getText().trim();

            try {
                String cleanTime = newTimeText.replaceAll("[^0-9]", "");
                if (cleanTime.isEmpty()) {
                    showError("Ingrese un tiempo válido");
                    return;
                }
                while (cleanTime.length() > 1) {
                    try {
                        int newTime = Integer.parseInt(cleanTime);
                        if (newTime <= 0) {
                         showError("El tiempo debe ser mayor a 0");
                         return;
                    }

                    Status newStatus = cmbEditStatus.getSelectedIndex() == 0 ? 
                      Status.NO_BLOQUEADO : Status.BLOQUEADO;

                // Editar manteniendo la posición
                     processManager.editProcess(selectedRow, oldName, newTime, newStatus);
                     updateProcessTable();
                     editDialog.dispose();
                     showInfo("Proceso editado exitosamente");
                     return;
                    } catch (NumberFormatException ex) {
                            cleanTime = cleanTime.substring(1);
                    }
                }
                int newTime = Integer.parseInt(cleanTime);
                if (newTime <= 0) {
                        showError("El tiempo debe ser mayor a 0");
                        return;
                    }

            } catch (NumberFormatException ex) {
                showError("Ingrese un tiempo válido");
            }
        });

        btnCancel.addActionListener(e -> editDialog.dispose());

        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        editDialog.add(buttonPanel, gbc);

        editDialog.setVisible(true);
    }

    private void setupEditTimeField(JTextField timeField) {
        timeField.addKeyListener(new KeyListener() {
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
                String text = timeField.getText().replaceAll("[^0-9]", "");
                if (!text.isEmpty()) {
                    try {
                        long number = Long.parseLong(text);
                        String formatted = numberFormatter.format(number);
                        
                        if (!timeField.getText().equals(formatted)) {
                            int caretPos = timeField.getCaretPosition();
                            timeField.setText(formatted);
                            try {
                                timeField.setCaretPosition(Math.min(caretPos, formatted.length()));
                            } catch (IllegalArgumentException ex) {
                                timeField.setCaretPosition(formatted.length());
                            }
                        }
                    } catch (NumberFormatException ex) {
                        
                    }
                }
            }
        });
    }

    private void deleteProcess() {
        int selectedRow = processTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Seleccione un proceso para eliminar");
            return;
        }

        String processName = (String) processTableModel.getValueAt(selectedRow, 0);
        currentAction = "DELETE_PROCESS:" + processName;
        
        new CustomDialog(this, "Esta seguro de que desea eliminar el proceso '" + processName + "'?", CustomDialog.CONFIRM_TYPE);
    }

    private void runSimulation() {
        if (processManager.isEmpty()) {
            showError("No hay procesos para simular");
            return;
        }

        processManager.runSimulation();
        
        
        for (int i = 0; i < tableNames.length; i++) {
            updateResultTable(i);
        }
        
  
        cardLayout.show(resultsPanel, tableNames[1]); // Mostrar "Listos"
        
    }



    private void updateProcessTable() {
        processTableModel.setRowCount(0);
        for (model.Process p : processManager.getInitialProcesses()) {
             String formattedTime = numberFormatter.format(p.getOriginalTime());
            processTableModel.addRow(new Object[]{
                p.getName(),
                formattedTime,
                p.getStatusString()
            });
        }
    }

    private void updateResultTable(int tableIndex) {
        if (tableIndex == 0) {
            // Tabla inicial
            resultTableModels[0].setRowCount(0);
            for (model.Process p : processManager.getInitialProcesses()) {
                String formattedTime = numberFormatter.format(p.getOriginalTime());
                resultTableModels[0].addRow(new Object[]{
                    p.getName(), formattedTime, p.getStatusString(), 0
                });
            }
        } else {
            // Otras tablas y mostrar logs filtrados
            List<Log> logs = processManager.getLogsByFilter(filters[tableIndex]);
            resultTableModels[tableIndex].setRowCount(0);
            for (Log log : logs) {
                String formattedTime = numberFormatter.format(log.getRemainingTime());
                resultTableModels[tableIndex].addRow(new Object[]{
                    log.getProcessName(),
                    formattedTime,
                    log.getStatusString(),
                    log.getCycleCount()
                });
            }
        }
    }

    private void clearAll() {
        currentAction = "CLEAR_ALL";
        new CustomDialog(this, "¿Está seguro de que desea eliminar todos los procesos", CustomDialog.CONFIRM_TYPE);
    }

    private void clearForm() {
        txtProcessName.setText("");
        txtProcessTime.setText("");
        cmbStatus.setSelectedIndex(0);
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
                showInfo("Proceso eliminado");
            } else if (currentAction.equals("CLEAR_ALL")) {
                processManager.clearAll();
                updateProcessTable();
                
                
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