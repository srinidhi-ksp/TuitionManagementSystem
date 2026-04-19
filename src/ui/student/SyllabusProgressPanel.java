package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import model.User;
import model.ChapterProgress;
import dao.SyllabusProgressDAO;

public class SyllabusProgressPanel extends JPanel {

    private User student;

    public SyllabusProgressPanel(User user) {
        this.student = user;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("Syllabus Tracker");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);

        List<ChapterProgress> progressList = new SyllabusProgressDAO().getProgressForStudent(student.getUserId());
        
        if(progressList == null || progressList.isEmpty()) {
            JLabel empty = new JLabel("No syllabus progress data available. (Mock/Database tables may be empty)");
            empty.setForeground(Color.GRAY);
            content.add(empty);
        } else {
            // Group by Subject
            HashMap<String, List<ChapterProgress>> grouped = new HashMap<>();
            for(ChapterProgress cp : progressList) {
                grouped.putIfAbsent(cp.getSubjectName(), new ArrayList<>());
                grouped.get(cp.getSubjectName()).add(cp);
            }
            
            for (String subName : grouped.keySet()) {
                JPanel subBlock = new JPanel(new BorderLayout());
                subBlock.setBackground(Color.WHITE);
                subBlock.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(230,230,230)),
                    new EmptyBorder(10,10,10,10)
                ));
                
                JLabel stitle = new JLabel("📚 " + subName);
                stitle.setFont(new Font("Arial", Font.BOLD, 16));
                subBlock.add(stitle, BorderLayout.NORTH);
                
                JPanel chapList = new JPanel(new GridLayout(0, 1, 0, 10));
                chapList.setBackground(Color.WHITE);
                chapList.setBorder(new EmptyBorder(10,0,0,0));
                
                for(ChapterProgress cp : grouped.get(subName)) {
                    chapList.add(createChapterRow(cp));
                }
                
                subBlock.add(chapList, BorderLayout.CENTER);
                content.add(subBlock);
                content.add(Box.createRigidArea(new Dimension(0, 20)));
            }
        }

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createChapterRow(ChapterProgress cp) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        
        JLabel name = new JLabel(cp.getChapterName());
        name.setPreferredSize(new Dimension(200, 20));
        
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(cp.getCompletionPercentage());
        bar.setStringPainted(true);
        bar.setPreferredSize(new Dimension(300, 20));
        bar.setForeground(new Color(30, 190, 160));
        if (cp.getCompletionPercentage() < 100) {
            bar.setForeground(new Color(255, 150, 50));
        }

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setBackground(Color.WHITE);
        right.add(new JLabel(cp.getStatus()));
        
        row.add(name, BorderLayout.WEST);
        row.add(bar, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        
        return row;
    }
}
