package ui.student;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTabbedPane;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import model.SubjectFeeDTO;
import service.FeeService;
import util.SessionManager;

public class FeesPanel extends JPanel {

    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color SUCCESS_GREEN = new Color(52, 211, 153);
    private static final Color WARNING_ORANGE = new Color(251, 146, 60);
    private static final Color ERROR_RED   = new Color(248, 113, 113);

    private JTable feesTable;
    private DefaultTableModel tableModel;
    private JTable historyTable;
    private DefaultTableModel historyModel;
    private FeeService feeService;

    private JLabel totalFeeCard;
    private JLabel paidAmountCard;
    private JLabel pendingAmountCard;
    private JLabel statusCard;

    public FeesPanel() {
        this.feeService = new FeeService();
        
        setLayout(new BorderLayout(0, 24));
        setBackground(PAGE_BG);
        setBorder(new EmptyBorder(32, 36, 32, 36));

        add(createHeader(), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);

        loadData();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PAGE_BG);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setBackground(PAGE_BG);
        JLabel title = new JLabel("My Fees & Payments");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(TEXT_PRI);
        JLabel sub = new JLabel("View your fee breakdown and payment status");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(TEXT_SEC);
        titlePanel.add(title);
        titlePanel.add(sub);

        JButton refreshBtn = new JButton("↻ Refresh");
        refreshBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadData());

        header.add(titlePanel, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        return header;
    }

    private JPanel createContent() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));

        // 1. MAKE PAYMENT PANEL
        JPanel paymentPanel = new JPanel(new BorderLayout(0, 20));
        paymentPanel.setBackground(PAGE_BG);
        paymentPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        // Top Summary Cards
        JPanel topCardsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        topCardsPanel.setBackground(PAGE_BG);
        
        totalFeeCard = new JLabel("₹0.00", SwingConstants.CENTER);
        paidAmountCard = new JLabel("₹0.00", SwingConstants.CENTER);
        pendingAmountCard = new JLabel("₹0.00", SwingConstants.CENTER);
        statusCard = new JLabel("-", SwingConstants.CENTER);

        topCardsPanel.add(createStatCard("Total Fees", totalFeeCard, ACCENT));
        topCardsPanel.add(createStatCard("Amount Paid", paidAmountCard, SUCCESS_GREEN));
        topCardsPanel.add(createStatCard("Pending Balance", pendingAmountCard, WARNING_ORANGE));
        topCardsPanel.add(createStatCard("Overall Status", statusCard, ERROR_RED));

        // Payment Gateway (The Table and Action buttons)
        JPanel paymentGatewayPanel = new JPanel(new BorderLayout());
        paymentGatewayPanel.setBackground(CARD_BG);
        paymentGatewayPanel.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 245), 1, true));

        String[] cols = {"Subject Name", "Monthly Fee", "Payment Status", "Action", "subjectId"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3; }
        };
        feesTable = new JTable(tableModel);
        styleTable(feesTable);
        feesTable.getColumnModel().getColumn(4).setMinWidth(0);
        feesTable.getColumnModel().getColumn(4).setMaxWidth(0);

        JScrollPane scroll = new JScrollPane(feesTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);
        paymentGatewayPanel.add(scroll, BorderLayout.CENTER);

        paymentPanel.add(topCardsPanel, BorderLayout.NORTH);
        paymentPanel.add(paymentGatewayPanel, BorderLayout.CENTER);

        // 2. PAYMENT HISTORY PANEL
        JPanel historyPanel = new JPanel(new BorderLayout(0, 20));
        historyPanel.setBackground(PAGE_BG);
        historyPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        String[] hCols = {"Date", "Subject", "Batch", "Amount", "Method", "Status"};
        historyModel = new DefaultTableModel(hCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        styleHistoryTable(historyTable);

        JScrollPane hScroll = new JScrollPane(historyTable);
        hScroll.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 245)));
        hScroll.getViewport().setBackground(CARD_BG);

        JButton downloadBtn = new JButton("Download Receipt");
        downloadBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        downloadBtn.setPreferredSize(new Dimension(180, 45));
        downloadBtn.setBackground(ACCENT);
        downloadBtn.setForeground(Color.WHITE);

        historyPanel.add(hScroll, BorderLayout.CENTER);
        historyPanel.add(downloadBtn, BorderLayout.SOUTH);

        // ADD TABS
        tabs.addTab("Make Payment", paymentPanel);
        tabs.addTab("Payment History", historyPanel);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(tabs, BorderLayout.CENTER);
        return mainPanel;
    }

    private void styleHistoryTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t.setRowHeight(45);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(240, 242, 245));
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(250, 251, 253));
        t.getTableHeader().setPreferredSize(new Dimension(0, 40));
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(accent);
                g2.fillRect(0, 0, 4, getHeight());
                g2.dispose();
            }
        };
        card.setBackground(CARD_BG);
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        titleLbl.setForeground(TEXT_SEC);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(TEXT_PRI);
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t.setRowHeight(50);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(240, 242, 245));
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBackground(new Color(250, 251, 253));
        t.getTableHeader().setForeground(TEXT_SEC);
        t.getTableHeader().setPreferredSize(new Dimension(0, 40));
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 2) {
                    String status = String.valueOf(value).toUpperCase();
                    if ("PAID".equals(status)) {
                        setForeground(SUCCESS_GREEN);
                    } else if ("PARTIAL".equals(status) || "PENDING".equals(status)) {
                        setForeground(WARNING_ORANGE);
                    } else {
                        setForeground(ERROR_RED);
                    }
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    setForeground(TEXT_PRI);
                }
                setBackground(isSelected ? new Color(245, 247, 255) : CARD_BG);
                setBorder(new EmptyBorder(0, 20, 0, 20));
                return c;
            }
        };
        
        for (int i = 0; i < 3; i++) t.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        
        t.getColumnModel().getColumn(3).setCellRenderer(new ButtonRenderer());
        t.getColumnModel().getColumn(3).setCellEditor(new ButtonEditor());
    }

    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setFont(new Font("SansSerif", Font.BOLD, 12));
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            String status = (String) table.getValueAt(row, 2);
            if ("PAID".equalsIgnoreCase(status)) {
                setText("Generate Receipt");
                setForeground(ACCENT);
                setEnabled(true);
            } else {
                setText("-");
                setForeground(TEXT_SEC);
                setEnabled(false);
            }
            return this;
        }
    }

    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private JButton button;
        private int row;

        public ButtonEditor() {
            button = new JButton();
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.addActionListener(e -> {
                row = feesTable.getEditingRow();
                if (row == -1) return;
                
                String batchIdStr = (String) tableModel.getValueAt(row, 4);
                int batchId = Integer.parseInt(batchIdStr);
                String studentId = SessionManager.getInstance().getUserId();
                String status = (String) tableModel.getValueAt(row, 2);
                String subjectName = (String) tableModel.getValueAt(row, 0);
                String feeStr = (String) tableModel.getValueAt(row, 1);
                
                double amount = 0;
                try {
                    amount = Double.parseDouble(feeStr.replaceAll("[^0-9.]", ""));
                } catch(Exception ex) {}

                if ("PAID".equalsIgnoreCase(status)) {
                    model.Receipt receipt = feeService.generateReceipt(studentId, batchId);
                    if (receipt != null) {
                        new ReceiptDialog((Frame) SwingUtilities.getWindowAncestor(FeesPanel.this), receipt).setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(FeesPanel.this, "Error generating receipt.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    fireEditingStopped();
                    return;
                }
                
                if ("PENDING".equalsIgnoreCase(status)) {
                    JOptionPane.showMessageDialog(FeesPanel.this, "Your payment is pending admin approval.\nPlease contact the tuition center.", "Status", JOptionPane.INFORMATION_MESSAGE);
                    fireEditingStopped();
                    return;
                }

                // For UNPAID or PARTIAL
                new PaymentDialog((Frame) SwingUtilities.getWindowAncestor(FeesPanel.this), 
                                 studentId, batchId, amount, subjectName, () -> loadData()).setVisible(true);
                
                fireEditingStopped();
            });
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int r, int c) {
            String status = (String) table.getValueAt(r, 2);
            if ("PAID".equalsIgnoreCase(status)) {
                button.setText("Generate Receipt");
                button.setForeground(ACCENT);
                button.setEnabled(true);
            } else {
                button.setText("-");
                button.setForeground(TEXT_SEC);
                button.setEnabled(false);
            }
            return button;
        }
        @Override public Object getCellEditorValue() { return button.getText(); }
    }

    private void loadData() {
        String userIdFromSession = SessionManager.getInstance().getUserId();
        if (userIdFromSession == null) {
            System.err.println("[FeesPanel] ❌ User ID is null in session!");
            return;
        }

        System.out.println("[FeesPanel] 🔄 Starting loadData...");
        System.out.println("[FeesPanel] User ID from session: " + userIdFromSession);

        new javax.swing.SwingWorker<Void, Void>() {
            List<SubjectFeeDTO> fees;
            Map<String, Object> summary;
            List<Map<String, Object>> history;

            @Override
            protected Void doInBackground() throws Exception {
                fees = feeService.getStudentFeeDetails(userIdFromSession);
                summary = feeService.getFeeSummary(userIdFromSession);
                history = feeService.getPaymentHistory(userIdFromSession);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    tableModel.setRowCount(0);
                    historyModel.setRowCount(0);

                    // Update summary cards
                    if (summary != null) {
                        totalFeeCard.setText(String.format("₹%.2f", (Double)summary.get("totalFee")));
                        paidAmountCard.setText(String.format("₹%.2f", (Double)summary.get("paidAmount")));
                        pendingAmountCard.setText(String.format("₹%.2f", (Double)summary.get("pendingAmount")));
                        
                        String status = (String) summary.get("status");
                        statusCard.setText(status != null ? status : "-");
                        
                        if ("PAID".equals(status)) statusCard.setForeground(SUCCESS_GREEN);
                        else if ("PARTIAL".equals(status) || "PENDING".equals(status)) statusCard.setForeground(WARNING_ORANGE);
                        else statusCard.setForeground(ERROR_RED);
                    }

                    // Populate current fees table
                    if (fees != null && !fees.isEmpty()) {
                        for (SubjectFeeDTO f : fees) {
                            tableModel.addRow(new Object[]{
                                f.getSubjectName(), 
                                String.format("₹%.2f", f.getMonthlyFee()), 
                                f.getPaymentStatus(),
                                "Generate Receipt",
                                String.valueOf(f.getBatchId())
                            });
                        }
                    } else {
                        String statusMsg = summary != null ? (String) summary.get("status") : "UNKNOWN";
                        if ("NO_ENROLLMENT".equals(statusMsg)) {
                            tableModel.addRow(new Object[]{"No active enrollments found", "", "", "", ""});
                        } else {
                            tableModel.addRow(new Object[]{"Error: Unable to load fee data", "", "", "", ""});
                        }
                    }

                    // Populate history table
                    if (history != null) {
                        for (Map<String, Object> row : history) {
                            historyModel.addRow(new Object[]{
                                row.get("date"),
                                row.get("subject"),
                                row.get("batch"),
                                String.format("₹%.2f", (Double)row.get("amount")),
                                row.get("method"),
                                row.get("status")
                            });
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}
