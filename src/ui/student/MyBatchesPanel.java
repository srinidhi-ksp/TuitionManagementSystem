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
        
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        String columns[] = {"Batch Name", "Subject", "Teacher ID", "Schedule", "Mode", "Status"};
        model = new DefaultTableModel(columns, 0);
        batchTable = new JTable(model);
        batchTable.setRowHeight(40);
        batchTable.setIntercellSpacing(new Dimension(0, 0));
        batchTable.setShowGrid(false);
        batchTable.setShowHorizontalLines(true);
        batchTable.setGridColor(new Color(230, 230, 230));

        loadBatchesAsync();

        JScrollPane scrollPane = new JScrollPane(batchTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadBatchesAsync() {
        model.setRowCount(0);
        model.addRow(new Object[]{"Loading...", "", "", "", "", ""});
        
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                String userId = SessionManager.getInstance().getUserId();
                List<Object[]> rows = new java.util.ArrayList<>();
                if (userId == null) return rows;

                List<Batch> enrolled = new EnrollmentDAO().getBatchesByStudentId(userId);
                SubjectDAO subDao = new SubjectDAO();

                if (enrolled != null) {
                    for (Batch b : enrolled) {
                        String subName = "Unknown";
                        if (subDao.getSubjectById(b.getSubjectId()) != null) {
                            subName = subDao.getSubjectById(b.getSubjectId()).getSubjectName();
                        }
                        String start = b.getStartTime() != null ? b.getStartTime().toString().substring(11,16) : "";
                        String end = b.getEndTime() != null ? b.getEndTime().toString().substring(11,16) : "";
                        rows.add(new Object[]{
                            b.getBatchName(), subName, b.getTeacherUserId(), start + " - " + end, b.getClassMode(), "Active"
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
                    model.setRowCount(0);
                    model.addRow(new Object[]{"Error loading data", "", "", "", "", ""});
                }
            }
        }.execute();
    }
}
