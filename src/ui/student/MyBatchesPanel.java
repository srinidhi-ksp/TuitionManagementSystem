package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import model.Batch;
import dao.EnrollmentDAO;
import dao.SubjectDAO;
import util.SessionManager;
import ui.admin.TableActionCellRender;
import ui.admin.TableActionCellEditor;
import ui.admin.TableActionEvent;

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
                String userId = SessionManager.getInstance().getUserId();
                String userEmail = SessionManager.getInstance().getUserEmail();
                List<Object[]> rows = new java.util.ArrayList<>();
                
                System.out.println("--- Debug: Student Batch Loading ---");
                System.out.println("User ID: " + userId);
                
                if (userId == null) return rows;

                // Step 1: Map User -> Student
                dao.StudentDAO studentDao = new dao.StudentDAO();
                model.Student student = studentDao.getStudentByUserId(userId);
                if (student == null && userEmail != null) {
                    student = studentDao.getStudentByEmail(userEmail);
                }

                if (student == null) {
                    System.out.println("Mapped Student: NULL");
                    return rows;
                }

                String studentId = student.getUserId(); // Maps to _id like S001
                System.out.println("Mapped Student ID: " + studentId);

                // Step 2: Fetch Enrollments correctly
                dao.EnrollmentDAO enrollmentDao = new dao.EnrollmentDAO();
                List<model.Enrollment> enrollments = enrollmentDao.getEnrollmentsByStudentId(studentId);
                System.out.println("Enrollments Found: " + enrollments.size());

                dao.BatchDAO batchDao = new dao.BatchDAO();
                dao.SubjectDAO subDao = new dao.SubjectDAO();
                dao.TeacherDAO teacherDao = new dao.TeacherDAO();

                for (model.Enrollment e : enrollments) {
                    model.Batch b = batchDao.getBatchById(e.getBatchId());
                    if (b != null) {
                        model.Subject s = subDao.getSubjectById(b.getSubjectId());
                        model.Teacher t = teacherDao.getTeacherById(b.getTeacherUserId());
                        
                        String subName = (s != null) ? s.getSubjectName() : "Unknown";
                        String teacherName = (t != null) ? t.getName() : b.getTeacherUserId();
                        String schedule = b.getStartTime() != null ? b.getStartTime().toString().substring(11,16) + " - " + b.getEndTime().toString().substring(11,16) : "";

                        rows.add(new Object[]{
                            b.getBatchName(),
                            subName,
                            teacherName,
                            schedule,
                            b.getClassMode(),
                            e.getStatus()
                        });
                    }
                }
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    model.setRowCount(0);
                    if (!rows.isEmpty()) {
                        for (Object[] row : rows) {
                            model.addRow(row);
                        }
                    } else {
                        model.addRow(new Object[]{"No Data Available", "", "", "", "", ""});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    model.addRow(new Object[]{"Error loading data", "", "", "", "", ""});
                }
            }
        }.execute();
    }
}
