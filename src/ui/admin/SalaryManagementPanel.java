package ui.admin;

import java.awt.*;
import java.util.Calendar;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import dao.SalaryDAO;
import dao.TeacherDAO;
import model.SalaryRecord;
import model.Teacher;
import service.SalaryService;
import util.ThemeManager;

/**
 * Enhanced Salary Management panel — spec §9.
 * Changes vs. original:
 *   – 2 new summary cards: TOTAL EXTRA SLOTS + TOTAL EXTRA BONUS  (§9.1)
 *   – 2 new table columns: Extra Slots | Extra Bonus  (§9.2)
 *   – "+ Extra Slot" action button per row → LogExtraSlotDialog  (§9.3)
 *   – "⚙ Rules" button → SalaryRulesDialog  (§9.4)
 */
public class SalaryManagementPanel extends JPanel {

    private JComboBox<String> monthCombo;
    private JComboBox<String> yearCombo;
    private JTable salaryTable;
    private DefaultTableModel tableModel;

    // Summary card labels
    private JLabel totalPaidLbl;
    private JLabel totalDeductionLbl;
    private JLabel teachersCountLbl;
    private JLabel totalExtraSlotsLbl;
    private JLabel totalExtraBonusLbl;

    private SalaryDAO salaryDAO;
    private SalaryService salaryService;
    private TeacherDAO teacherDAO;

    public SalaryManagementPanel() {
        this.salaryDAO     = new SalaryDAO();
        this.salaryService = new SalaryService();
        this.teacherDAO    = new TeacherDAO();

        setLayout(new BorderLayout(0, 20));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(25, 30, 30, 30));

        initHeader();
        initStatsCards();
        initTable();

        loadData();
    }

    // ── Header (month/year selector + buttons) ──────────────────────────────

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.BG);

        JLabel title = new JLabel("Salary Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(ThemeManager.TEXT);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        filters.setBackground(ThemeManager.BG);

        monthCombo = new JComboBox<>(
            new String[]{"01","02","03","04","05","06","07","08","09","10","11","12"});
        yearCombo = new JComboBox<>(new String[]{"2025", "2026", "2027"});

        Calendar cal = Calendar.getInstance();
        monthCombo.setSelectedItem(String.format("%02d", cal.get(Calendar.MONTH) + 1));
        yearCombo.setSelectedItem(String.valueOf(cal.get(Calendar.YEAR)));

        // ⚙ Rules button — §9.4
        JButton rulesBtn = new JButton("⚙  Rules");
        rulesBtn.setBackground(new Color(107, 114, 128));
        rulesBtn.setForeground(Color.WHITE);
        rulesBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        rulesBtn.setFocusPainted(false);
        rulesBtn.setBorderPainted(false);
        rulesBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rulesBtn.addActionListener(e -> {
            Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
            new SalaryRulesDialog(parent).setVisible(true);
        });

        JButton calcBtn = new JButton("Calculate All");
        calcBtn.setBackground(new Color(34, 197, 94));
        calcBtn.setForeground(Color.WHITE);
        calcBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        calcBtn.setFocusPainted(false);
        calcBtn.setBorderPainted(false);
        calcBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        calcBtn.addActionListener(e -> calculateAll());

        JButton downloadSalaryPDF = new JButton("📥 Download PDF");
        downloadSalaryPDF.setBackground(new Color(59, 130, 246));
        downloadSalaryPDF.setForeground(Color.WHITE);
        downloadSalaryPDF.setFont(new Font("SansSerif", Font.BOLD, 13));
        downloadSalaryPDF.setFocusPainted(false);
        downloadSalaryPDF.setBorderPainted(false);
        downloadSalaryPDF.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("MRK_Salary_Report_"
                + new java.text.SimpleDateFormat("MMM_yyyy").format(new java.util.Date()) + ".pdf"));
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                service.SalaryPDFService pdfService = new service.SalaryPDFService();
                String result = pdfService.generateSalaryReport(chooser.getSelectedFile().getAbsolutePath());
                if (result != null) {
                    JOptionPane.showMessageDialog(null,
                        "✅ Salary report saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null,
                        "❌ Failed to generate PDF.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        filters.add(new JLabel("Month:"));
        filters.add(monthCombo);
        filters.add(new JLabel("Year:"));
        filters.add(yearCombo);
        filters.add(rulesBtn);
        filters.add(calcBtn);
        filters.add(downloadSalaryPDF);

        header.add(title,   BorderLayout.WEST);
        header.add(filters, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        monthCombo.addActionListener(e -> loadData());
        yearCombo.addActionListener(e  -> loadData());
    }

    // ── Stats cards (6 cards in a 2-row 3-col or single wide row) ───────────

    private void initStatsCards() {
        // Two rows: row 1 = 4 original cards, row 2 = 2 new cards centred
        JPanel statsPanel = new JPanel(new BorderLayout(0, 10));
        statsPanel.setBackground(ThemeManager.BG);

        JPanel row1 = new JPanel(new GridLayout(1, 4, 20, 0));
        row1.setBackground(ThemeManager.BG);

        teachersCountLbl    = new JLabel("0");
        totalPaidLbl        = new JLabel("₹0");
        totalDeductionLbl   = new JLabel("₹0");

        row1.add(createStatCard("TEACHERS",        teachersCountLbl,  new Color(59, 130, 246)));
        row1.add(createStatCard("TOTAL PAID",      totalPaidLbl,      new Color(34, 197, 94)));
        row1.add(createStatCard("TOTAL DEDUCTION", totalDeductionLbl, new Color(239, 68, 68)));
        row1.add(createStatCard("STATUS",          new JLabel("Processed"), new Color(139, 92, 246)));

        JPanel row2 = new JPanel(new GridLayout(1, 2, 20, 0));
        row2.setBackground(ThemeManager.BG);

        totalExtraSlotsLbl = new JLabel("0");
        totalExtraBonusLbl = new JLabel("₹0");

        row2.add(createStatCard("TOTAL EXTRA SLOTS", totalExtraSlotsLbl, new Color(245, 158, 11)));
        row2.add(createStatCard("TOTAL EXTRA BONUS",  totalExtraBonusLbl, new Color(16, 185, 129)));

        statsPanel.add(row1, BorderLayout.NORTH);
        statsPanel.add(row2, BorderLayout.CENTER);

        add(statsPanel, BorderLayout.CENTER);
    }

    // ── Table ─────────────────────────────────────────────────────────────────

    private void initTable() {
        // Columns — §9.2: inserted Extra Slots & Extra Bonus between Deduction and Final Salary
        String[] cols = {
            "Teacher ID", "Teacher Name", "Month", "Absent",
            "Deduction", "Extra Slots", "Extra Bonus", "Final Salary", "Action"
        };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        salaryTable = new JTable(tableModel);

        // Styling
        salaryTable.setRowHeight(40);
        salaryTable.setBackground(ThemeManager.CARD);
        salaryTable.setForeground(ThemeManager.TEXT);
        salaryTable.getTableHeader().setBackground(ThemeManager.BG);
        salaryTable.getTableHeader().setForeground(ThemeManager.TEXT);
        salaryTable.setSelectionBackground(new Color(59, 130, 246, 30));

        // "Action" column (last): render a button
        salaryTable.getColumnModel().getColumn(8).setPreferredWidth(100);
        salaryTable.getColumnModel().getColumn(8).setMaxWidth(120);

        JScrollPane scroll = new JScrollPane(salaryTable);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(ThemeManager.BG);
        bottom.add(scroll, BorderLayout.CENTER);
        bottom.setPreferredSize(new Dimension(0, 420));

        add(bottom, BorderLayout.SOUTH);

        // Mouse listener for "Action" column click — §9.3
        salaryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                int col = salaryTable.getColumnModel().getColumnIndexAtX(e.getX());
                int row = salaryTable.rowAtPoint(e.getPoint());
                if (col == 8 && row >= 0) {
                    openLogExtraSlotDialog(row);
                }
            }
        });
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadData() {
        tableModel.setRowCount(0);
        String m = (String) monthCombo.getSelectedItem();
        String y = (String) yearCombo.getSelectedItem();

        List<SalaryRecord> records = salaryDAO.getAllSalaryRecords(m, y);
        double totalPaid       = 0;
        double totalDed        = 0;
        int    totalExtraSlots = 0;
        double totalExtraBonus = 0;

        for (SalaryRecord r : records) {
            Teacher t = teacherDAO.getTeacherById(r.getTeacherId());
            String name = t != null ? t.getName() : "Unknown";
            boolean noData = (r.getPresentDays() == 0 && r.getAbsentDays() == 0);

            tableModel.addRow(new Object[]{
                r.getTeacherId(),
                name,
                r.getMonth() + "/" + r.getYear(),
                noData ? "No Data" : r.getAbsentDays(),
                noData ? "No Data" : String.format("₹%.2f", r.getDeduction()),
                noData ? "—"       : r.getExtraSlots(),
                noData ? "—"       : String.format("₹%.2f", r.getExtraBonus()),
                noData ? "No Data" : String.format("₹%.2f", r.getFinalSalary()),
                "+ Extra Slot"
            });

            if (!noData) {
                totalPaid       += r.getFinalSalary();
                totalDed        += r.getDeduction();
                totalExtraSlots += r.getExtraSlots();
                totalExtraBonus += r.getExtraBonus();
            }
        }

        teachersCountLbl.setText(String.valueOf(records.size()));
        totalPaidLbl.setText(      String.format("₹%.0f", totalPaid));
        totalDeductionLbl.setText( String.format("₹%.0f", totalDed));
        totalExtraSlotsLbl.setText(String.valueOf(totalExtraSlots));
        totalExtraBonusLbl.setText(String.format("₹%.0f", totalExtraBonus));
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void calculateAll() {
        List<Teacher> teachers = teacherDAO.getAllTeachers();
        int m = Integer.parseInt((String) monthCombo.getSelectedItem());
        int y = Integer.parseInt((String) yearCombo.getSelectedItem());

        for (Teacher t : teachers) {
            salaryService.calculateSalary(t.getUserId(), m, y);
        }
        loadData();
        JOptionPane.showMessageDialog(this, "Salary calculation completed for all teachers.");
    }

    /** Open the Log Extra Slot dialog for the teacher in the given table row. */
    private void openLogExtraSlotDialog(int row) {
        String teacherId = tableModel.getValueAt(row, 0).toString();
        Teacher teacher = teacherDAO.getTeacherById(teacherId);
        if (teacher == null) {
            JOptionPane.showMessageDialog(this, "Could not find teacher record.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String m = (String) monthCombo.getSelectedItem();
        String y = (String) yearCombo.getSelectedItem();

        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        new LogExtraSlotDialog(parent, teacher, m, y, this::loadData).setVisible(true);
    }

    // ── Card builder ─────────────────────────────────────────────────────────

    private JPanel createStatCard(String title, JLabel valLbl, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 5));
        card.setBackground(ThemeManager.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1),
            new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 10));
        t.setForeground(ThemeManager.SUB_TEXT);

        valLbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        valLbl.setForeground(accent);

        card.add(t,      BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        return card;
    }
}
