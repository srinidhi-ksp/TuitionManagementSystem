package ui.student;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import model.SubjectFeeDTO;
import model.User;
import service.FeeService;

/**
 * Enhanced Fees Panel for Student Dashboard
 * Displays student's subject-wise fees and payment status
 * Student can only VIEW, not make payments
 */
public class EnhancedFeesPanel extends JPanel {

    private FeeService feeService;
    private JLabel totalFeeLabel;
    private JLabel paidAmountLabel;
    private JLabel pendingAmountLabel;
    private JLabel statusLabel;
    private JTable feesTable;
    private DefaultTableModel tableModel;
    private User currentUser;

    public EnhancedFeesPanel(User user) {
        this.currentUser = user;
        this.feeService = new FeeService();
        
        initializeUI();
        loadFeeData();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(new Color(244, 247, 249));

        // ====== TOP: Summary Cards ======
        JPanel summaryPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        summaryPanel.setOpaque(false);
        
        totalFeeLabel = createSummaryCard("Total Fees", "Rs. 0.00", new Color(33, 150, 243));
        paidAmountLabel = createSummaryCard("Paid Amount", "Rs. 0.00", new Color(76, 175, 80));
        pendingAmountLabel = createSummaryCard("Pending Amount", "Rs. 0.00", new Color(244, 67, 54));
        statusLabel = createSummaryCard("Payment Status", "UNPAID", new Color(255, 152, 0));
        
        summaryPanel.add(totalFeeLabel);
        summaryPanel.add(paidAmountLabel);
        summaryPanel.add(pendingAmountLabel);
        summaryPanel.add(statusLabel);
        
        add(summaryPanel, BorderLayout.NORTH);

        // ====== CENTER: Fees Table ======
        String[] columns = {"Subject", "Monthly Fee (Rs.)", "Payment Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;  // Read-only for students
            }
        };

        feesTable = new JTable(tableModel);
        feesTable.setRowHeight(28);
        feesTable.setForeground(new Color(26, 35, 64));
        feesTable.getTableHeader().setBackground(new Color(74, 144, 226));
        feesTable.getTableHeader().setForeground(Color.WHITE);
        feesTable.setDefaultRenderer(Object.class, new FeesTableCellRenderer());

        // Set column widths
        feesTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        feesTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        feesTable.getColumnModel().getColumn(2).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(feesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 220, 235), 1));
        add(scrollPane, BorderLayout.CENTER);
    }

    private JLabel createSummaryCard(String title, String value, Color bgColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        titleLabel.setForeground(new Color(107, 122, 153));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        valueLabel.setForeground(bgColor);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(valueLabel);

        // Wrap in a container to maintain layout
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.CENTER);
        return new JLabel();  // We'll update this later
    }

    private void loadFeeData() {
        try {
            if (currentUser == null || currentUser.getUserId() == null) {
                System.err.println("[EnhancedFeesPanel] User ID is null!");
                return;
            }

            String studentId = currentUser.getUserId();
            System.out.println("[EnhancedFeesPanel] Loading fees for student: " + studentId);

            // Get fee details
            List<SubjectFeeDTO> feeDetails = feeService.getStudentFeeDetails(studentId);
            
            // Get summary
            Map<String, Object> summary = feeService.getFeeSummary(studentId);

            // Update summary cards
            double totalFee = (double) summary.get("totalFee");
            double paidAmount = (double) summary.get("paidAmount");
            double pendingAmount = (double) summary.get("pendingAmount");
            String status = (String) summary.get("status");

            totalFeeLabel.setText(String.format("Total Fees\nRs. %.2f", totalFee));
            paidAmountLabel.setText(String.format("Paid Amount\nRs. %.2f", paidAmount));
            pendingAmountLabel.setText(String.format("Pending Amount\nRs. %.2f", pendingAmount));
            statusLabel.setText("Payment Status\n" + status);

            // Clear existing table rows
            tableModel.setRowCount(0);

            // Populate table
            if (feeDetails != null && !feeDetails.isEmpty()) {
                for (SubjectFeeDTO fee : feeDetails) {
                    Object[] row = {
                        fee.getSubjectName(),
                        String.format("Rs. %.2f", fee.getMonthlyFee()),
                        fee.getPaymentStatus()
                    };
                    tableModel.addRow(row);
                }
            } else {
                Object[] row = {"No subjects enrolled", "", ""};
                tableModel.addRow(row);
            }

        } catch (Exception e) {
            System.err.println("[EnhancedFeesPanel] Error loading fee data: " + e.getMessage());
            e.printStackTrace();
            Object[] row = {"Error loading data", "", ""};
            tableModel.setRowCount(0);
            tableModel.addRow(row);
        }
    }

    // ====== Custom Cell Renderer ======
    private class FeesTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                      boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(244, 247, 249));
            }

            // Color code the status
            if (column == 2 && value != null) {
                String status = value.toString();
                if ("PAID".equals(status)) {
                    c.setForeground(new Color(76, 175, 80));
                    ((JLabel) c).setFont(new Font("SansSerif", Font.BOLD, 12));
                } else if ("UNPAID".equals(status)) {
                    c.setForeground(new Color(244, 67, 54));
                    ((JLabel) c).setFont(new Font("SansSerif", Font.BOLD, 12));
                }
            }

            return c;
        }
    }

    /**
     * Refresh fees data
     */
    public void refreshData() {
        loadFeeData();
    }
}
