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
    private JComboBox<String> monthSelector;
    private JComboBox<String> yearSelector;
    private JLabel totalClassesLbl;
    private JLabel presentLbl;
    private JLabel absentLbl;
    private JProgressBar attProgressBar;
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

        
        JPanel filterPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        filterPanel.setBackground(ThemeManager.BG);
        
        String[] months = {"All Months", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        monthSelector = new JComboBox<>(months);
        monthSelector.setPreferredSize(new Dimension(100, 38));
        monthSelector.addActionListener(e -> onStudentSelected());

        String[] years = {"All Years", "2024", "2025", "2026", "2027"};
        yearSelector = new JComboBox<>(years);
        yearSelector.setPreferredSize(new Dimension(100, 38));
        yearSelector.addActionListener(e -> onStudentSelected());

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setPreferredSize(new Dimension(100, 38));
        refreshBtn.addActionListener(e -> onStudentSelected());

        filterPanel.add(monthSelector);
        filterPanel.add(yearSelector);
        filterPanel.add(refreshBtn);
        filterPanel.add(studentSelector);

        header.add(titles, BorderLayout.WEST);
        header.add(filterPanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
    }

    private void initContent() {
        JPanel center = new JPanel(new BorderLayout(0, 30));
        center.setBackground(ThemeManager.BG);

        // Top: Analytics Cards
        JPanel statsCard = new JPanel(new GridLayout(1, 4, 20, 0));
        statsCard.setBackground(ThemeManager.BG);
        statsCard.setPreferredSize(new Dimension(0, 120));

        totalClassesLbl = new JLabel("0");
        presentLbl = new JLabel("0");
        absentLbl = new JLabel("0");
        monthlyPercentLbl = new JLabel("0.0%");

        statsCard.add(createSummaryMiniCard("TOTAL CLASSES", totalClassesLbl, new Color(59, 130, 246)));
        statsCard.add(createSummaryMiniCard("PRESENT DAYS", presentLbl, new Color(34, 197, 94)));
        statsCard.add(createSummaryMiniCard("ABSENT DAYS", absentLbl, new Color(239, 68, 68)));
        
        JPanel percentPanel = createSummaryMiniCard("ATTENDANCE %", monthlyPercentLbl, new Color(168, 85, 247));
        attProgressBar = new JProgressBar(0, 100);
        attProgressBar.setPreferredSize(new Dimension(100, 6));
        attProgressBar.setForeground(new Color(34, 197, 94));
        attProgressBar.setBorderPainted(false);
        percentPanel.add(attProgressBar, BorderLayout.SOUTH);
        statsCard.add(percentPanel);

        center.add(statsCard, BorderLayout.NORTH);

        // Bottom: Table
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(ThemeManager.CARD);
        tableCard.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true));

        String[] cols = {"Date", "Day", "Batch", "Status", "Marked By", "Remarks"};
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

    private JPanel createSummaryMiniCard(String title, JLabel val, Color accent) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(ThemeManager.CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true),
            new EmptyBorder(20, 24, 20, 24)
        ));
        
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 11));
        t.setForeground(ThemeManager.SUB_TEXT);
        
        val.setFont(new Font("SansSerif", Font.BOLD, 24));
        val.setForeground(accent);
        
        p.add(t, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
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
                else if ("CANCELLED".equalsIgnoreCase(status)) l.setForeground(new Color(148, 163, 184));
                else l.setForeground(new Color(245, 158, 11));
                l.setFont(new Font("SansSerif", Font.BOLD, 13));
                return l;
            }
        };
        t.getColumnModel().getColumn(3).setCellRenderer(statusRenderer);
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
        if (currentStudent == null) return;
        
        int selectedMonth = monthSelector.getSelectedIndex();
        String selectedYearStr = yearSelector.getSelectedItem().toString();
        
        new SwingWorker<Void, Object[]>() {
            int presentCount = 0;
            int absentCount = 0;
            int totalCount = 0;

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    com.mongodb.client.MongoDatabase database = db.DBConnection.getDatabase();
                    if (database == null) return null;

                    com.mongodb.client.MongoCollection<org.bson.Document> attendanceCol = database.getCollection("attendance");
                    com.mongodb.client.MongoCollection<org.bson.Document> batchesCol = database.getCollection("batches");
                    com.mongodb.client.MongoCollection<org.bson.Document> subjectsCol = database.getCollection("subjects");

                    com.mongodb.client.FindIterable<org.bson.Document> list = attendanceCol.find(
                        com.mongodb.client.model.Filters.eq("user_id", currentStudent.getUserId())
                    ).sort(new org.bson.Document("date", -1));
                    
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy hh:mm a");
                    java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("EEEE");

                    for (org.bson.Document doc : list) {
                        java.util.Date date = doc.getDate("date");
                        if (date == null) date = doc.getDate("attendance_date");
                        if (date == null) continue;
                        
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTime(date);
                        int attMonth = cal.get(java.util.Calendar.MONTH) + 1;
                        int attYear = cal.get(java.util.Calendar.YEAR);
                        
                        if (selectedMonth > 0 && attMonth != selectedMonth) continue;
                        if (!"All Years".equals(selectedYearStr) && attYear != Integer.parseInt(selectedYearStr)) continue;
                        
                        String status = doc.getString("status");
                        if ("PRESENT".equalsIgnoreCase(status)) presentCount++;
                        else if ("ABSENT".equalsIgnoreCase(status)) absentCount++;
                        
                        if (!"CANCELLED".equalsIgnoreCase(status)) totalCount++;
                        
                        String batchLabel = "Unknown Batch";
                        Object batchIdObj = doc.get("batch_id");
                        if (batchIdObj != null) {
                            org.bson.Document b = batchesCol.find(new org.bson.Document("_id", batchIdObj)).first();
                            if (b != null) {
                                org.bson.Document s = subjectsCol.find(new org.bson.Document("_id", b.get("subject_id"))).first();
                                String sName = s != null ? s.getString("subject_name") : "";
                                batchLabel = sName + " - " + b.getString("batch_name");
                            }
                        }
                        
                        String markedBy = doc.getString("marked_by");
                        if (markedBy == null) markedBy = doc.getString("teacher_id");
                        
                        publish(new Object[]{
                            sdf.format(date),
                            dayFormat.format(date),
                            batchLabel,
                            status != null ? status.toUpperCase() : "—",
                            markedBy != null ? markedBy : "—",
                            doc.getString("remarks") != null ? doc.getString("remarks") : "—"
                        });
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                for (Object[] row : chunks) model.addRow(row);
            }

            @Override
            protected void done() {
                totalClassesLbl.setText(String.valueOf(totalCount));
                presentLbl.setText(String.valueOf(presentCount));
                absentLbl.setText(String.valueOf(absentCount));
                
                double percent = totalCount == 0 ? 0 : (double) presentCount / totalCount * 100;
                monthlyPercentLbl.setText(String.format("%.1f%%", percent));
                monthlyPercentLbl.setForeground(percent >= 75 ? new Color(34, 197, 94) : new Color(239, 68, 68));
                
                attProgressBar.setValue((int)percent);
                attProgressBar.setForeground(percent >= 75 ? new Color(34, 197, 94) : new Color(239, 68, 68));
            }
        }.execute();
    }
}
