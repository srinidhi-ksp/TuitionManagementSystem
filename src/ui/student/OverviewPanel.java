package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

import model.User;
import model.Batch;
import dao.EnrollmentDAO;

public class OverviewPanel extends JPanel {

    private User student;
    private Color bgLight = new Color(245, 247, 250);

    public OverviewPanel(User user) {
        this.student = user;
        setLayout(new BorderLayout());
        setBackground(bgLight);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setBackground(bgLight);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("Welcome back, " + (student.getName() != null ? student.getName() : "Student") + "!");
        title.setFont(new Font("Serif", Font.BOLD, 28));
        
        JLabel subtitle = new JLabel("Here is your academic summary across all active batches.");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(Color.GRAY);

        header.add(title);
        header.add(subtitle);

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(bgLight);

        // Fetch counts using Enrollment or standard mappings
        List<Batch> enrolled = new EnrollmentDAO().getBatchesByStudentId(student.getUserId());
        int batchCount = enrolled != null ? enrolled.size() : 0;

        statsPanel.add(createModernStatCard("Active Batches", String.valueOf(batchCount), new Color(100, 150, 255)));
        statsPanel.add(createModernStatCard("Syllabus Avg", "65%", new Color(100, 200, 150))); // mock dynamic logic placeholder
        statsPanel.add(createModernStatCard("Attendance", "92%", new Color(180, 100, 255))); // mock dynamic logic placeholder
        statsPanel.add(createModernStatCard("Fees Status", "PAID", new Color(100, 200, 150))); // mock dynamic logic placeholder

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(bgLight);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(statsPanel, BorderLayout.CENTER);

        add(topSection, BorderLayout.NORTH);
        
        JPanel lowerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        lowerPanel.setBackground(bgLight);
        lowerPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        
        lowerPanel.add(createNotificationCard());
        lowerPanel.add(createUpcomingClassesCard(enrolled));

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
    
    private JPanel createNotificationCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel t = new JLabel("Notifications");
        t.setFont(new Font("Arial", Font.BOLD, 16));
        t.setBorder(new EmptyBorder(0,0,10,0));
        card.add(t, BorderLayout.NORTH);
        
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        
        JLabel n1 = new JLabel("• Tomorrow: Math Class at 10:00 AM");
        n1.setForeground(Color.DARK_GRAY);
        n1.setBorder(new EmptyBorder(5,0,5,0));
        
        JLabel n2 = new JLabel("• Fees due in 5 days (Rs. 2500)");
        n2.setForeground(new Color(220, 80, 80));
        n2.setBorder(new EmptyBorder(5,0,5,0));
        
        list.add(n1);
        list.add(n2);
        
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel createUpcomingClassesCard(List<Batch> batches) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,230,230), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel t = new JLabel("Upcoming Classes");
        t.setFont(new Font("Arial", Font.BOLD, 16));
        t.setBorder(new EmptyBorder(0,0,10,0));
        card.add(t, BorderLayout.NORTH);
        
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        
        if (batches != null && !batches.isEmpty()) {
            for (Batch b : batches) {
                JLabel l = new JLabel("• " + b.getBatchName() + " (" + b.getClassMode() + ")");
                l.setForeground(Color.DARK_GRAY);
                l.setBorder(new EmptyBorder(5,0,5,0));
                list.add(l);
            }
        } else {
            JLabel empty = new JLabel("No upcoming classes scheduled.");
            empty.setForeground(Color.GRAY);
            list.add(empty);
        }
        
        card.add(new JScrollPane(list), BorderLayout.CENTER);
        return card;
    }
}
