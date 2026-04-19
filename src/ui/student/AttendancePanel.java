package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import model.User;
import model.Attendance;
import dao.AttendanceDAO;

public class AttendancePanel extends JPanel {

    private User student;
    private JTable attTable;
    private DefaultTableModel model;

    public AttendancePanel(User user) {
        this.student = user;
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

        refreshTable();

        JScrollPane scrollPane = new JScrollPane(attTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void refreshTable() {
        model.setRowCount(0);
        List<Attendance> allAtt = new AttendanceDAO().getAllAttendance();
        
        if (allAtt != null) {
            for (Attendance a : allAtt) {
                // Filter specifically for this student
                if (a.getUserId() != null && a.getUserId().equals(student.getUserId())) {
                    String dateStr = a.getAttendanceDate() != null ? a.getAttendanceDate().toString().substring(0, 10) : "";
                    model.addRow(new Object[]{
                        dateStr, a.getStatus(), a.getReason() != null ? a.getReason() : "-", a.getMarkedBy()
                    });
                }
            }
        }
    }
}
