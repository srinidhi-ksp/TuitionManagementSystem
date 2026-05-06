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
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, value.length() > 8 ? 16 : 24));
        valueLabel.setForeground(new Color(26, 35, 64));
        
        textPanel.add(titleLabel);
        textPanel.add(valueLabel);

        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }
    
    private JPanel createNotificationCard(List<String> notices) {
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
        
        if (notices == null || notices.isEmpty()) {
            JLabel lbl = new JLabel("No new notifications");
            lbl.setFont(new Font("SansSerif", Font.ITALIC, 13));
            lbl.setForeground(new Color(107, 122, 153));
            list.add(lbl);
        } else {
            for (String n : notices) {
                JLabel lbl = new JLabel("• " + n);
                lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
                lbl.setForeground(new Color(107, 122, 153));
                lbl.setBorder(new EmptyBorder(4, 0, 4, 0));
                list.add(lbl);
            }
        }
        
        card.add(new JScrollPane(list), BorderLayout.CENTER);
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
        new SwingWorker<java.util.Map<String, Object>, Void>() {
            @Override
            protected java.util.Map<String, Object> doInBackground() throws Exception {
                String userId = util.SessionManager.getInstance().getUserId();
                if (userId == null) return null;
                
                // EXACT Step 4 logic: Get student dashboard
                model.StudentDashboard dashboard = studentService.getDashboard(userId);
                
                // Additional data for the UI
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("dashboard", dashboard);
                data.put("batches", studentService.getActiveBatches(userId));
                
                service.ParentPortalService ppService = new service.ParentPortalService();
                data.put("notifications", ppService.getNotifications(userId));
                
                return data;
            }

            @Override
            protected void done() {
                try {
                    java.util.Map<String, Object> data = get();
                    if (data == null) return;

                    model.StudentDashboard dash = (model.StudentDashboard) data.get("dashboard");
                    List<Batch> enrolled = (List<Batch>) data.get("batches");
                    List<String> notices = (List<String>) data.get("notifications");

                    title.setText("Welcome back, " + (dash.getName() != null ? dash.getName() : "Student") + "!");

                    statsPanel.removeAll();
                    statsPanel.add(createModernStatCard("Active Batches", String.valueOf(dash.getBatchCount()), new Color(74, 144, 226)));
                    statsPanel.add(createModernStatCard("Total Fees", String.format("₹%.0f", dash.getTotalFees()), new Color(52, 211, 153)));
                    statsPanel.add(createModernStatCard("Paid Amount", String.format("₹%.0f", dash.getPaidAmount()), new Color(167, 139, 250)));
                    statsPanel.add(createModernStatCard("Pending", String.format("₹%.0f", dash.getPending()), new Color(251, 146, 60)));
                    
                    lowerPanel.removeAll();
                    lowerPanel.add(createNotificationCard(notices));
                    lowerPanel.add(createUpcomingClassesCard(enrolled));
                    
                    // Step 6: Force Refresh
                    revalidate();
                    repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}
