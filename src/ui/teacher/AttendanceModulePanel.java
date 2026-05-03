package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.Batch;
import model.Student;
import service.TeacherPortalService;
import util.SessionManager;

public class AttendanceModulePanel extends JPanel {

    private static final String[] STATUS_OPTIONS = {"Present", "Absent", "Late", "Cancelled"};

    private JComboBox<String> batchSelector;
    private JTextField dateField;
    private JTable studentTable;
    private DefaultTableModel model;
    private List<Batch> myBatches = new ArrayList<>();
    private List<Student> currentStudents;
    private JButton loadBtn;
    private JButton saveBtn;
    private TeacherPortalService service = new TeacherPortalService();

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

        batchSelector = new JComboBox<>();
        batchSelector.addItem("Loading batches...");

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

        String columns[] = {"Student ID", "Student Name", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 2; }
        };

        studentTable = new JTable(model);
        studentTable.setRowHeight(40);
        studentTable.setIntercellSpacing(new Dimension(0, 0));
        studentTable.setShowGrid(false);
        studentTable.setShowHorizontalLines(true);
        studentTable.setGridColor(new Color(230, 230, 230));
        studentTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(STATUS_OPTIONS)));
        studentTable.getColumnModel().getColumn(2).setCellRenderer(new StatusRenderer());
        studentTable.getModel().addTableModelListener(e -> studentTable.repaint());

        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(20, 0, 0, 0));

        JButton cancelAllBtn = new JButton("Cancel Entire Class");
        cancelAllBtn.addActionListener(e -> {
            for (int i = 0; i < model.getRowCount(); i++) model.setValueAt("Cancelled", i, 2);
        });

        saveBtn = new JButton("Save Attendance");
        saveBtn.setPreferredSize(new Dimension(160, 40));
        saveBtn.setBackground(new Color(30, 190, 160));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.addActionListener(e -> saveAttendanceAsync());

        footer.add(cancelAllBtn);
        footer.add(saveBtn);
        add(footer, BorderLayout.SOUTH);

        loadBatchesAsync();
    }

    private void loadBatchesAsync() {
        loadBtn.setEnabled(false);
        new SwingWorker<List<Batch>, Void>() {
            @Override protected List<Batch> doInBackground() {
                return service.getTeacherBatches(SessionManager.getCurrentTeacherId());
            }

            @Override protected void done() {
                try {
                    myBatches = get();
                    batchSelector.removeAllItems();
                    batchSelector.addItem("-- Select Batch --");
                    for (Batch batch : myBatches) batchSelector.addItem(batch.getBatchName());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AttendanceModulePanel.this, "Failed to load batches: " + e.getMessage());
                } finally {
                    loadBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void loadStudentRosterAsync() {
        if(batchSelector.getSelectedIndex() <= 0) return;

        Batch selectedBatch = myBatches.get(batchSelector.getSelectedIndex() - 1);
        String dateText = dateField.getText().trim();
        try {
            parseDate(dateText);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Date must be in YYYY-MM-DD format.");
            return;
        }

        model.setRowCount(0);
        model.addRow(new Object[]{"Loading...", "Loading roster...", "Present"});
        loadBtn.setEnabled(false);

        new SwingWorker<RosterData, Void>() {
            @Override protected RosterData doInBackground() {
                RosterData data = new RosterData();
                String teacherId = SessionManager.getCurrentTeacherId();
                data.students = service.getStudentsForTeacherBatch(teacherId, selectedBatch.getBatchId());
                data.statuses = service.getAttendanceStatusMap(teacherId, selectedBatch.getBatchId(), dateText);
                return data;
            }

            @Override protected void done() {
                try {
                    RosterData data = get();
                    currentStudents = data.students;
                    model.setRowCount(0);
                    if (currentStudents != null && !currentStudents.isEmpty()) {
                        for(Student student : currentStudents) {
                            String status = data.statuses.getOrDefault(student.getUserId(), "Present");
                            model.addRow(new Object[]{ student.getUserId(), student.getName(), status });
                        }
                    } else {
                        model.addRow(new Object[]{"N/A", "No active students in this batch", "Present"});
                    }
                } catch (Exception e) {
                    model.setRowCount(0);
                    JOptionPane.showMessageDialog(AttendanceModulePanel.this, "Failed to load students: " + e.getMessage());
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

        if (studentTable.isEditing()) studentTable.getCellEditor().stopCellEditing();

        final String dateText = dateField.getText().trim();
        Date attendanceDate;
        try {
            attendanceDate = parseDate(dateText);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Date must be in YYYY-MM-DD format.");
            return;
        }
        if (attendanceDate.after(startOfToday())) {
            JOptionPane.showMessageDialog(this, "Cannot mark attendance for a future date.");
            return;
        }

        final int batchId = myBatches.get(batchSelector.getSelectedIndex() - 1).getBatchId();
        final String teacherId = SessionManager.getCurrentTeacherId();
        final Map<String, String> statuses = new LinkedHashMap<>();
        for(int i = 0; i < studentTable.getRowCount(); i++) {
            statuses.put(String.valueOf(model.getValueAt(i, 0)), String.valueOf(model.getValueAt(i, 2)));
        }

        saveBtn.setEnabled(false);
        saveBtn.setText("Checking...");

        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return service.hasAttendance(teacherId, batchId, dateText);
            }

            @Override protected void done() {
                try {
                    boolean exists = get();
                    if (exists) {
                        int choice = JOptionPane.showConfirmDialog(AttendanceModulePanel.this,
                                "Attendance for " + dateText + " already exists. Overwrite?",
                                "Confirm Overwrite",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (choice != JOptionPane.YES_OPTION) {
                            saveBtn.setEnabled(true);
                            saveBtn.setText("Save Attendance");
                            return;
                        }
                    }
                    saveAttendanceWorker(teacherId, batchId, dateText, statuses);
                } catch(Exception e) {
                    saveBtn.setEnabled(true);
                    saveBtn.setText("Save Attendance");
                    JOptionPane.showMessageDialog(AttendanceModulePanel.this, "Error checking attendance: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void saveAttendanceWorker(String teacherId, int batchId, String dateText, Map<String, String> statuses) {
        saveBtn.setText("Saving...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() {
                return service.saveAttendance(teacherId, batchId, dateText, statuses);
            }

            @Override protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(AttendanceModulePanel.this, "Attendance saved successfully.");
                    } else {
                        JOptionPane.showMessageDialog(AttendanceModulePanel.this, "Error saving attendance.");
                    }
                } catch(Exception e) {
                    JOptionPane.showMessageDialog(AttendanceModulePanel.this, "Error saving attendance: " + e.getMessage());
                } finally {
                    saveBtn.setEnabled(true);
                    saveBtn.setText("Save Attendance");
                }
            }
        }.execute();
    }

    private Date parseDate(String text) throws Exception {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        fmt.setLenient(false);
        return fmt.parse(text);
    }

    private Date startOfToday() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static class RosterData {
        List<Student> students = new ArrayList<>();
        Map<String, String> statuses = new LinkedHashMap<>();
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            String status = value != null ? value.toString() : "Present";
            if (!selected) {
                if ("Absent".equalsIgnoreCase(status)) label.setBackground(new Color(254, 242, 242));
                else if ("Late".equalsIgnoreCase(status)) label.setBackground(new Color(255, 251, 235));
                else if ("Cancelled".equalsIgnoreCase(status)) label.setBackground(new Color(243, 244, 246));
                else label.setBackground(new Color(240, 253, 244));
            }
            label.setToolTipText("Cancelled".equalsIgnoreCase(status) ? "Class cancelled" : null);
            return label;
        }
    }
}
