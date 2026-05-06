package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import model.Attendance;
import dao.AttendanceDAO;
import util.SessionManager;

public class AttendancePanel extends JPanel {

    private DefaultTableModel model;
    private JTable attTable;

    public AttendancePanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("My Attendance");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        String columns[] = {"Date", "Status", "Reason", "Marked By"};
        model = new DefaultTableModel(columns, 0);
        attTable = new JTable(model);
        attTable.setRowHeight(40);
        attTable.setIntercellSpacing(new Dimension(0, 0));
        attTable.setShowGrid(false);
        attTable.setShowHorizontalLines(true);
        attTable.setGridColor(new Color(230, 230, 230));

        loadAttendanceData();

        JScrollPane scrollPane = new JScrollPane(attTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
        
        attTable.getColumnModel().getColumn(1).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                if ("PRESENT".equalsIgnoreCase(status)) l.setForeground(new Color(34, 197, 94));
                else if ("ABSENT".equalsIgnoreCase(status)) l.setForeground(new Color(239, 68, 68));
                else if ("CANCELLED".equalsIgnoreCase(status)) l.setForeground(new Color(148, 163, 184));
                else l.setForeground(new Color(245, 158, 11));
                l.setFont(new Font("SansSerif", Font.BOLD, 13));
                return l;
            }
        });
    }

    private void loadAttendanceData() {
        model.setRowCount(0);
        model.addRow(new Object[]{"Loading...", "", "", ""});
        
        new SwingWorker<List<Attendance>, Void>() {
            @Override
            protected List<Attendance> doInBackground() throws Exception {
                String userId = SessionManager.getInstance().getUserId();
                if (userId == null) return new java.util.ArrayList<>();
                return new AttendanceDAO().getAttendanceByStudentId(userId);
            }

            @Override
            protected void done() {
                try {
                    List<Attendance> attendances = get();
                    model.setRowCount(0);
                    if (attendances != null && !attendances.isEmpty()) {
                        for (Attendance a : attendances) {
                            String dateStr = a.getAttendanceDate() != null ? a.getAttendanceDate().toString().substring(0, 10) : "";
                            model.addRow(new Object[]{
                                dateStr, a.getStatus(), a.getReason() != null ? a.getReason() : "-", a.getMarkedBy()
                            });
                        }
                    } else {
                        model.addRow(new Object[]{"No Data Available", "", "", ""});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    model.setRowCount(0);
                    model.addRow(new Object[]{"Error loading data", "", "", ""});
                }
            }
        }.execute();
    }
}
