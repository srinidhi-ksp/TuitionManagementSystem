package ui.student;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import util.SessionManager;

public class MyBatchesPanel extends JPanel {

    private JTable batchTable;
    private DefaultTableModel model;

    public MyBatchesPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("My Enrolled Batches");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadBatchesAsync());
        
        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String columns[] = {"Batch Name", "Subject", "Teacher", "Schedule", "Mode", "Status"};
        model = new DefaultTableModel(columns, 0);
        batchTable = new JTable(model);
        batchTable.setRowHeight(45);
        batchTable.setIntercellSpacing(new Dimension(0, 0));
        batchTable.setShowGrid(false);
        batchTable.setShowHorizontalLines(true);
        batchTable.setGridColor(new Color(235, 235, 235));
        batchTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));

        loadBatchesAsync();

        JScrollPane scrollPane = new JScrollPane(batchTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadBatchesAsync() {
        model.setRowCount(0);
        
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                String userIdFromSession = SessionManager.getInstance().getUserId();
                String userEmail = SessionManager.getInstance().getUserEmail();
                List<Object[]> rows = new java.util.ArrayList<>();
                
                System.out.println("\n[MyBatchesPanel] 🔄 Starting batch load...");
                System.out.println("[MyBatchesPanel] User ID: " + userIdFromSession);
                System.out.println("[MyBatchesPanel] User Email: " + userEmail);
                
                if (userIdFromSession == null) {
                    System.err.println("[MyBatchesPanel] ❌ User ID is null!");
                    return rows;
                }

                // Step 1: Map User → Student
                dao.StudentDAO studentDao = new dao.StudentDAO();
                model.Student student = studentDao.getStudentByUserId(userIdFromSession);
                
                if (student == null && userEmail != null) {
                    System.out.println("[MyBatchesPanel] First lookup failed, trying by email...");
                    student = studentDao.getStudentByEmail(userEmail);
                }

                if (student == null) {
                    System.err.println("[MyBatchesPanel] ❌ Mapped Student: NULL - Cannot proceed");
                    return rows;
                }

                String studentId = student.getUserId(); // Maps to _id like S001
                System.out.println("[MyBatchesPanel] ✅ Mapped Student ID: " + studentId);

                // Step 2: Fetch Enrollments correctly
                dao.EnrollmentDAO enrollmentDao = new dao.EnrollmentDAO();
                List<model.Enrollment> enrollments = enrollmentDao.getEnrollmentsByStudentId(studentId);
                System.out.println("[MyBatchesPanel] 📊 Enrollments Found: " + enrollments.size());

                if (enrollments.isEmpty()) {
                    System.out.println("[MyBatchesPanel] ⚠️  No active enrollments for student: " + studentId);
                    return rows;
                }

                dao.BatchDAO batchDao = new dao.BatchDAO();
                dao.SubjectDAO subDao = new dao.SubjectDAO();
                dao.TeacherDAO teacherDao = new dao.TeacherDAO();

                int enrollmentIndex = 0;
                for (model.Enrollment e : enrollments) {
                    enrollmentIndex++;
                    System.out.println("[MyBatchesPanel] Processing enrollment #" + enrollmentIndex + 
                                     " (Batch ID: " + e.getBatchId() + ", Status: " + e.getStatus() + ")");
                    
                    model.Batch b = batchDao.getBatchById(e.getBatchId());
                    if (b != null) {
                        model.Subject s = subDao.getSubjectById(b.getSubjectId());
                        model.Teacher t = teacherDao.getTeacherById(b.getTeacherUserId());
                        
                        String subName = (s != null) ? s.getSubjectName() : "Unknown";
                        String teacherName = (t != null) ? t.getName() : b.getTeacherUserId();
                        String schedule = b.getStartTime() != null ? b.getStartTime().toString().substring(11,16) + " - " + b.getEndTime().toString().substring(11,16) : "";

                        Object[] row = {
                            b.getBatchName(),
                            subName,
                            teacherName,
                            schedule,
                            b.getClassMode(),
                            e.getStatus()
                        };
                        rows.add(row);
                        System.out.println("[MyBatchesPanel]   ✅ Added: " + b.getBatchName() + " (" + subName + ")");
                    } else {
                        System.err.println("[MyBatchesPanel]   ❌ Batch not found: ID " + e.getBatchId());
                    }
                }
                
                System.out.println("[MyBatchesPanel] ✅ Total rows to display: " + rows.size());
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    model.setRowCount(0);
                    if (!rows.isEmpty()) {
                        System.out.println("[MyBatchesPanel] Populating table with " + rows.size() + " batches");
                        for (Object[] row : rows) {
                            model.addRow(row);
                        }
                    } else {
                        System.out.println("[MyBatchesPanel] ⚠️  No data to display");
                        model.addRow(new Object[]{"No Data Available", "", "", "", "", ""});
                    }
                } catch (Exception e) {
                    System.err.println("[MyBatchesPanel] ❌ Error in loadBatchesAsync done(): " + e.getMessage());
                    e.printStackTrace();
                    model.addRow(new Object[]{"Error loading data", "", "", "", "", ""});
                }
            }
        }.execute();
    }
}
