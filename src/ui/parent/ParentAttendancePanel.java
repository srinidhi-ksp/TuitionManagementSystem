package ui.parent;

import java.awt.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import model.Attendance;
import model.Student;
import service.ParentPortalService;
import util.SessionManager;
import util.ThemeManager;

public class ParentAttendancePanel extends JPanel {

    private ParentPortalService portalService;
    private JComboBox<String> studentSelector;
    private JTable attendanceTable;
    private DefaultTableModel model;
    private JLabel monthlyPercentLbl;
    private List<Student> linkedStudents;
    private Student currentStudent;

    public ParentAttendancePanel() {
        this.portalService = new ParentPortalService();
        setLayout(new BorderLayout(0, 24));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(32, 40, 40, 40));

        initHeader();
        initContent();
        loadInitialData();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.BG);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(ThemeManager.BG);
        JLabel title = new JLabel("Attendance Records");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(ThemeManager.TEXT);
        JLabel sub = new JLabel("View daily attendance logs and overall monthly consistency");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(ThemeManager.SUB_TEXT);
        titles.add(title);
        titles.add(sub);

        studentSelector = new JComboBox<>();
        studentSelector.setPreferredSize(new Dimension(220, 38));
        studentSelector.addActionListener(e -> onStudentSelected());

        header.add(titles, BorderLayout.WEST);
        header.add(studentSelector, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initContent() {
        JPanel center = new JPanel(new BorderLayout(0, 30));
        center.setBackground(ThemeManager.BG);

        // Top: Visualization Card
        JPanel statsCard = new JPanel(new BorderLayout());
        statsCard.setBackground(ThemeManager.CARD);
        statsCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true),
            new EmptyBorder(25, 30, 25, 30)
        ));
        statsCard.setPreferredSize(new Dimension(0, 120));

        JLabel info = new JLabel("Overall Monthly Consistency");
        info.setFont(new Font("SansSerif", Font.BOLD, 16));
        info.setForeground(ThemeManager.TEXT);
        
        monthlyPercentLbl = new JLabel("0.0%");
        monthlyPercentLbl.setFont(new Font("SansSerif", Font.BOLD, 32));
        monthlyPercentLbl.setForeground(new Color(34, 197, 94));

        statsCard.add(info, BorderLayout.WEST);
        statsCard.add(monthlyPercentLbl, BorderLayout.EAST);
        center.add(statsCard, BorderLayout.NORTH);

        // Bottom: Table
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(ThemeManager.CARD);
        tableCard.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true));

        String[] cols = {"Date", "Status", "Marked By", "Remarks"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        attendanceTable = new JTable(model);
        styleTable(attendanceTable);

        JScrollPane scroll = new JScrollPane(attendanceTable);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeManager.CARD);
        tableCard.add(scroll, BorderLayout.CENTER);

        center.add(tableCard, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t.setRowHeight(45);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        t.getTableHeader().setPreferredSize(new Dimension(0, 40));
        
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String status = (String) value;
                if ("PRESENT".equalsIgnoreCase(status)) l.setForeground(new Color(34, 197, 94));
                else if ("ABSENT".equalsIgnoreCase(status)) l.setForeground(new Color(239, 68, 68));
                else l.setForeground(new Color(245, 158, 11));
                l.setFont(new Font("SansSerif", Font.BOLD, 13));
                return l;
            }
        };
        t.getColumnModel().getColumn(1).setCellRenderer(statusRenderer);
    }

    private void loadInitialData() {
        linkedStudents = portalService.getLinkedStudents(SessionManager.getInstance().getUserId());
        for (Student s : linkedStudents) studentSelector.addItem(s.getName());
        if (!linkedStudents.isEmpty()) onStudentSelected();
    }

    private void onStudentSelected() {
        int idx = studentSelector.getSelectedIndex();
        if (idx >= 0) {
            currentStudent = linkedStudents.get(idx);
            refreshData();
        }
    }

    private void refreshData() {
        model.setRowCount(0);
        dao.AttendanceDAO attDAO = new dao.AttendanceDAO();
        List<Attendance> list = attDAO.getAttendanceByStudentId(currentStudent.getUserId());
        
        int presentCount = 0;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy");
        for (Attendance a : list) {
            if ("PRESENT".equalsIgnoreCase(a.getStatus())) presentCount++;
            model.addRow(new Object[]{
                a.getAttendanceDate() != null ? sdf.format(a.getAttendanceDate()) : "—",
                a.getStatus(),
                a.getMarkedBy(),
                a.getReason() != null ? a.getReason() : "—"
            });
        }

        double percent = list.isEmpty() ? 0 : (double) presentCount / list.size() * 100;
        monthlyPercentLbl.setText(String.format("%.1f%%", percent));
        monthlyPercentLbl.setForeground(percent >= 75 ? new Color(34, 197, 94) : new Color(239, 68, 68));
    }
}
