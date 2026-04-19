package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

import model.User;
import model.Batch;
import dao.BatchDAO;

public class OverviewPanel extends JPanel {

    private User teacherContext;
    private Color bgLight = new Color(245, 247, 250);

    public OverviewPanel(User user) {
        this.teacherContext = user;
        setLayout(new BorderLayout());
        setBackground(bgLight);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(bgLight);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("Welcome back, " + (teacherContext.getName() != null ? teacherContext.getName() : "Teacher") + " 👋");
        title.setFont(new Font("Serif", Font.BOLD, 28));
        
        JLabel subtitle = new JLabel("Your Academic Dashboard Overview");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(Color.GRAY);

        header.add(title);
        header.add(subtitle);

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(bgLight);

        List<Batch> myBatches = new BatchDAO().getBatchesByTeacherId(teacherContext.getUserId());
        int batchCount = myBatches != null ? myBatches.size() : 0;

        statsPanel.add(createModernStatCard("Assigned Batches", String.valueOf(batchCount), new Color(100, 150, 255)));
        statsPanel.add(createModernStatCard("Total Students", "72", new Color(100, 200, 150))); 
        statsPanel.add(createModernStatCard("Avg Attendance", "88%", new Color(180, 100, 255))); 
        statsPanel.add(createModernStatCard("Pending Tasks", "3", new Color(255, 100, 100))); 

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(bgLight);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(statsPanel, BorderLayout.CENTER);

        add(topSection, BorderLayout.NORTH);
        
        JPanel lowerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        lowerPanel.setBackground(bgLight);
        lowerPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        lowerPanel.add(createTodayClassesCard(myBatches));
        lowerPanel.add(createPendingTasksCard());

        add(lowerPanel, BorderLayout.CENTER);
    }

    private JPanel createModernStatCard(String title, String value, Color iconColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230), 1, true),
            new EmptyBorder(25, 25, 25, 25)
        ));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.GRAY);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        
        textPanel.add(titleLabel);
        textPanel.add(valueLabel);

        JLabel iconLabel = new JLabel("●");
        iconLabel.setForeground(iconColor);
        iconLabel.setFont(new Font("Arial", Font.BOLD, 30));

        card.add(textPanel, BorderLayout.CENTER);
        card.add(iconLabel, BorderLayout.EAST);

        return card;
    }
    
    private JPanel createTodayClassesCard(List<Batch> batches) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel t = new JLabel("Today's Classes");
        t.setFont(new Font("Arial", Font.BOLD, 16));
        t.setBorder(new EmptyBorder(0,0,10,0));
        card.add(t, BorderLayout.NORTH);
        
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        
        if (batches != null && !batches.isEmpty()) {
            for (Batch b : batches) {
                String subText = b.getBatchName() + " (" + (b.getStartTime() != null ? b.getStartTime().toString().substring(11,16) : "") + ")";
                JLabel l = new JLabel("• " + subText);
                l.setForeground(Color.DARK_GRAY);
                l.setBorder(new EmptyBorder(5,0,5,0));
                list.add(l);
            }
        } else {
            JLabel empty = new JLabel("No classes scheduled today.");
            empty.setForeground(Color.GRAY);
            list.add(empty);
        }
        
        card.add(new JScrollPane(list), BorderLayout.CENTER);
        return card;
    }

    private JPanel createPendingTasksCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel t = new JLabel("Action Items");
        t.setFont(new Font("Arial", Font.BOLD, 16));
        t.setBorder(new EmptyBorder(0,0,10,0));
        card.add(t, BorderLayout.NORTH);
        
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        
        JLabel n1 = new JLabel("• Warning: Class 10th Math attendance not marked yesterday");
        n1.setForeground(new Color(220, 80, 80));
        n1.setBorder(new EmptyBorder(5,0,5,0));
        
        JLabel n2 = new JLabel("• Reminder: Quantum Physics midterm marks pending");
        n2.setForeground(new Color(255, 150, 50));
        n2.setBorder(new EmptyBorder(5,0,5,0));
        
        list.add(n1);
        list.add(n2);
        
        card.add(list, BorderLayout.CENTER);
        return card;
    }
}
