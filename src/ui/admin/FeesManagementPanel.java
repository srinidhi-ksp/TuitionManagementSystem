package ui.admin;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import dao.FeeAnalyticsDAO;
import dao.StudentDAO;
import model.Student;
import model.SubjectFeeDTO;
import service.FeeService;
import util.ThemeManager;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import db.DBConnection;

/**
 * Fees & Payments Module - Redesigned
 * Architecture: Separated into Analytics Dashboard and Student-wise Management
 */
public class FeesManagementPanel extends JPanel {

    private static final Color COLOR_ACCENT  = new Color(59, 130, 246);
    private static final Color COLOR_SUCCESS = new Color(34, 197, 94);
    private static final Color COLOR_ERROR   = new Color(239, 68, 68);
    private static final Color COLOR_PENDING = new Color(245, 158, 11); // Orange-600

    private FeeService feeService;
    private StudentDAO studentDAO;
    private FeeAnalyticsDAO analyticsDAO;

    // Components for Tab 1 (Analytics)
    private DefaultTableModel analyticsModel;
    private JComboBox<String> analyticsFilterCombo;
    private JLabel totalStudentsCard, paidStudentsCard, unpaidStudentsCard;
    private DoughnutChartPanel chartPanel;

    // Components for Tab 2 (Management)
    private JTable managementTable;
    private DefaultTableModel managementModel;
    private JComboBox<String> studentSelectCombo;
    private Map<String, String> studentIdMap;
    private JLabel totalFeeCard, paidAmountCard, pendingAmountCard, statusCard;

    public FeesManagementPanel() {
        this.feeService = new FeeService();
        this.studentDAO = new StudentDAO();
        this.analyticsDAO = new FeeAnalyticsDAO();
        
        setLayout(new BorderLayout());
        setBackground(ThemeManager.BG);
        
        add(createHeader(), BorderLayout.NORTH);
        add(createTabbedPane(), BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.BG);
        header.setBorder(new EmptyBorder(28, 36, 12, 36));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setBackground(ThemeManager.BG);
        JLabel title = new JLabel("Fees & Payments");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(ThemeManager.TEXT);
        JLabel sub = new JLabel("Track financial performance and student payments");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(ThemeManager.SUB_TEXT);
        titlePanel.add(title);
        titlePanel.add(sub);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 15));
        rightPanel.setBackground(ThemeManager.BG);

        JButton downloadFeesPDF = new JButton("📥 Download Fee Report PDF");
        downloadFeesPDF.setBackground(new Color(59, 130, 246));
        downloadFeesPDF.setForeground(Color.WHITE);
        downloadFeesPDF.setFont(new Font("SansSerif", Font.BOLD, 13));
        downloadFeesPDF.setFocusPainted(false);
        downloadFeesPDF.setPreferredSize(new Dimension(220, 38));
        downloadFeesPDF.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("MRK_Fee_Report_" +
                new java.text.SimpleDateFormat("MMM_yyyy").format(new java.util.Date()) + ".pdf"));
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                service.FeeReportPDFService svc = new service.FeeReportPDFService();
                String result = svc.generateFeeReport(chooser.getSelectedFile().getAbsolutePath());
                if (result != null) {
                    JOptionPane.showMessageDialog(null, "✅ Fee report saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "❌ Failed to generate PDF.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        rightPanel.add(downloadFeesPDF);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.setBorder(new EmptyBorder(0, 36, 36, 36));
        tabs.setBackground(ThemeManager.BG);

        tabs.addTab("📊  Fee Analytics Dashboard", createAnalyticsTab());
        tabs.addTab("📋  Student-wise Fee Management", createManagementTab());

        return tabs;
    }

    // ─────────────────────────── TAB 1: ANALYTICS ───────────────────────────
    private JPanel createAnalyticsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(ThemeManager.BG);
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));

        // Top Summary Cards
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setBackground(ThemeManager.BG);
        
        totalStudentsCard = new JLabel("0", SwingConstants.LEFT);
        paidStudentsCard = new JLabel("0", SwingConstants.LEFT);
        unpaidStudentsCard = new JLabel("0", SwingConstants.LEFT);

        statsPanel.add(createStatCard("TOTAL STUDENTS", totalStudentsCard, COLOR_ACCENT));
        statsPanel.add(createStatCard("PAID STUDENTS", paidStudentsCard, COLOR_SUCCESS));
        statsPanel.add(createStatCard("UNPAID STUDENTS", unpaidStudentsCard, COLOR_ERROR));

        panel.add(statsPanel, BorderLayout.NORTH);

        // Center Content: Chart and Filtered Table
        JPanel centerPanel = new JPanel(new BorderLayout(20, 20));
        centerPanel.setBackground(ThemeManager.BG);

        // LEFT: Chart Card
        JPanel chartCard = new JPanel(new BorderLayout());
        chartCard.setBackground(ThemeManager.CARD);
        chartCard.setPreferredSize(new Dimension(350, 0));
        chartCard.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true));
        
        JPanel chartHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        chartHeader.setBackground(ThemeManager.CARD);
        JLabel chartTitle = new JLabel("Payment Distribution");
        chartTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        chartHeader.add(chartTitle);
        chartCard.add(chartHeader, BorderLayout.NORTH);
        
        chartPanel = new DoughnutChartPanel(0, 0);
        chartCard.add(chartPanel, BorderLayout.CENTER);
        
        centerPanel.add(chartCard, BorderLayout.WEST);

        // RIGHT: Table Card
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(ThemeManager.CARD);
        tableCard.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true));

        // Table Header with Filter
        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setBackground(ThemeManager.CARD);
        tableHeader.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        JLabel listTitle = new JLabel("Student Fee Status");
        listTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        tableHeader.add(listTitle, BorderLayout.WEST);

        analyticsFilterCombo = new JComboBox<>(new String[]{"All Students", "Paid Students", "Unpaid Students", "Partially Paid Students"});
        analyticsFilterCombo.setPreferredSize(new Dimension(180, 30));
        analyticsFilterCombo.addActionListener(e -> refreshAnalytics());
        tableHeader.add(analyticsFilterCombo, BorderLayout.EAST);
        
        tableCard.add(tableHeader, BorderLayout.NORTH);

        String[] cols = {"Student Name", "Standard", "Batch", "Status"};
        analyticsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable analyticsTable = new JTable(analyticsModel);
        styleAnalyticsTable(analyticsTable);

        JScrollPane scroll = new JScrollPane(analyticsTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(ThemeManager.CARD);
        tableCard.add(scroll, BorderLayout.CENTER);

        centerPanel.add(tableCard, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        refreshAnalytics();
        return panel;
    }

    private void styleAnalyticsTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(ThemeManager.DIVIDER);
        
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBackground(ThemeManager.CARD);
        t.getTableHeader().setPreferredSize(new Dimension(0, 35));
        
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(table, value, sel, foc, r, c);
                setBackground(sel ? (ThemeManager.isDarkMode ? ThemeManager.DARK_HOVER : new Color(241, 245, 249)) : ThemeManager.CARD);
                setForeground(ThemeManager.TEXT);
                setBorder(new EmptyBorder(0, 15, 0, 0));
                
                if (c == 3) { // Status column
                    String status = String.valueOf(value).toUpperCase();
                    if (status.equals("PAID")) setForeground(COLOR_SUCCESS);
                    else if (status.equals("PARTIAL")) setForeground(COLOR_PENDING); // orange
                    else if (status.equals("UNPAID")) setForeground(COLOR_ERROR);
                    else if (status.equals("PENDING")) setForeground(COLOR_PENDING);
                    else setForeground(ThemeManager.SUB_TEXT);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                return comp;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++) t.getColumnModel().getColumn(i).setCellRenderer(renderer);
    }

    private void refreshAnalytics() {
        List<Map<String, Object>> data = analyticsDAO.getAllStudentFeeStatus();
        String filter = (String) analyticsFilterCombo.getSelectedItem();
        
        long total = data.size();
        long paid = data.stream().filter(d -> "PAID".equals(d.get("status"))).count();
        long unpaid = data.stream().filter(d -> "UNPAID".equals(d.get("status"))).count();
        long partial = data.stream().filter(d -> "PARTIAL".equals(d.get("status"))).count();

        totalStudentsCard.setText(String.valueOf(total));
        paidStudentsCard.setText(String.valueOf(paid));
        unpaidStudentsCard.setText(String.valueOf(unpaid + partial)); // Group partial with unpaid for summary color or split if needed
        
        chartPanel.updateData(paid, unpaid + partial);

        analyticsModel.setRowCount(0);
        for (Map<String, Object> d : data) {
            String status = (String) d.get("status");
            if ("Paid Students".equals(filter) && !"PAID".equals(status)) continue;
            if ("Unpaid Students".equals(filter) && !"UNPAID".equals(status)) continue;
            if ("Partially Paid Students".equals(filter) && !"PARTIAL".equals(status)) continue;
            
            analyticsModel.addRow(new Object[]{
                d.get("name"),
                d.get("standard"),
                d.get("batch"),
                status
            });
        }
    }

    // ─────────────────────────── TAB 2: MANAGEMENT ───────────────────────────
    private JPanel createManagementTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 24));
        panel.setBackground(ThemeManager.BG);
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));

        // 1. Initialize models first
        String[] cols = {"Subject", "Monthly Fee", "Method", "Status", "Action"};
        managementModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 4; }
        };
        managementTable = new JTable(managementModel);
        styleManagementTable(managementTable);

        // 2. Setup Student Selector
        JPanel selectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        selectPanel.setBackground(ThemeManager.BG);
        
        JLabel lbl = new JLabel("Select Student: ");
        lbl.setForeground(ThemeManager.TEXT);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        
        studentSelectCombo = new JComboBox<>();
        studentSelectCombo.setPreferredSize(new Dimension(300, 38));
        
        // Load data BEFORE adding listener to avoid early trigger
        loadStudentList();
        studentSelectCombo.addActionListener(e -> loadStudentFees());
        
        selectPanel.add(lbl);
        selectPanel.add(studentSelectCombo);
        panel.add(selectPanel, BorderLayout.NORTH);

        // Content: Cards and Table
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(ThemeManager.BG);

        // Stats Row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 20, 0));
        statsRow.setBackground(ThemeManager.BG);
        
        totalFeeCard = new JLabel("₹0.00");
        paidAmountCard = new JLabel("₹0.00");
        pendingAmountCard = new JLabel("₹0.00");
        statusCard = new JLabel("—");

        statsRow.add(createStatCard("TOTAL FEES", totalFeeCard, COLOR_ACCENT));
        statsRow.add(createStatCard("PAID", paidAmountCard, COLOR_SUCCESS));
        statsRow.add(createStatCard("PENDING", pendingAmountCard, COLOR_ERROR));
        statsRow.add(createStatCard("STATUS", statusCard, ThemeManager.SUB_TEXT));

        content.add(statsRow, BorderLayout.NORTH);

        // Management Table Card
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(ThemeManager.CARD);
        tableCard.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true));

        JScrollPane scroll = new JScrollPane(managementTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(ThemeManager.CARD);
        tableCard.add(scroll, BorderLayout.CENTER);

        content.add(tableCard, BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private void styleManagementTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t.setRowHeight(44);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(ThemeManager.DIVIDER);
        
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        t.getTableHeader().setBackground(ThemeManager.isDarkMode ? ThemeManager.DARK_TABLE_HEADER : new Color(250, 251, 253));
        t.getTableHeader().setPreferredSize(new Dimension(0, 40));
        
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(table, value, sel, foc, r, c);
                setBackground(sel ? (ThemeManager.isDarkMode ? ThemeManager.DARK_HOVER : new Color(241, 245, 249)) : ThemeManager.CARD);
                setForeground(ThemeManager.TEXT);
                setBorder(new EmptyBorder(0, 15, 0, 0));
                
                if (c == 3) {
                    String status = String.valueOf(value).toUpperCase();
                    if (status.equals("PAID") || status.equals("SUCCESS")) setForeground(COLOR_SUCCESS);
                    else if (status.equals("UNPAID")) setForeground(COLOR_ERROR);
                    else if (status.equals("PENDING") || status.equals("REQUESTED")) setForeground(COLOR_PENDING);
                    else setForeground(ThemeManager.SUB_TEXT);
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                return comp;
            }
        };
        for (int i = 0; i < 4; i++) t.getColumnModel().getColumn(i).setCellRenderer(renderer);
        
        // Add "Pay Now" button functionality here if needed (omitted for brevity, assume similar to original)
    }

    private void loadStudentList() {
        studentIdMap = new java.util.HashMap<>();
        List<Student> students = studentDAO.getAllStudents();
        studentSelectCombo.addItem("Select a student...");
        for (Student s : students) {
            String label = s.getName() + " (" + s.getUserId() + ")";
            studentSelectCombo.addItem(label);
            studentIdMap.put(label, s.getUserId());
        }
    }

    private void loadStudentFees() {
        if (managementModel == null) return;
        managementModel.setRowCount(0);
        String selected = (String) studentSelectCombo.getSelectedItem();
        if (selected == null || selected.startsWith("Select")) return;
        
        String sId = studentIdMap.get(selected);
        Map<String, Object> summary = feeService.getFeeSummary(sId);
        List<SubjectFeeDTO> details = feeService.getStudentFeeDetails(sId);

        totalFeeCard.setText(String.format("₹%.2f", summary.get("totalFee")));
        paidAmountCard.setText(String.format("₹%.2f", summary.get("paidAmount")));
        pendingAmountCard.setText(String.format("₹%.2f", summary.get("pendingAmount")));
        
        String status = (String) summary.get("status");
        statusCard.setText(status);
        
        switch (status) {
            case "PAID":    statusCard.setForeground(COLOR_SUCCESS); break;
            case "PARTIAL": statusCard.setForeground(COLOR_PENDING); break; // orange
            case "PENDING": statusCard.setForeground(COLOR_PENDING); break; // orange
            default:        statusCard.setForeground(COLOR_ERROR);   break; // UNPAID = red
        }

        if ("NO_ENROLLMENT".equals(status)) {
            managementModel.addRow(new Object[]{"NO ENROLLMENT FOUND", "--", "--", "--"});
            return;
        }

        double pendingAmt = (double) summary.get("pendingAmount");

        for (SubjectFeeDTO f : details) {
            boolean canPay = "REQUESTED".equalsIgnoreCase(f.getDetailedStatus());
            managementModel.addRow(new Object[]{
                f.getSubjectName(),
                String.format("₹%.2f", f.getMonthlyFee()),
                f.getPaymentMethod(),
                f.getDetailedStatus(),
                canPay ? "Mark as Paid" : "—"
            });
        }

        // Wire "Mark as Paid" button column
        managementTable.getColumnModel().getColumn(4).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                String val = v != null ? v.toString() : "";
                if ("Mark as Paid".equals(val)) {
                    JButton btn = new JButton("Mark as Paid \u2713");
                    btn.setFont(new Font("SansSerif", Font.BOLD, 11));
                    btn.setBackground(new Color(34, 197, 94)); btn.setForeground(Color.WHITE);
                    btn.setOpaque(true); btn.setBorderPainted(false);
                    return btn;
                }
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                lbl.setForeground(new Color(107, 122, 153));
                lbl.setBorder(new EmptyBorder(0, 14, 0, 0));
                return lbl;
            }
        });
        managementTable.getColumnModel().getColumn(4).setCellEditor(
            new javax.swing.DefaultCellEditor(new JTextField()) {
                @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
                    String val = v != null ? v.toString() : "";
                    if ("Mark as Paid".equals(val)) {
                        // Trigger payment and refresh
                        SwingUtilities.invokeLater(() -> {
                            String sId = studentIdMap.get((String) studentSelectCombo.getSelectedItem());
                            SubjectFeeDTO fee = details.get(r);
                            
                            int confirm = JOptionPane.showConfirmDialog(FeesManagementPanel.this, 
                                "Mark " + fee.getSubjectName() + " as PAID for " + sId + "?\nThis will update collections and reduce pending fees.", 
                                "Confirm Payment Approval", JOptionPane.YES_NO_OPTION);
                                
                            if (confirm == JOptionPane.YES_OPTION) {
                                boolean ok = new dao.PaymentDAO().markBatchAsPaid(sId, fee.getBatchId(), fee.getMonthlyFee());

                                if (ok) {
                                    JOptionPane.showMessageDialog(FeesManagementPanel.this,
                                        "✓ Payment APPROVED for " + fee.getSubjectName());
                                } else {
                                    JOptionPane.showMessageDialog(FeesManagementPanel.this,
                                        "Error processing approval.", "Error", JOptionPane.ERROR_MESSAGE);
                                }
                                
                                loadStudentFees();
                                refreshAnalytics(); // Force refresh of the analytics tab as well
                                revalidate();
                                repaint();
                            }
                        });
                        fireEditingStopped();
                    }
                    return new JLabel(val);
                }
                @Override public Object getCellEditorValue() { return ""; }
            });

    }

    // ─────────────────────────── UI COMPONENTS ───────────────────────────
    private JPanel createStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
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

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        titleLbl.setForeground(ThemeManager.SUB_TEXT);

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        valueLabel.setForeground(ThemeManager.TEXT);
        
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────── CUSTOM CHART ───────────────────────────
    private static class DoughnutChartPanel extends JPanel {
        private long paid, unpaid;
        public DoughnutChartPanel(long p, long u) {
            this.paid = p; this.unpaid = u;
            setBackground(ThemeManager.CARD);
        }
        public void updateData(long p, long u) {
            this.paid = p; this.unpaid = u;
            repaint();
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int size = Math.min(getWidth(), getHeight()) - 100;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            
            long total = paid + unpaid;
            if (total == 0) {
                g2.setColor(ThemeManager.DIVIDER);
                g2.drawOval(x, y, size, size);
                g2.dispose(); return;
            }
            
            int anglePaid = (int) (360 * paid / total);
            int angleUnpaid = 360 - anglePaid;
            
            g2.setColor(COLOR_SUCCESS);
            g2.fillArc(x, y, size, size, 90, anglePaid);
            g2.setColor(COLOR_ERROR);
            g2.fillArc(x, y, size, size, 90 + anglePaid, angleUnpaid);
            
            // Hole for Doughnut
            int holeSize = (int) (size * 0.6);
            g2.setColor(ThemeManager.CARD);
            g2.fillOval(x + (size - holeSize) / 2, y + (size - holeSize) / 2, holeSize, holeSize);
            
            // Legend
            int ly = y + size + 30;
            drawLegend(g2, x + 20, ly, COLOR_SUCCESS, "Paid (" + paid + ")");
            drawLegend(g2, x + size/2 + 20, ly, COLOR_ERROR, "Unpaid (" + unpaid + ")");
            
            g2.dispose();
        }
        private void drawLegend(Graphics2D g2, int x, int y, Color c, String text) {
            g2.setColor(c);
            g2.fillRoundRect(x, y, 12, 12, 4, 4);
            g2.setColor(ThemeManager.TEXT);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(text, x + 18, y + 10);
        }
    }
}
