package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import model.User;
import model.Batch;
import model.Student;
import model.Enrollment;
import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.StudentDAO;

public class StudentsListPanel extends JPanel {

    private User teacherContext;
    private JTable studentTable;
    private DefaultTableModel model;

    public StudentsListPanel(User user) {
        this.teacherContext = user;
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

        String columns[] = {"Student ID", "Student Name", "Class Standard", "Board", "City"};
        model = new DefaultTableModel(columns, 0);
        studentTable = new JTable(model);
        studentTable.setRowHeight(40);
        studentTable.setIntercellSpacing(new Dimension(0, 0));
        studentTable.setShowGrid(false);
        studentTable.setShowHorizontalLines(true);
        studentTable.setGridColor(new Color(230, 230, 230));

        refreshTable();

        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void refreshTable() {
        model.setRowCount(0);
        
        // Find teacher batches
        List<Batch> assigned = new BatchDAO().getBatchesByTeacherId(teacherContext.getUserId());
        Set<String> uniqueStudentIds = new HashSet<>();
        
        if (assigned != null) {
            List<Enrollment> allEnrollments = new EnrollmentDAO().getAllEnrollments();
            for (Batch b : assigned) {
                for (Enrollment e : allEnrollments) {
                    if (e.getBatchId() == b.getBatchId()) {
                        uniqueStudentIds.add(e.getStudentUserId());
                    }
                }
            }
        }
        
        if (!uniqueStudentIds.isEmpty()) {
            StudentDAO sDao = new StudentDAO();
            List<Student> allStudents = sDao.getAllStudents(); // Fast local cache scan
            for (Student s : allStudents) {
                if (uniqueStudentIds.contains(s.getUserId())) {
                    model.addRow(new Object[]{
                        s.getUserId(), s.getName(), s.getCurrentStd(), s.getBoard(), s.getCity()
                    });
                }
            }
        }
    }
}
