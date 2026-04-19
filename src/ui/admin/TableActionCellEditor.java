package ui.admin;

import javax.swing.*;
import java.awt.*;

public class TableActionCellEditor extends DefaultCellEditor {
    private TableActionEvent event;

    public TableActionCellEditor(TableActionEvent event) {
        super(new JCheckBox());
        this.event = event;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actionPanel.setBackground(table.getSelectionBackground());
        
        JButton editBtn = new JButton("✎");
        editBtn.setBorderPainted(false);
        editBtn.setContentAreaFilled(false);
        editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton delBtn = new JButton("🗑");
        delBtn.setBorderPainted(false);
        delBtn.setContentAreaFilled(false);
        delBtn.setForeground(new Color(220, 50, 50));
        delBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        editBtn.addActionListener(e -> {
            fireEditingStopped();
            event.onEdit(row);
        });
        
        delBtn.addActionListener(e -> {
            fireEditingStopped();
            event.onDelete(row);
        });
        
        actionPanel.add(editBtn);
        actionPanel.add(delBtn);
        return actionPanel;
    }
}
