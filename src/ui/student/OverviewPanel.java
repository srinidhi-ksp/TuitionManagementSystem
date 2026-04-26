package ui.student;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import model.Batch;

public class OverviewPanel extends JPanel {

    private Color bgLight = new Color(245, 247, 250);
    private JPanel statsPanel;
    private JPanel lowerPanel;
    private JLabel title;

    private service.StudentService studentService;

    public OverviewPanel() {
        this.studentService = new service.StudentService();
        setLayout(new BorderLayout());
        setBackground(bgLight);
        setBorder(new EmptyBorder(32, 40, 32, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(bgLight);
        header.setBorder(new EmptyBorder(0, 0, 24, 0));
        
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setBackground(bgLight);
        title = new JLabel("Welcome back!");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(new Color(26, 35, 64));
        
        JLabel subtitle = new JLabel("Track your academic progress and upcoming schedule");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(new Color(107, 122, 153));
        titlePanel.add(title);
        titlePanel.add(subtitle);

        JButton refreshBtn = new JButton("↻ Refresh Dashboard");
        refreshBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshBtn.setBackground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> loadOverviewDataAsync());

        header.add(titlePanel, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(bgLight);

        JPanel topSection = new JPanel(new BorderLayout(0, 20));
        topSection.setBackground(bgLight);
        topSection.add(header, BorderLayout.NORTH);
        topSection.add(statsPanel, BorderLayout.CENTER);

        add(topSection, BorderLayout.NORTH);
        
        lowerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        lowerPanel.setBackground(bgLight);
        lowerPanel.setBorder(new EmptyBorder(24, 0, 0, 0));

        add(lowerPanel, BorderLayout.CENTER);

        loadOverviewDataAsync();
    }

    private JPanel createModernStatCard(String title, String value, Color iconColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g2.dispose();
            }
        };
        card.setBackground(Color.WHITE);
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        textPanel.setOpaque(false);
        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setForeground(new Color(107, 122, 153));
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        valueLabel.setForeground(new Color(26, 35, 64));
        
        textPanel.add(titleLabel);
        textPanel.add(valueLabel);

        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }
    
    private JPanel createNotificationCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,235,245), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel t = new JLabel("Notifications");
        t.setFont(new Font("SansSerif", Font.BOLD, 16));
        t.setForeground(new Color(26, 35, 64));
        t.setBorder(new EmptyBorder(0,0,12,0));
        card.add(t, BorderLayout.NORTH);
        
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        
        String[] notices = {
            "• Welcome to the new Student Portal!",
            "• Check 'Fees & Payments' for upcoming dues.",
            "• Your attendance record is being updated."
        };

        for (String n : notices) {
            JLabel lbl = new JLabel(n);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            lbl.setForeground(new Color(107, 122, 153));
            lbl.setBorder(new EmptyBorder(4, 0, 4, 0));
            list.add(lbl);
        }
        
        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel createUpcomingClassesCard(List<Batch> batches) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230,235,245), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        JLabel t = new JLabel("Enrolled Batches");
        t.setFont(new Font("SansSerif", Font.BOLD, 16));
        t.setForeground(new Color(26, 35, 64));
        t.setBorder(new EmptyBorder(0,0,12,0));
        card.add(t, BorderLayout.NORTH);
        
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        
        if (batches != null && !batches.isEmpty()) {
            for (Batch b : batches) {
                JLabel l = new JLabel("• " + b.getBatchName() + " [" + b.getClassMode() + "]");
                l.setFont(new Font("SansSerif", Font.PLAIN, 13));
                l.setForeground(new Color(107, 122, 153));
                l.setBorder(new EmptyBorder(4, 0, 4, 0));
                list.add(l);
            }
        } else {
            JLabel empty = new JLabel("No active enrollments found.");
            empty.setFont(new Font("SansSerif", Font.ITALIC, 13));
            empty.setForeground(new Color(107, 122, 153));
            list.add(empty);
        }
        
        card.add(new JScrollPane(list), BorderLayout.CENTER);
        return card;
    }

    private void loadOverviewDataAsync() {
        new SwingWorker<List<Batch>, Void>() {
            @Override
            protected List<Batch> doInBackground() throws Exception {
                String userIdFromSession = util.SessionManager.getInstance().getUserId();
                System.out.println("[OverviewPanel] 🔄 Loading dashboard for user: " + userIdFromSession);
                
                if (userIdFromSession == null) {
                    System.err.println("[OverviewPanel] ❌ User ID is null!");
                    return new java.util.ArrayList<>();
                }
                
                // studentService.getActiveBatches() will internally resolve user_id → student_id
                List<Batch> batches = studentService.getActiveBatches(userIdFromSession);
                System.out.println("[OverviewPanel] ✅ Found " + (batches != null ? batches.size() : 0) + " active batches");
                
                return batches;
            }

            @Override
            protected void done() {
                try {
                    List<Batch> enrolled = get();
                    int batchCount = enrolled != null ? enrolled.size() : 0;
                    
                    System.out.println("[OverviewPanel] Updating stats with " + batchCount + " batches");
                    
                    String userName = util.SessionManager.getInstance().getUserName();
                    title.setText("Welcome back, " + (userName != null ? userName : "Student") + "!");

                    statsPanel.removeAll();
                    statsPanel.add(createModernStatCard("Active Batches", String.valueOf(batchCount), new Color(74, 144, 226)));
                    statsPanel.add(createModernStatCard("Subjects", String.valueOf(batchCount), new Color(52, 211, 153)));
                    statsPanel.add(createModernStatCard("Attendance", "92%", new Color(167, 139, 250)));
                    statsPanel.add(createModernStatCard("Fees Status", batchCount > 0 ? "PAID" : "N/A", new Color(251, 146, 60)));
                    
                    lowerPanel.removeAll();
                    lowerPanel.add(createNotificationCard());
                    lowerPanel.add(createUpcomingClassesCard(enrolled));
                    
                    revalidate();
                    repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}
