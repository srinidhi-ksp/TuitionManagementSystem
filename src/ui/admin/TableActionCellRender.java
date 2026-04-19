package ui.admin;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class TableActionCellRender extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component com = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actionPanel.setBackground(com.getBackground());
        
        JButton editBtn = new JButton("✎");
        editBtn.setBorderPainted(false);
        editBtn.setContentAreaFilled(false);
        editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton delBtn = new JButton("🗑");
        delBtn.setBorderPainted(false);
        delBtn.setContentAreaFilled(false);
        delBtn.setForeground(new Color(220, 50, 50));
        delBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        actionPanel.add(editBtn);
        actionPanel.add(delBtn);
        return actionPanel;
    }
}
