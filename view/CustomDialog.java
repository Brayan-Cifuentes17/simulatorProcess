package view;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CustomDialog extends JDialog{
    
    public static final int WARNING_TYPE = 1;
    public static final int INFO_TYPE = 2;
    public static final int CONFIRM_TYPE = 3;
    
    public CustomDialog(ActionListener listener, String text, int type){
        this.setSize(new Dimension(350, 100+(10*quantityOfWords(text, "<br>"))));
        if(type == CONFIRM_TYPE) {
            this.setSize(new Dimension(350, 120+(10*quantityOfWords(text, "<br>"))));
        }
        this.setLocationRelativeTo(null);
        this.setLayout(new GridBagLayout());
        this.setUndecorated(true);
        this.setAlwaysOnTop(true);
        this.setModal(true);
        initComponents(listener, text, type);
        this.setVisible(true);
    }
    
    public static int quantityOfWords(String text, String word) {
        String textCopy = ""+text;
        String[] words = textCopy.split("\\s+");
        int counter = 0;
        for (String p : words) {
            if (p.equalsIgnoreCase(word)) {
                counter++;
            }
        }
        return counter;
    }
    
    public void initComponents(ActionListener listener, String text, int type){
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(this.getSize());
        panel.setBackground(Color.lightGray);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 0, 0);
        JLabel label = new JLabel(text);
        panel.add(label, gbc);
        gbc.gridy = 1;
        
        if(type == CONFIRM_TYPE) {
            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.setBackground(Color.lightGray);
            
            JButton yesButton = new JButton("Si");
            yesButton.addActionListener(listener);
            yesButton.setActionCommand(Constants.CONFIRM_YES);
            
            JButton noButton = new JButton("No");
            noButton.addActionListener(listener);
            noButton.setActionCommand(Constants.CONFIRM_NO);
            
            buttonPanel.add(yesButton);
            buttonPanel.add(noButton);
            panel.add(buttonPanel, gbc);
        } else {
            JButton button = new JButton("Aceptar");
            button.addActionListener(listener);
            if(type == WARNING_TYPE) {
                button.setActionCommand(Constants.CLOSE_WARNING);
            } else {
                button.setActionCommand(Constants.CLOSE_INFO);
            }
            panel.add(button, gbc);
        }
        
        this.add(panel);
    }
}