package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

import model.User;
import model.Batch;
import model.Student;
import model.Enrollment;
import model.Attendance;
import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.StudentDAO;
import dao.AttendanceDAO;
import java.util.Date;
import java.text.SimpleDateFormat;

public class AttendanceModulePanel extends JPanel {

    private User teacherContext;
    private JComboBox<String> batchSelector;
    private JTextField dateField;
    private JTable studentTable;
    private DefaultTableModel model;
    private List<Batch> myBatches;
    private List<Student> currentStudents;

    public AttendanceModulePanel(User user) {
        this.teacherContext = user;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("Take Class Attendance");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        controlPanel.setBackground(Color.WHITE);
        
        myBatches = new BatchDAO().getBatchesByTeacherId(teacherContext.getUserId());
        batchSelector = new JComboBox<>();
        if(myBatches != null) {
            batchSelector.addItem("-- Select Batch --");
            for(Batch b : myBatches) {
                batchSelector.addItem(b.getBatchName());
            }
        }
        
        dateField = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 10);
        
        JButton loadBtn = new JButton("Load Students");
        loadBtn.setBackground(new Color(245, 245, 250));
        loadBtn.setFocusPainted(false);
        loadBtn.addActionListener(e -> loadStudentRoster());
        
        controlPanel.add(new JLabel("Batch:"));
        controlPanel.add(batchSelector);
        controlPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        controlPanel.add(dateField);
        controlPanel.add(loadBtn);

        header.add(title, BorderLayout.WEST);
        header.add(controlPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String columns[] = {"Student ID", "Student Name", "Is Present?"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) return Boolean.class;
                return super.getColumnClass(columnIndex);
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Only the checkbox is editable
            }
        };
        
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
        
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        JButton saveBtn = new JButton("Save Attendance");
        saveBtn.setPreferredSize(new Dimension(160, 40));
        saveBtn.setBackground(new Color(30, 190, 160));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.addActionListener(e -> saveAttendance());
        
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private void loadStudentRoster() {
        model.setRowCount(0);
        currentStudents = new ArrayList<>();
        
        if(batchSelector.getSelectedIndex() <= 0) return;
        
        Batch selectedBatch = myBatches.get(batchSelector.getSelectedIndex() - 1);
        List<Enrollment> enrollments = new EnrollmentDAO().getAllEnrollments();
        List<Student> allStudents = new StudentDAO().getAllStudents();
        
        for(Enrollment e : enrollments) {
            if(e.getBatchId() == selectedBatch.getBatchId()) {
                for(Student s : allStudents) {
                    if(s.getUserId().equals(e.getStudentUserId())) {
                        currentStudents.add(s);
                        model.addRow(new Object[]{ s.getUserId(), s.getName(), true }); // Default to Present
                        break;
                    }
                }
            }
        }
    }

    private void saveAttendance() {
        if(currentStudents == null || currentStudents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students loaded.");
            return;
        }
        
        if (studentTable.isEditing()) {
            studentTable.getCellEditor().stopCellEditing();
        }

        AttendanceDAO attDao = new AttendanceDAO();
        try {
            Date attDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateField.getText());
            int successCount = 0;
            
            for(int i = 0; i < studentTable.getRowCount(); i++) {
                String sId = (String) model.getValueAt(i, 0);
                boolean isPresent = (Boolean) model.getValueAt(i, 2);
                
                Attendance a = new Attendance();
                a.setAttendanceId((int)(Math.random() * 100000));
                a.setUserId(sId);
                a.setAttendanceDate(attDate);
                a.setStatus(isPresent ? "Present" : "Absent");
                a.setMarkedBy(teacherContext.getUserId());
                a.setReason(isPresent ? "Attended" : "Unexcused");
                
                if(attDao.addAttendance(a)) {
                    successCount++;
                }
            }
            
            JOptionPane.showMessageDialog(this, "Successfully saved " + successCount + " records mapping to DB.");
            
        } catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Invalid Date Format. Use YYYY-MM-DD");
        }
    }
}
