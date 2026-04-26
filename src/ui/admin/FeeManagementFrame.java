package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import model.SubjectFeeDTO;
import model.Student;
import service.FeeService;
import dao.StudentDAO;

/**
 * Fee Management Frame for Admin Dashboard
 * Allows admin to view and manage subject-wise fees
 * Features: List students with their subject fees, mark subjects as paid
 */
public class FeeManagementFrame extends JPanel {

    private DefaultTableModel tableModel;
    private JTable feeTable;
    private FeeService feeService;
    private StudentDAO studentDAO;
    
    private static final Color ACCENT_COLOR = new Color(74, 144, 226);
    private static final Color BG_COLOR = new Color(244, 247, 249);
    private static final Color TABLE_HEADER_BG = new Color(41, 57, 86);
    private static final Color PAID_COLOR = new Color(76, 175, 80);
    private static final Color UNPAID_COLOR = new Color(244, 67, 54);

    public FeeManagementFrame() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        feeService = new FeeService();
        studentDAO = new StudentDAO();

        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Table
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Fee Management & Payments");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(26, 35, 64));

        JLabel subtitleLabel = new JLabel("Manage and record subject-wise fee payments");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(107, 122, 153));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setBackground(BG_COLOR);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setBackground(BG_COLOR);

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        refreshBtn.setBackground(ACCENT_COLOR);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR, 1, true),
            new EmptyBorder(8, 16, 8, 16)
        ));
        refreshBtn.addActionListener(e -> loadData());

        actionPanel.add(refreshBtn);

        panel.add(textPanel, BorderLayout.WEST);
        panel.add(actionPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 240), 1, true));

        // Create table model
        String[] columns = {"Student ID", "Student Name", "Subject", "Monthly Fee", "Status", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only action column is editable
            }
        };

        feeTable = new JTable(tableModel);
        feeTable.setRowHeight(45);
        feeTable.setFont(new Font("Arial", Font.PLAIN, 12));
        feeTable.setGridColor(new Color(230, 230, 230));
        feeTable.setShowGrid(true);
        feeTable.setShowHorizontalLines(true);
        feeTable.setShowVerticalLines(false);
        feeTable.setIntercellSpacing(new Dimension(1, 1));

        // Set column widths
        feeTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        feeTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        feeTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        feeTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        feeTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        feeTable.getColumnModel().getColumn(5).setPreferredWidth(100);

        // Set header styling
        feeTable.getTableHeader().setBackground(TABLE_HEADER_BG);
        feeTable.getTableHeader().setForeground(Color.WHITE);
        feeTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        feeTable.getTableHeader().setPreferredSize(new Dimension(0, 40));

        // Add custom renderer for status column
        feeTable.getColumnModel().getColumn(4).setCellRenderer(new StatusCellRenderer());

        // Add custom renderer and editor for action column
        feeTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        feeTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox(), this));

        JScrollPane scrollPane = new JScrollPane(feeTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        panel.add(scrollPane, BorderLayout.CENTER);

        // Load data
        loadData();

        return panel;
    }

    private void loadData() {
        tableModel.setRowCount(0); // Clear existing rows

        try {
            // Get all students
            List<Student> students = studentDAO.getAllStudents();

            if (students == null || students.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No students found.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // For each student, get their fee details
            for (Student student : students) {
                List<SubjectFeeDTO> feeDetails = feeService.getStudentFeeDetails(student.getUserId());

                if (!feeDetails.isEmpty()) {
                    for (SubjectFeeDTO fee : feeDetails) {
                        String[] row = {
                            student.getUserId(),
                            student.getName(),
                            fee.getSubjectName(),
                            String.format("Rs. %.2f", fee.getMonthlyFee()),
                            fee.getPaymentStatus(),
                            "Mark as Paid"
                        };
                        tableModel.addRow(row);
                    }
                } else {
                    // Show student with no enrollments
                    String[] row = {
                        student.getUserId(),
                        student.getName(),
                        "No subjects enrolled",
                        "0.00",
                        "UNPAID",
                        "-"
                    };
                    tableModel.addRow(row);
                }
            }

        } catch (Exception e) {
            System.err.println("[FeeManagementFrame] Error loading data: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Handle mark as paid action
     */
    public void handleMarkAsPaid(int row) {
        try {
            if (row < 0 || row >= tableModel.getRowCount()) return;

            String studentId = (String) tableModel.getValueAt(row, 0);
            String studentName = (String) tableModel.getValueAt(row, 1);
            String subjectName = (String) tableModel.getValueAt(row, 2);
            String feeStr = (String) tableModel.getValueAt(row, 3);
            String status = (String) tableModel.getValueAt(row, 4);

            // Extract fee amount
            double fee = 0;
            if (feeStr != null && !feeStr.isEmpty()) {
                fee = Double.parseDouble(feeStr.replace("Rs. ", "").trim());
            }

            // Check if already paid
            if ("PAID".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(this, "Subject is already marked as paid.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Open payment dialog
            PaymentDialog dialog = new PaymentDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                studentId,
                studentName,
                subjectName,
                fee,
                this::loadData // Callback to refresh table
            );
            dialog.setVisible(true);

        } catch (Exception e) {
            System.err.println("[FeeManagementFrame] Error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Custom Cell Renderers and Editors ──

    /**
     * Renderer for Status column with color coding
     */
    private class StatusCellRenderer extends JLabel implements TableCellRenderer {
        public StatusCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Arial", Font.BOLD, 11));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                      boolean hasFocus, int row, int column) {
            String status = value != null ? value.toString() : "UNPAID";

            if ("PAID".equalsIgnoreCase(status)) {
                setBackground(PAID_COLOR);
                setForeground(Color.WHITE);
                setText("✓ PAID");
            } else if ("UNPAID".equalsIgnoreCase(status)) {
                setBackground(UNPAID_COLOR);
                setForeground(Color.WHITE);
                setText("✗ UNPAID");
            } else {
                setBackground(new Color(200, 200, 200));
                setForeground(Color.WHITE);
                setText(status);
            }

            return this;
        }
    }

    /**
     * Renderer for Action button column
     */
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Arial", Font.PLAIN, 11));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                      boolean hasFocus, int row, int column) {
            String status = (String) table.getValueAt(row, 4);

            if ("PAID".equalsIgnoreCase(status)) {
                setText("Paid");
                setBackground(new Color(200, 200, 200));
                setForeground(Color.GRAY);
                setEnabled(false);
            } else {
                setText("Mark as Paid");
                setBackground(ACCENT_COLOR);
                setForeground(Color.WHITE);
                setEnabled(true);
            }

            return this;
        }
    }

    /**
     * Editor for Action button column
     */
    private class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int selectedRow;
        private FeeManagementFrame parent;

        public ButtonEditor(JCheckBox checkBox, FeeManagementFrame parent) {
            super(checkBox);
            this.parent = parent;
            button = new JButton();
            button.setOpaque(true);
            button.setFont(new Font("Arial", Font.PLAIN, 11));
            button.setBackground(ACCENT_COLOR);
            button.setForeground(Color.WHITE);
            button.addActionListener(e -> {
                String status = (String) tableModel.getValueAt(selectedRow, 4);
                if (!"PAID".equalsIgnoreCase(status)) {
                    parent.handleMarkAsPaid(selectedRow);
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                    int row, int column) {
            selectedRow = row;
            String status = (String) table.getValueAt(row, 4);

            if ("PAID".equalsIgnoreCase(status)) {
                button.setText("Paid");
                button.setBackground(new Color(200, 200, 200));
                button.setForeground(Color.GRAY);
                button.setEnabled(false);
            } else {
                button.setText("Mark as Paid");
                button.setBackground(ACCENT_COLOR);
                button.setForeground(Color.WHITE);
                button.setEnabled(true);
            }

            return button;
        }
    }
}
