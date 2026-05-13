package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import model.SalaryRecord;
import model.User;
import service.SalaryPDFService;
import util.SessionManager;
import util.ThemeManager;

/**
 * TeacherSalaryPanel - Allows teachers to view their salary history and download payslips.
 */
public class TeacherSalaryPanel extends JPanel {

    private User teacher;
    private dao.TeacherDAO teacherDAO;
    private SalaryPDFService pdfService;
    private JTable salaryTable;
    private DefaultTableModel tableModel;

    public TeacherSalaryPanel(User teacher) {
        this.teacher = teacher;
        this.teacherDAO = new dao.TeacherDAO();
        this.pdfService = new SalaryPDFService();

        setLayout(new BorderLayout(0, 20));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(32, 40, 40, 40));

        initHeader();
        initTable();
        loadSalaryHistory();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.BG);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(ThemeManager.BG);
        JLabel title = new JLabel("My Salary & Payslips");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(ThemeManager.TEXT);
        JLabel sub = new JLabel("View your monthly earnings and download professional payslips");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(ThemeManager.SUB_TEXT);
        titles.add(title);
        titles.add(sub);

        JButton refreshBtn = new JButton("↻  Refresh History");
        refreshBtn.addActionListener(e -> loadSalaryHistory());

        header.add(titles, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initTable() {
        String[] columns = {"Month", "Year", "Working Days", "Present", "Deductions", "Final Salary", "Action"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only action column is editable for buttons
            }
        };

        salaryTable = new JTable(tableModel);
        salaryTable.setRowHeight(48);
        salaryTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        salaryTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        salaryTable.setShowGrid(false);
        salaryTable.setIntercellSpacing(new Dimension(0, 0));

        // Action Column: Download Button
        salaryTable.getColumnModel().getColumn(6).setCellRenderer(new TableButtonRenderer());
        salaryTable.getColumnModel().getColumn(6).setCellEditor(new TableButtonEditor(new JCheckBox()));

        JScrollPane scroll = new JScrollPane(salaryTable);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        add(scroll, BorderLayout.CENTER);
    }

    private void loadSalaryHistory() {
        tableModel.setRowCount(0);
        
        // As per requirements: "You DO NOT have a salary collection. So DO NOT query salary table."
        // We fetch the fixed salary from the Teacher document directly.
        model.Teacher t = teacherDAO.getTeacherById(SessionManager.getCurrentTeacherId());
        
        if (t != null) {
            String currentMonth = new java.text.SimpleDateFormat("MMMM").format(new java.util.Date());
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            
            // Placeholder values for attendance/deductions since we don't have a salary record collection
            int workingDays = 30;
            int presentDays = 28; 
            double deduction = 0;
            double finalSalary = t.getSalary();

            tableModel.addRow(new Object[]{
                currentMonth,
                currentYear,
                workingDays,
                presentDays,
                "₹" + deduction,
                "₹" + finalSalary,
                t // Pass teacher object to enable PDF generation
            });
        } else {
            tableModel.addRow(new Object[]{"No teacher data found", "-", "-", "-", "-", "-", "-"});
        }
    }

    private void downloadPayslip(model.Teacher teacherRecord) {
        JFileChooser chooser = new JFileChooser();
        String month = new java.text.SimpleDateFormat("MMMM").format(new java.util.Date());
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        chooser.setSelectedFile(new File("Payslip_" + month + "_" + year + ".pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = pdfService.generateTeacherPayslip(teacherRecord, chooser.getSelectedFile().getAbsolutePath());
            if (path != null) {
                JOptionPane.showMessageDialog(this, "Payslip downloaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to generate PDF.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Table Button Components ──────────────────────────────────────────────────

    class TableButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public TableButtonRenderer() {
            setOpaque(true);
            setText("Download PDF");
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setBackground(new Color(59, 130, 246));
            setForeground(Color.WHITE);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof String) setEnabled(false); // No data case
            else setEnabled(true);
            return this;
        }
    }

    class TableButtonEditor extends DefaultCellEditor {
        private JButton button;
        private model.Teacher currentTeacher;

        public TableButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("Download PDF");
            button.setOpaque(true);
            button.addActionListener(e -> {
                fireEditingStopped();
                if (currentTeacher != null) downloadPayslip(currentTeacher);
            });
        }
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof model.Teacher) {
                currentTeacher = (model.Teacher) value;
            }
            return button;
        }
        @Override
        public Object getCellEditorValue() {
            return currentTeacher;
        }
    }
}
