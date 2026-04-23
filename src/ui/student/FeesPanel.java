package ui.student;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Cursor;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.AbstractCellEditor;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JCheckBox;
import javax.swing.DefaultCellEditor;
import java.awt.Frame;
import java.awt.Insets;

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
        JPanel content = new JPanel(new BorderLayout(0, 24));
        content.setBackground(PAGE_BG);

        // Summary Cards
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        cardsPanel.setBackground(PAGE_BG);
        
        totalFeeCard = new JLabel("₹0.00", SwingConstants.CENTER);
        paidAmountCard = new JLabel("₹0.00", SwingConstants.CENTER);
        pendingAmountCard = new JLabel("₹0.00", SwingConstants.CENTER);
        statusCard = new JLabel("—", SwingConstants.CENTER);

        cardsPanel.add(createStatCard("Total Fees", totalFeeCard, ACCENT));
        cardsPanel.add(createStatCard("Amount Paid", paidAmountCard, SUCCESS_GREEN));
        cardsPanel.add(createStatCard("Pending Balance", pendingAmountCard, WARNING_ORANGE));
        cardsPanel.add(createStatCard("Overall Status", statusCard, ERROR_RED));

        // Table Panel
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(CARD_BG);
        tableCard.setBorder(BorderFactory.createLineBorder(new Color(230, 235, 245), 1, true));

        String[] cols = {"Subject Name", "Monthly Fee", "Payment Status", "Action", "subjectId"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3; }
        };
        feesTable = new JTable(tableModel);
        styleTable(feesTable);
        
        // Hide subjectId column
        feesTable.getColumnModel().getColumn(4).setMinWidth(0);
        feesTable.getColumnModel().getColumn(4).setMaxWidth(0);
        feesTable.getColumnModel().getColumn(4).setPreferredWidth(0);

        JScrollPane scroll = new JScrollPane(feesTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

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
                    if ("PAID".equals(value)) {
                        setForeground(SUCCESS_GREEN);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(ERROR_RED);
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
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
            if ("PAID".equals(status)) {
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
        private String subjectId;
        private int row;

        public ButtonEditor() {
            button = new JButton();
            button.setFont(new Font("SansSerif", Font.BOLD, 12));
            button.addActionListener(e -> {
                row = feesTable.getEditingRow();
                if (row == -1) return;
                subjectId = (String) tableModel.getValueAt(row, 4);
                String studentId = SessionManager.getInstance().getUserId();
                
                model.Receipt receipt = feeService.generateReceipt(studentId, subjectId);
                if (receipt != null) {
                    new ReceiptDialog((Frame) SwingUtilities.getWindowAncestor(FeesPanel.this), receipt).setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(FeesPanel.this, "Error generating receipt.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                fireEditingStopped();
            });
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int r, int c) {
            String status = (String) table.getValueAt(r, 2);
            if ("PAID".equals(status)) {
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
        String studentId = SessionManager.getInstance().getUserId();
        if (studentId == null) return;

        new javax.swing.SwingWorker<Void, Void>() {
            List<SubjectFeeDTO> fees;
            Map<String, Object> summary;

            @Override
            protected Void doInBackground() throws Exception {
                fees = feeService.getStudentFeeDetails(studentId);
                summary = feeService.getFeeSummary(studentId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    tableModel.setRowCount(0);
                    if (summary != null) {
                        totalFeeCard.setText(String.format("₹%.2f", (Double)summary.get("totalFee")));
                        paidAmountCard.setText(String.format("₹%.2f", (Double)summary.get("paidAmount")));
                        pendingAmountCard.setText(String.format("₹%.2f", (Double)summary.get("pendingAmount")));
                        
                        String status = (String) summary.get("status");
                        statusCard.setText(status);
                        statusCard.setForeground("PAID".equals(status) ? SUCCESS_GREEN : ERROR_RED);
                    }

                    if (fees != null) {
                        for (SubjectFeeDTO f : fees) {
                            tableModel.addRow(new Object[]{
                                f.getSubjectName(), 
                                String.format("₹%.2f", f.getMonthlyFee()), 
                                f.getPaymentStatus(),
                                "Generate Receipt",
                                f.getSubjectId()
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
