package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

import model.SubjectFeeDTO;
import service.FeeService;
import util.SessionManager;

/**
 * FeesPanel - Student view for fees and payments
 * Displays subject-wise fee details and payment status
 * No payment button (students can only view)
 */
public class FeesPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JPanel statsPanel;
    private JTable feeTable;
    private FeeService feeService;
    
    private static final Color PAID_COLOR = new Color(76, 175, 80);
    private static final Color UNPAID_COLOR = new Color(244, 67, 54);
    private static final Color TABLE_HEADER_BG = new Color(41, 57, 86);

    public FeesPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        feeService = new FeeService();

        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Stats Cards
        JPanel statsAndTablePanel = new JPanel(new BorderLayout());
        statsAndTablePanel.setBackground(Color.WHITE);
        
        statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(new EmptyBorder(0, 0, 30, 0));
        statsAndTablePanel.add(statsPanel, BorderLayout.NORTH);

        // Table for subject fees
        JPanel tablePanel = createTablePanel();
        statsAndTablePanel.add(tablePanel, BorderLayout.CENTER);

        add(statsAndTablePanel, BorderLayout.CENTER);

        loadDataAsync();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("Fees & Payments");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setForeground(new Color(26, 35, 64));

        JLabel subtitle = new JLabel("View your subject-wise fees and payment status");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitle.setForeground(new Color(107, 122, 153));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setBackground(Color.WHITE);
        textPanel.add(title);
        textPanel.add(subtitle);

        panel.add(textPanel, BorderLayout.WEST);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 240), 1, true));

        // Create table model with subject-wise fee details
        String[] columns = {"Subject Name", "Monthly Fee", "Payment Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only for students
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
        feeTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        feeTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        feeTable.getColumnModel().getColumn(2).setPreferredWidth(150);

        // Set header styling
        feeTable.getTableHeader().setBackground(TABLE_HEADER_BG);
        feeTable.getTableHeader().setForeground(Color.WHITE);
        feeTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        feeTable.getTableHeader().setPreferredSize(new Dimension(0, 40));

        // Add custom renderer for status column
        feeTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(feeTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createModernStatCard(String title, String value, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel contentPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        contentPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(107, 122, 153));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 20));
        valueLabel.setForeground(new Color(26, 35, 64));

        contentPanel.add(titleLabel);
        contentPanel.add(valueLabel);

        card.add(contentPanel, BorderLayout.CENTER);
        return card;
    }

    private void loadDataAsync() {
        statsPanel.removeAll();
        statsPanel.add(createModernStatCard("Total Fees", "Loading...", new Color(100, 150, 255)));
        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"Loading...", "", ""});

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String userId = SessionManager.getInstance().getUserId();
                if (userId == null || userId.isEmpty()) {
                    System.err.println("[FeesPanel] No user ID from SessionManager");
                    return null;
                }

                System.out.println("[FeesPanel] Loading fees for student: " + userId);

                // Get all data in background
                List<SubjectFeeDTO> feeDetails = feeService.getStudentFeeDetails(userId);
                double totalFee = feeService.calculateTotalFee(userId);
                double paidAmount = feeService.calculatePaidAmount(userId);
                double pendingAmount = feeService.calculatePendingAmount(userId);
                String overallStatus = feeService.getOverallPaymentStatus(userId);

                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    updateUI(feeDetails, totalFee, paidAmount, pendingAmount, overallStatus);
                });

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    System.err.println("[FeesPanel] Error loading data: " + e.getMessage());
                    e.printStackTrace();
                    statsPanel.removeAll();
                    statsPanel.add(createModernStatCard("Error", "Failed to load", new Color(255, 100, 100)));
                    statsPanel.revalidate();
                    statsPanel.repaint();
                }
            }
        }.execute();
    }

    private void updateUI(List<SubjectFeeDTO> feeDetails, double totalFee, 
                         double paidAmount, double pendingAmount, String overallStatus) {
        // Update stat cards
        statsPanel.removeAll();

        statsPanel.add(createModernStatCard("Total Fees", String.format("Rs. %.2f", totalFee), new Color(100, 150, 255)));
        statsPanel.add(createModernStatCard("Amount Paid", String.format("Rs. %.2f", paidAmount), new Color(76, 175, 80)));
        statsPanel.add(createModernStatCard("Pending", String.format("Rs. %.2f", pendingAmount), new Color(244, 67, 54)));
        
        String statusDisplay = overallStatus;
        Color statusColor = new Color(180, 100, 255);
        if ("PAID".equalsIgnoreCase(overallStatus)) {
            statusColor = PAID_COLOR;
        } else if ("UNPAID".equalsIgnoreCase(overallStatus)) {
            statusColor = UNPAID_COLOR;
        }
        statsPanel.add(createModernStatCard("Status", statusDisplay, statusColor));
        
        statsPanel.revalidate();
        statsPanel.repaint();

        // Update fee table
        tableModel.setRowCount(0);

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

        feeTable.revalidate();
        feeTable.repaint();
    }

    /**
     * Custom cell renderer for payment status column
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
}

