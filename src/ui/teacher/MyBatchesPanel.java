package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import model.User;
import model.Batch;
import service.TeacherPortalService;
import util.SessionManager;

public class MyBatchesPanel extends JPanel {

    private JTable batchTable;
    private DefaultTableModel model;

    public MyBatchesPanel(User user) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("My Assigned Batches");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        String columns[] = {"Batch Name", "Subject", "Schedule", "Students", "Mode", "Status"};
        model = new DefaultTableModel(columns, 0);
        batchTable = new JTable(model);
        batchTable.setRowHeight(40);
        batchTable.setIntercellSpacing(new Dimension(0, 0));
        batchTable.setShowGrid(false);
        batchTable.setShowHorizontalLines(true);
        batchTable.setGridColor(new Color(230, 230, 230));

        JScrollPane scrollPane = new JScrollPane(batchTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        refreshTable();
    }

    private void refreshTable() {
        model.setRowCount(0);
        model.addRow(new Object[]{"Loading...", "", "", "", "", ""});

        new SwingWorker<List<TeacherPortalService.BatchRow>, Void>() {
            @Override protected List<TeacherPortalService.BatchRow> doInBackground() {
                return new TeacherPortalService().getTeacherBatchRows(SessionManager.getCurrentTeacherId());
            }

            @Override protected void done() {
                try {
                    List<TeacherPortalService.BatchRow> rows = get();
                    model.setRowCount(0);
                    if (rows.isEmpty()) {
                        model.addRow(new Object[]{"No batches assigned", "-", "-", "0", "-", "-"});
                        return;
                    }
                    for (TeacherPortalService.BatchRow row : rows) {
                        Batch b = row.batch;
                        model.addRow(new Object[]{
                            safe(b.getBatchName()),
                            safe(row.subjectName),
                            formatSchedule(b),
                            row.studentCount,
                            safe(b.getClassMode()),
                            safe(b.getStatus())
                        });
                    }
                } catch (Exception e) {
                    model.setRowCount(0);
                    JOptionPane.showMessageDialog(MyBatchesPanel.this, "Failed to load batches: " + e.getMessage());
                }
            }
        }.execute();
    }

    private String formatSchedule(Batch batch) {
        if (batch.getSchedules() == null || batch.getSchedules().isEmpty()) {
            return batch.getTiming() != null ? batch.getTiming() : "-";
        }
        StringBuilder sb = new StringBuilder();
        for (model.Schedule schedule : batch.getSchedules()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(safe(schedule.getDay())).append(" ")
              .append(safe(schedule.getStart())).append("-")
              .append(safe(schedule.getEnd()));
        }
        return sb.toString();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
