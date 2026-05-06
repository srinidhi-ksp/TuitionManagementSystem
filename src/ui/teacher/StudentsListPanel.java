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
    private JComboBox<String> batchFilter;
    private java.util.List<model.Batch> teacherBatches;

    public StudentsListPanel(User user) {
        setLayout(new BorderLayout());
        setBackground(new Color(248, 249, 250));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("My Enrolled Students");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(13, 27, 42));

        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        filterPanel.setOpaque(false);

        JLabel filterLabel = new JLabel("Select Batch:");
        filterLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        batchFilter = new JComboBox<>();
        batchFilter.setPreferredSize(new Dimension(200, 35));
        batchFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        batchFilter.addItem("All Batches");
        
        loadBatches();

        batchFilter.addActionListener(e -> refreshTable());

        JButton refreshBtn = new JButton("⟳ Refresh");
        refreshBtn.setBackground(new Color(74, 144, 226));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshBtn.setPreferredSize(new Dimension(120, 35));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> refreshTable());

        filterPanel.add(filterLabel);
        filterPanel.add(batchFilter);
        filterPanel.add(refreshBtn);

        header.add(title, BorderLayout.WEST);
        header.add(filterPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        String columns[] = {"Student ID", "Student Name", "Enrollment Date", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        studentTable = new JTable(model);
        studentTable.setRowHeight(45);
        studentTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        studentTable.setIntercellSpacing(new Dimension(0, 0));
        studentTable.setShowGrid(false);
        studentTable.setShowHorizontalLines(true);
        studentTable.setGridColor(new Color(230, 230, 230));
        
        studentTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        studentTable.getTableHeader().setBackground(new Color(245, 247, 250));
        studentTable.getTableHeader().setPreferredSize(new Dimension(0, 40));

        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        refreshTable();
    }

    private void loadBatches() {
        new SwingWorker<java.util.List<model.Batch>, Void>() {
            @Override protected java.util.List<model.Batch> doInBackground() {
                return new TeacherPortalService().getTeacherBatches(SessionManager.getCurrentTeacherId());
            }
            @Override protected void done() {
                try {
                    teacherBatches = get();
                    for (model.Batch b : teacherBatches) {
                        batchFilter.addItem(b.getBatchName() + " (" + b.getBatchId() + ")");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private void refreshTable() {
        model.setRowCount(0);
        model.addRow(new Object[]{"Loading...", "", "", ""});

        int selectedIdx = batchFilter.getSelectedIndex();
        int batchId = -1;
        if (selectedIdx > 0 && teacherBatches != null) {
            batchId = teacherBatches.get(selectedIdx - 1).getBatchId();
        }

        final int finalBatchId = batchId;

        new SwingWorker<List<TeacherPortalService.StudentRow>, Void>() {
            @Override protected List<TeacherPortalService.StudentRow> doInBackground() {
                return new TeacherPortalService().getTeacherStudentsByBatch(SessionManager.getCurrentTeacherId(), finalBatchId);
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
