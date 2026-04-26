package ui.admin;

import util.ThemeManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import dao.StudentDAO;
import model.Student;
import model.SubjectFeeDTO;
import service.FeeService;

public class FeesManagementPanel extends JPanel {

    private static final Color ACCENT      = new Color(59, 130, 246);

    private JTable feesTable;
    private DefaultTableModel tableModel;
    private FeeService feeService;
    private StudentDAO studentDAO;
    private JComboBox<String> studentFilterCombo;
    private Map<String, String> studentIdMap;

    private JLabel totalFeeCard;
    private JLabel paidAmountCard;
    private JLabel pendingAmountCard;
    private JLabel statusCard;

    public FeesManagementPanel() {
        this.feeService = new FeeService();
        this.studentDAO = new StudentDAO();
        this.studentIdMap = new HashMap<>();
        
        setLayout(new BorderLayout(0, 20));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(32, 36, 32, 36));

        add(createHeader(), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);

        loadStudents();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.BG);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setBackground(ThemeManager.BG);
        JLabel title = new JLabel("Fees & Payments");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(ThemeManager.TEXT);
        JLabel sub = new JLabel("Manage student fee records and track payments");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(ThemeManager.SUB_TEXT);
        titlePanel.add(title);
        titlePanel.add(sub);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actionPanel.setBackground(ThemeManager.BG);
        
        studentFilterCombo = new JComboBox<>();
        studentFilterCombo.addItem("Select Student");
        studentFilterCombo.setPreferredSize(new Dimension(250, 40));
        studentFilterCombo.setBackground(ThemeManager.CARD);
        studentFilterCombo.setForeground(ThemeManager.TEXT);
        studentFilterCombo.addActionListener(e -> {
            if (studentFilterCombo.getSelectedIndex() > 0) {
                loadFeesForStudent();
            }
        });
        
        JButton refreshBtn = makeAccentButton("↻ Refresh", e -> loadFeesForStudent());
        refreshBtn.setPreferredSize(new Dimension(120, 40));

        JLabel studentLabel = new JLabel("Student:");
        studentLabel.setForeground(ThemeManager.TEXT);
        actionPanel.add(studentLabel);
        actionPanel.add(studentFilterCombo);
        actionPanel.add(refreshBtn);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(actionPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout(0, 24));
        content.setBackground(ThemeManager.BG);

        // Summary Cards
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        cardsPanel.setBackground(ThemeManager.BG);
        
        totalFeeCard = new JLabel("₹0.00", SwingConstants.CENTER);
        paidAmountCard = new JLabel("₹0.00", SwingConstants.CENTER);
        pendingAmountCard = new JLabel("₹0.00", SwingConstants.CENTER);
        statusCard = new JLabel("—", SwingConstants.CENTER);

        cardsPanel.add(createStatCard("Total Fees", totalFeeCard, ACCENT));
        cardsPanel.add(createStatCard("Paid", paidAmountCard, ThemeManager.SUCCESS));
        cardsPanel.add(createStatCard("Pending", pendingAmountCard, ThemeManager.WARNING));
        cardsPanel.add(createStatCard("Status", statusCard, ThemeManager.ERROR));

        // Table Panel
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(ThemeManager.CARD);
        tableCard.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true));

        String[] cols = {"Subject", "Fee", "Status", "Action"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3; }
        };
        feesTable = new JTable(tableModel);
        styleTable(feesTable);

        JScrollPane scroll = new JScrollPane(feesTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(ThemeManager.CARD);
        scroll.setBackground(ThemeManager.CARD);

        tableCard.add(scroll, BorderLayout.CENTER);

        content.add(cardsPanel, BorderLayout.NORTH);
        content.add(tableCard, BorderLayout.CENTER);
        return content;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(accent);
                g2.fillRect(0, 0, 4, getHeight());
                g2.dispose();
            }
        };
        card.setBackground(ThemeManager.CARD);
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        titleLbl.setForeground(ThemeManager.SUB_TEXT);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(ThemeManager.TEXT);
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(ThemeManager.DIVIDER);
        
        JTableHeader header = t.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setBackground(ThemeManager.isDarkMode ? ThemeManager.DARK_TABLE_HEADER : new Color(250, 251, 253));
        header.setForeground(ThemeManager.SUB_TEXT);
        header.setPreferredSize(new Dimension(0, 40));
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBackground(isSelected ? (ThemeManager.isDarkMode ? ThemeManager.DARK_HOVER : new Color(241, 245, 249)) : ThemeManager.TABLE_BG);
                setForeground(ThemeManager.TABLE_TEXT);
                
                if (column == 2) {
                    if ("PAID".equals(value)) {
                        setForeground(ThemeManager.SUCCESS);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(ThemeManager.ERROR);
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
                return c;
            }
        };
        for (int i = 0; i < 3; i++) t.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        
        t.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        t.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor(new JCheckBox()));
    }

    private void loadStudents() {
        // Disable listener temporarily
        java.awt.event.ActionListener[] listeners = studentFilterCombo.getActionListeners();
        for (java.awt.event.ActionListener l : listeners) studentFilterCombo.removeActionListener(l);

        List<Student> students = studentDAO.getAllStudents();
        studentFilterCombo.removeAllItems();
        studentFilterCombo.addItem("Select Student");
        studentIdMap.clear();
        for (Student s : students) {
            String name = s.getName() + " (" + s.getUserId() + ")";
            studentFilterCombo.addItem(name);
            studentIdMap.put(name, s.getUserId());
        }

        // Re-enable listener
        for (java.awt.event.ActionListener l : listeners) studentFilterCombo.addActionListener(l);
    }

    private void loadFeesForStudent() {
        tableModel.setRowCount(0);
        String selected = (String) studentFilterCombo.getSelectedItem();
        if (selected == null) return;
        String studentId = studentIdMap.get(selected);

        List<SubjectFeeDTO> fees = feeService.getStudentFeeDetails(studentId);
        Map<String, Object> summary = feeService.getFeeSummary(studentId);

        totalFeeCard.setText(String.format("₹%.2f", (Double)summary.get("totalFee")));
        paidAmountCard.setText(String.format("₹%.2f", (Double)summary.get("paidAmount")));
        pendingAmountCard.setText(String.format("₹%.2f", (Double)summary.get("pendingAmount")));
        
        String status = (String) summary.get("status");
        statusCard.setText(status);
        if ("PAID".equals(status)) {
            statusCard.setName("success");
            statusCard.setForeground(ThemeManager.SUCCESS);
        } else {
            statusCard.setName("error");
            statusCard.setForeground(ThemeManager.ERROR);
        }

        if (fees == null || fees.isEmpty()) {
            // No popup on load. Only if explicitly selected.
            System.out.println("[FeesPanel] No subjects found for " + studentId);
            return;
        }

        for (SubjectFeeDTO f : fees) {
            tableModel.addRow(new Object[]{f.getSubjectName(), String.format("₹%.2f", f.getMonthlyFee()), f.getPaymentStatus(), "Pay Now"});
        }
    }

    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            setBackground(ThemeManager.TABLE_BG);
            String status = (String) table.getValueAt(row, 2);
            if ("PAID".equals(status)) {
                setText("✔ Paid"); setForeground(ThemeManager.SUCCESS); setEnabled(false);
            } else {
                setText("Mark as Paid"); setForeground(ThemeManager.PRIMARY); setEnabled(true);
            }
            setFont(new Font("SansSerif", Font.BOLD, 12));
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String studentId;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.addActionListener(e -> {
                row = feesTable.getEditingRow();
                String studentName = (String) studentFilterCombo.getSelectedItem();
                studentId = studentIdMap.get(studentName);
                String subjectName = (String) tableModel.getValueAt(row, 0);
                String fee = (String) tableModel.getValueAt(row, 1);
                
                List<SubjectFeeDTO> fees = feeService.getStudentFeeDetails(studentId);
                SubjectFeeDTO f = fees.get(row);
                
                showPaymentDialog(studentId, f.getSubjectId(), subjectName, fee);
                fireEditingStopped();
            });
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int r, int c) {
            String status = (String) table.getValueAt(r, 2);
            button.setText("PAID".equals(status) ? "✔ Paid" : "Mark as Paid");
            button.setForeground("PAID".equals(status) ? ThemeManager.SUCCESS : ThemeManager.PRIMARY);
            return button;
        }
        @Override public Object getCellEditorValue() { return button.getText(); }
    }

    private void showPaymentDialog(String sId, String subId, String subName, String fee) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Payment Confirmation", true);
        d.setSize(400, 350);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout());

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(24, 24, 24, 24));
        p.setBackground(ThemeManager.CARD);

        JLabel title = new JLabel("Confirm Payment");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(ThemeManager.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subjectLabel = new JLabel("Subject: " + subName);
        subjectLabel.setForeground(ThemeManager.TEXT);
        JLabel amountLabel = new JLabel("Amount: " + fee);
        amountLabel.setForeground(ThemeManager.TEXT);
        JLabel modeLabel = new JLabel("Payment Mode:");
        modeLabel.setForeground(ThemeManager.TEXT);
        
        p.add(title); p.add(Box.createVerticalStrut(20));
        p.add(subjectLabel); p.add(Box.createVerticalStrut(10));
        p.add(amountLabel); p.add(Box.createVerticalStrut(20));
        
        p.add(modeLabel);
        JComboBox<String> modes = new JComboBox<>(new String[]{"Cash", "UPI", "Card"});
        modes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        p.add(modes); p.add(Box.createVerticalStrut(30));

        JButton confirm = makeAccentButton("Confirm Payment", e -> {
            if (feeService.recordPayment(sId, subId, (String)modes.getSelectedItem())) {
                JOptionPane.showMessageDialog(d, "Payment recorded successfully!");
                d.dispose();
                loadFeesForStudent();
            } else {
                JOptionPane.showMessageDialog(d, "Failed to record payment.");
            }
        });
        confirm.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        p.add(confirm);

        d.add(p);
        d.setVisible(true);
    }

    private JButton makeAccentButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = ThemeManager.PRIMARY;
                g2.setColor(getModel().isRollover() ? base.brighter() : base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g2); g2.dispose();
            }
        };
        btn.setName("primary");
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (al != null) btn.addActionListener(al);
        return btn;
    }
}
