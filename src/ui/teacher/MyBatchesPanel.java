package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import model.User;
import model.Batch;
import dao.BatchDAO;
import dao.SubjectDAO;

public class MyBatchesPanel extends JPanel {

    private User teacherContext;
    private JTable batchTable;
    private DefaultTableModel model;

    public MyBatchesPanel(User user) {
        this.teacherContext = user;
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

        String columns[] = {"Batch Name", "Subject", "Schedule", "Mode", "Status"};
        model = new DefaultTableModel(columns, 0);
        batchTable = new JTable(model);
        batchTable.setRowHeight(40);
        batchTable.setIntercellSpacing(new Dimension(0, 0));
        batchTable.setShowGrid(false);
        batchTable.setShowHorizontalLines(true);
        batchTable.setGridColor(new Color(230, 230, 230));

        refreshTable();

        JScrollPane scrollPane = new JScrollPane(batchTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230,230,230)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void refreshTable() {
        model.setRowCount(0);
        List<Batch> assigned = new BatchDAO().getBatchesByTeacherId(teacherContext.getUserId());
        SubjectDAO subDao = new SubjectDAO();

        if (assigned != null) {
            for (Batch b : assigned) {
                String subName = "Unknown";
                if (subDao.getSubjectById(b.getSubjectId()) != null) {
                    subName = subDao.getSubjectById(b.getSubjectId()).getSubjectName();
                }
                String start = b.getStartTime() != null ? b.getStartTime().toString().substring(11,16) : "";
                String end = b.getEndTime() != null ? b.getEndTime().toString().substring(11,16) : "";
                
                model.addRow(new Object[]{
                    b.getBatchName(), subName, start + " - " + end, b.getClassMode(), "Active"
                });
            }
        }
    }
}
