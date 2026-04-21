package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

import model.Batch;
import model.Student;
import model.Attendance;
import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.StudentDAO;
import dao.AttendanceDAO;
import util.SessionManager;
import java.util.Date;
import java.text.SimpleDateFormat;

public class AttendanceModulePanel extends JPanel {

    private JComboBox<String> batchSelector;
    private JTextField dateField;
    private JTable studentTable;
    private DefaultTableModel model;
    private List<Batch> myBatches;
    private List<Student> currentStudents;
    private JButton loadBtn;
    private JButton saveBtn;

    public AttendanceModulePanel() {
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
        
        String userId = SessionManager.getInstance().getUserId();
        myBatches = userId != null ? new BatchDAO().getBatchesByTeacherId(userId) : new ArrayList<>();
        batchSelector = new JComboBox<>();
        if(myBatches != null) {
            batchSelector.addItem("-- Select Batch --");
            for(Batch b : myBatches) {
                batchSelector.addItem(b.getBatchName());
            }
        }
        
        dateField = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 10);
        
        loadBtn = new JButton("Load Students");
        loadBtn.setBackground(new Color(245, 245, 250));
        loadBtn.setFocusPainted(false);
        loadBtn.addActionListener(e -> loadStudentRosterAsync());
        
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
        
        saveBtn = new JButton("Save Attendance");
        saveBtn.setPreferredSize(new Dimension(160, 40));
        saveBtn.setBackground(new Color(30, 190, 160));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.addActionListener(e -> saveAttendanceAsync());
        
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);
    }

    private void loadStudentRosterAsync() {
        if(batchSelector.getSelectedIndex() <= 0) return;
        
        Batch selectedBatch = myBatches.get(batchSelector.getSelectedIndex() - 1);
        model.setRowCount(0);
        model.addRow(new Object[]{"Loading...", "Loading roster...", false});
        loadBtn.setEnabled(false);

        new SwingWorker<List<Student>, Void>() {
            @Override
            protected List<Student> doInBackground() throws Exception {
                List<String> studentIds = new EnrollmentDAO().getStudentIdsByBatchId(selectedBatch.getBatchId());
                if (studentIds == null || studentIds.isEmpty()) return new ArrayList<>();
                return new StudentDAO().getStudentsByIds(studentIds);
            }

            @Override
            protected void done() {
                try {
                    currentStudents = get();
                    model.setRowCount(0);
                    if (currentStudents != null && !currentStudents.isEmpty()) {
                        for(Student s : currentStudents) {
                            model.addRow(new Object[]{ s.getUserId(), s.getName(), true }); // Default to Present
                        }
                    } else {
                        model.addRow(new Object[]{"N/A", "No students in this batch", false});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    model.setRowCount(0);
                    model.addRow(new Object[]{"Error", "Failed to load students", false});
                } finally {
                    loadBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void saveAttendanceAsync() {
        if(currentStudents == null || currentStudents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students loaded.");
            return;
        }
        
        if (studentTable.isEditing()) {
            studentTable.getCellEditor().stopCellEditing();
        }

        saveBtn.setEnabled(false);
        saveBtn.setText("Saving...");

        // Capture table data before threading
        final List<Object[]> rowData = new ArrayList<>();
        for(int i = 0; i < studentTable.getRowCount(); i++) {
            rowData.add(new Object[]{
                model.getValueAt(i, 0),
                model.getValueAt(i, 2)
            });
        }
        
        final String dateText = dateField.getText();

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                AttendanceDAO attDao = new AttendanceDAO();
                Date attDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateText);
                int successCount = 0;
                String teacherId = SessionManager.getInstance().getUserId();
                
                for(Object[] row : rowData) {
                    String sId = (String) row[0];
                    boolean isPresent = (Boolean) row[1];
                    
                    Attendance a = new Attendance();
                    a.setAttendanceId((int)(Math.random() * 100000));
                    a.setUserId(sId);
                    a.setAttendanceDate(attDate);
                    a.setStatus(isPresent ? "Present" : "Absent");
                    a.setMarkedBy(teacherId);
                    a.setReason(isPresent ? "Attended" : "Unexcused");
                    
                    if(attDao.addAttendance(a)) {
                        successCount++;
                    }
                }
                return successCount;
            }

            @Override
            protected void done() {
                try {
                    int successCount = get();
                    JOptionPane.showMessageDialog(AttendanceModulePanel.this, "Successfully saved " + successCount + " records to DB.");
                } catch(Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(AttendanceModulePanel.this, "Error saving attendance. Ensure Date is YYYY-MM-DD.");
                } finally {
                    saveBtn.setEnabled(true);
                    saveBtn.setText("Save Attendance");
                }
            }
        }.execute();
    }
}
