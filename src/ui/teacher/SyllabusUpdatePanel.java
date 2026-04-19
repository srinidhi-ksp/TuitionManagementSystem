package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import model.User;
import model.Batch;
import model.ChapterProgress;
import dao.BatchDAO;
import dao.SyllabusProgressDAO;

public class SyllabusUpdatePanel extends JPanel {

    private User teacherContext;
    private JPanel content;
    private JComboBox<String> batchSelector;
    private List<Batch> myBatches;

    public SyllabusUpdatePanel(User user) {
        this.teacherContext = user;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("Update Syllabus Progress");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.setBackground(Color.WHITE);
        
        batchSelector = new JComboBox<>();
        myBatches = new BatchDAO().getBatchesByTeacherId(teacherContext.getUserId());
        if (myBatches != null) {
            batchSelector.addItem("-- Select Batch --");
            for(Batch b : myBatches) {
                batchSelector.addItem(b.getBatchName());
            }
        }
        batchSelector.addActionListener(e -> renderChapters());
        
        controlPanel.add(new JLabel("Select Batch: "));
        controlPanel.add(batchSelector);

        header.add(title, BorderLayout.WEST);
        header.add(controlPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void renderChapters() {
        content.removeAll();
        if (batchSelector.getSelectedIndex() <= 0) {
            content.revalidate();
            content.repaint();
            return;
        }

        Batch selectedBatch = myBatches.get(batchSelector.getSelectedIndex() - 1);
        SyllabusProgressDAO progDao = new SyllabusProgressDAO();
        
        List<ChapterProgress> all = progDao.getProgressByBatchId(selectedBatch.getBatchId());
        
        if (all == null || all.isEmpty()) {
            JLabel empty = new JLabel("No chapters assigned to this batch. Verify your DB mappings.");
            empty.setForeground(Color.RED);
            empty.setBorder(new EmptyBorder(20,20,20,20));
            content.add(empty);
            content.revalidate();
            content.repaint();
            return;
        }

        HashMap<String, List<ChapterProgress>> grouped = new HashMap<>();
        for(ChapterProgress cp : all) {
            grouped.putIfAbsent(cp.getSubjectName(), new ArrayList<>());
            grouped.get(cp.getSubjectName()).add(cp);
        }
        
        for (String subName : grouped.keySet()) {
            JPanel subBlock = new JPanel(new BorderLayout());
            subBlock.setBackground(Color.WHITE);
            subBlock.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230,230,230)),
                new EmptyBorder(15,15,15,15)
            ));
            
            JLabel stitle = new JLabel("📚 " + subName);
            stitle.setFont(new Font("Arial", Font.BOLD, 18));
            stitle.setBorder(new EmptyBorder(0,0,10,0));
            subBlock.add(stitle, BorderLayout.NORTH);
            
            JPanel chapList = new JPanel(new GridLayout(0, 1, 0, 15));
            chapList.setBackground(Color.WHITE);
            
            for(ChapterProgress cp : grouped.get(subName)) {
                chapList.add(createEditableChapterRow(cp, progDao));
            }
            
            subBlock.add(chapList, BorderLayout.CENTER);
            content.add(subBlock);
            content.add(Box.createRigidArea(new Dimension(0, 20)));
        }
        
        content.revalidate();
        content.repaint();
    }

    private JPanel createEditableChapterRow(ChapterProgress cp, SyllabusProgressDAO dao) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        
        JLabel name = new JLabel(cp.getChapterName());
        name.setPreferredSize(new Dimension(150, 30));
        
        JPanel mid = new JPanel(new BorderLayout());
        mid.setBackground(Color.WHITE);
        
        JSlider slider = new JSlider(0, 100, cp.getCompletionPercentage());
        slider.setMajorTickSpacing(25);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setBackground(Color.WHITE);
        
        JLabel percVal = new JLabel(cp.getCompletionPercentage() + "%");
        percVal.setFont(new Font("Arial", Font.BOLD, 14));
        percVal.setPreferredSize(new Dimension(50, 30));
        
        slider.addChangeListener(e -> percVal.setText(slider.getValue() + "%"));
        
        mid.add(slider, BorderLayout.CENTER);
        mid.add(percVal, BorderLayout.EAST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setBackground(Color.WHITE);
        
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Not Started", "In Progress", "Completed"});
        statusCombo.setSelectedItem(cp.getStatus());
        
        JButton updateBtn = new JButton("Update DB");
        updateBtn.setBackground(new Color(30, 190, 160));
        updateBtn.setForeground(Color.WHITE);
        updateBtn.setFocusPainted(false);
        updateBtn.addActionListener(e -> {
            boolean success = dao.updateProgress(cp.getProgressId(), slider.getValue(), (String)statusCombo.getSelectedItem());
            if (success) {
                JOptionPane.showMessageDialog(this, "Progress Synced to Database.");
            } else {
                JOptionPane.showMessageDialog(this, "Progress Update Failed.");
            }
        });
        
        right.add(statusCombo);
        right.add(updateBtn);
        
        row.add(name, BorderLayout.WEST);
        row.add(mid, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        
        return row;
    }
}
