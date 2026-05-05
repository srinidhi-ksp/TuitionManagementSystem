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

public class SalaryManagementPanel extends JPanel {

    private JComboBox<String> monthCombo;
    private JComboBox<String> yearCombo;
    private JTable salaryTable;
    private DefaultTableModel tableModel;
    private JLabel totalPaidLbl, totalDeductionLbl, teachersCountLbl;

    private SalaryDAO salaryDAO;
    private SalaryService salaryService;
    private TeacherDAO teacherDAO;

    public SalaryManagementPanel() {
        this.salaryDAO = new SalaryDAO();
        this.salaryService = new SalaryService();
        this.teacherDAO = new TeacherDAO();

        setLayout(new BorderLayout(0, 20));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(25, 30, 30, 30));

        initHeader();
        initStatsCards();
        initTable();

        loadData();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.BG);

        JLabel title = new JLabel("Salary Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(ThemeManager.TEXT);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        filters.setBackground(ThemeManager.BG);

        monthCombo = new JComboBox<>(new String[]{"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"});
        yearCombo = new JComboBox<>(new String[]{"2025", "2026", "2027"});
        
        Calendar cal = Calendar.getInstance();
        monthCombo.setSelectedItem(String.format("%02d", cal.get(Calendar.MONTH) + 1));
        yearCombo.setSelectedItem(String.valueOf(cal.get(Calendar.YEAR)));

        JButton calcBtn = new JButton("Calculate All");
        calcBtn.addActionListener(e -> calculateAll());

        JButton downloadSalaryPDF = new JButton("📥 Download Salary Report PDF");
        downloadSalaryPDF.setBackground(new Color(59, 130, 246));
        downloadSalaryPDF.setForeground(Color.WHITE);
        downloadSalaryPDF.setFont(new Font("SansSerif", Font.BOLD, 13));
        downloadSalaryPDF.setFocusPainted(false);
        downloadSalaryPDF.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("MRK_Salary_Report_" +
                new java.text.SimpleDateFormat("MMM_yyyy").format(new java.util.Date()) + ".pdf"));
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                service.SalaryPDFService pdfService = new service.SalaryPDFService();
                String result = pdfService.generateSalaryReport(chooser.getSelectedFile().getAbsolutePath());
                if (result != null) {
                    JOptionPane.showMessageDialog(null, "✅ Salary report saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "❌ Failed to generate PDF.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        filters.add(new JLabel("Month:"));
        filters.add(monthCombo);
        filters.add(new JLabel("Year:"));
        filters.add(yearCombo);
        filters.add(calcBtn);
        filters.add(downloadSalaryPDF);

        header.add(title, BorderLayout.WEST);
        header.add(filters, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        monthCombo.addActionListener(e -> loadData());
        yearCombo.addActionListener(e -> loadData());
    }

    private void initStatsCards() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(ThemeManager.BG);

        teachersCountLbl = new JLabel("0");
        totalPaidLbl = new JLabel("₹0");
        totalDeductionLbl = new JLabel("₹0");

        statsPanel.add(createStatCard("TEACHERS", teachersCountLbl, new Color(59, 130, 246)));
        statsPanel.add(createStatCard("TOTAL PAID", totalPaidLbl, new Color(34, 197, 94)));
        statsPanel.add(createStatCard("TOTAL DEDUCTION", totalDeductionLbl, new Color(239, 68, 68)));
        statsPanel.add(createStatCard("STATUS", new JLabel("Processed"), new Color(139, 92, 246)));

        add(statsPanel, BorderLayout.CENTER);
    }

    private void initTable() {
        String[] cols = {"Teacher ID", "Teacher Name", "Month", "Absent", "Deduction", "Final Salary"};
        tableModel = new DefaultTableModel(cols, 0);
        salaryTable = new JTable(tableModel);
        
        // Custom styling
        salaryTable.setRowHeight(40);
        salaryTable.setBackground(ThemeManager.CARD);
        salaryTable.setForeground(ThemeManager.TEXT);
        salaryTable.getTableHeader().setBackground(ThemeManager.BG);
        salaryTable.getTableHeader().setForeground(ThemeManager.TEXT);
        
        JScrollPane scroll = new JScrollPane(salaryTable);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(ThemeManager.BG);
        bottom.add(scroll, BorderLayout.CENTER);
        bottom.setPreferredSize(new Dimension(0, 400));
        
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadData() {
        tableModel.setRowCount(0);
        String m = (String) monthCombo.getSelectedItem();
        String y = (String) yearCombo.getSelectedItem();

        List<SalaryRecord> records = salaryDAO.getAllSalaryRecords(m, y);
        double totalPaid = 0;
        double totalDed = 0;

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
                noData ? "No Data" : String.format("₹%.2f", r.getFinalSalary())
            });

            if (!noData) {
                totalPaid += r.getFinalSalary();
                totalDed += r.getDeduction();
            }
        }

        teachersCountLbl.setText(String.valueOf(records.size()));
        totalPaidLbl.setText(String.format("₹%.0f", totalPaid));
        totalDeductionLbl.setText(String.format("₹%.0f", totalDed));
    }

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

        card.add(t, BorderLayout.NORTH);
        card.add(valLbl, BorderLayout.CENTER);
        return card;
    }
}
