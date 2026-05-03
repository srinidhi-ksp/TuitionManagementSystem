package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

import model.User;
import service.TeacherPortalService;
import util.SessionManager;

public class StudentsListPanel extends JPanel {

    private JTable studentTable;
    private DefaultTableModel model;

    public StudentsListPanel(User user) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("My Enrolled Students");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        String columns[] = {"Student ID", "Student Name", "Enrollment Date", "Status"};
        model = new DefaultTableModel(columns, 0);
        studentTable = new JTable(model);
        studentTable.setRowHeight(40);
        studentTable.setIntercellSpacing(new Dimension(0, 0));
        studentTable.setShowGrid(false);
        studentTable.setShowHorizontalLines(true);
        studentTable.setGridColor(new Color(230, 230, 230));

        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        refreshTable();
    }

    private void refreshTable() {
        model.setRowCount(0);
        model.addRow(new Object[]{"Loading...", "", "", ""});

        new SwingWorker<List<TeacherPortalService.StudentRow>, Void>() {
            @Override protected List<TeacherPortalService.StudentRow> doInBackground() {
                return new TeacherPortalService().getTeacherStudents(SessionManager.getCurrentTeacherId());
            }

            @Override protected void done() {
                try {
                    List<TeacherPortalService.StudentRow> rows = get();
                    model.setRowCount(0);
                    if (rows.isEmpty()) {
                        model.addRow(new Object[]{"No active students", "-", "-", "-"});
                        return;
                    }
                    SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy");
                    for (TeacherPortalService.StudentRow row : rows) {
                        model.addRow(new Object[]{
                            row.student.getUserId(),
                            row.student.getName(),
                            row.enrollmentDate != null ? fmt.format(row.enrollmentDate) : "-",
                            row.status
                        });
                    }
                } catch (Exception e) {
                    model.setRowCount(0);
                    JOptionPane.showMessageDialog(StudentsListPanel.this, "Failed to load students: " + e.getMessage());
                }
            }
        }.execute();
    }
}
