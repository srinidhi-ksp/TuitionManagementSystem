package ui.parent;

import java.awt.*;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import model.Student;
import model.User;
import service.ParentPortalService;
import util.SessionManager;
import util.ThemeManager;

/**
 * Parent Overview Panel - Summary Cards, Notifications, and Recent Activity
 */
public class ParentOverviewPanel extends JPanel {

    private static final Color ACCENT = new Color(59, 130, 246);
    private static final Color SUCCESS = new Color(34, 197, 94);
    private static final Color WARNING = new Color(245, 158, 11);
    private static final Color ERROR = new Color(239, 68, 68);

    private ParentPortalService portalService;
    private JComboBox<String> studentSelector;
    private JPanel cardsPanel;
    private List<Student> linkedStudents;
    private Student currentStudent;
    private JPanel notificationPanel;
    private JPanel activityPanel;

    public ParentOverviewPanel() {
        this.portalService = new ParentPortalService();
        setLayout(new BorderLayout(0, 24));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(32, 40, 40, 40));

        initHeader();
        initContent();
        
        loadInitialData();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.BG);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(ThemeManager.BG);
        JLabel title = new JLabel("Parent Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(ThemeManager.TEXT);
        JLabel sub = new JLabel("Quick overview of your child's recent performance and alerts");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(ThemeManager.SUB_TEXT);
        titles.add(title);
        titles.add(sub);

        // Student Selector
        studentSelector = new JComboBox<>();
        studentSelector.setPreferredSize(new Dimension(220, 38));
        studentSelector.addActionListener(e -> onStudentSelected());

        header.add(titles, BorderLayout.WEST);
        header.add(studentSelector, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
    }

    private void initContent() {
        JPanel center = new JPanel(new BorderLayout(0, 30));
        center.setBackground(ThemeManager.BG);

        // 1. Cards Row (Top)
        cardsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        cardsPanel.setBackground(ThemeManager.BG);
        center.add(cardsPanel, BorderLayout.NORTH);

        // 2. Main Content Area (Notifications + Activity)
        JPanel mainGrid = new JPanel(new GridLayout(1, 2, 30, 0));
        mainGrid.setBackground(ThemeManager.BG);

        // Left Column: Notifications
        notificationPanel = new JPanel();
        notificationPanel.setLayout(new BoxLayout(notificationPanel, BoxLayout.Y_AXIS));
        notificationPanel.setBackground(ThemeManager.BG);
        mainGrid.add(createSection("🔔 Notifications", notificationPanel));

        // Right Column: Recent Activity
        activityPanel = new JPanel();
        activityPanel.setLayout(new BoxLayout(activityPanel, BoxLayout.Y_AXIS));
        activityPanel.setBackground(ThemeManager.BG);
        mainGrid.add(createSection("🕒 Recent Activity", activityPanel));

        center.add(mainGrid, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private JPanel createSection(String title, JPanel content) {
        JPanel section = new JPanel(new BorderLayout(0, 15));
        section.setBackground(ThemeManager.BG);
        
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        lbl.setForeground(ThemeManager.TEXT);
        section.add(lbl, BorderLayout.NORTH);
        
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(ThemeManager.BG);
        scroll.getViewport().setBackground(ThemeManager.BG);
        section.add(scroll, BorderLayout.CENTER);
        
        return section;
    }

    private void loadInitialData() {
        String parentId = SessionManager.getInstance().getUserId();
        linkedStudents = portalService.getLinkedStudents(parentId);
        
        studentSelector.removeAllItems();
        for (Student s : linkedStudents) {
            studentSelector.addItem(s.getName() + " (" + s.getUserId() + ")");
        }
        
        if (!linkedStudents.isEmpty()) {
            studentSelector.setSelectedIndex(0);
        }
    }

    private void onStudentSelected() {
        int idx = studentSelector.getSelectedIndex();
        if (idx >= 0 && idx < linkedStudents.size()) {
            currentStudent = linkedStudents.get(idx);
            refreshStats();
            refreshNotifications();
            refreshActivity();
        }
    }

    private void refreshStats() {
        Map<String, Object> stats = portalService.getStudentOverview(currentStudent.getUserId());
        cardsPanel.removeAll();

        cardsPanel.add(createStatCard("STUDENT NAME", currentStudent.getName().split(" ")[0], "👤", ACCENT));
        cardsPanel.add(createStatCard("STD / BATCH", currentStudent.getCurrentStd() + " / " + (currentStudent.getBoard() != null ? currentStudent.getBoard() : "N/A"), "🎓", SUCCESS));
        cardsPanel.add(createStatCard("ATTENDANCE", String.format("%.1f%%", stats.get("attendancePercent")), "🗓️", WARNING));
        
        Map<String, Object> fees = (Map<String, Object>) stats.get("fees");
        double pending = (double) fees.get("pendingAmount");
        cardsPanel.add(createStatCard("PENDING FEES", String.format("₹%.0f", pending), "💰", pending > 0 ? ERROR : SUCCESS));

        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private void refreshNotifications() {
        notificationPanel.removeAll();
        List<String> notes = portalService.getNotifications(currentStudent.getUserId());
        if (notes.isEmpty()) {
            notificationPanel.add(createEmptyState("No new notifications"));
        } else {
            for (String note : notes) {
                notificationPanel.add(createNotificationItem(note));
                notificationPanel.add(Box.createVerticalStrut(10));
            }
        }
        notificationPanel.revalidate();
        notificationPanel.repaint();
    }

    private void refreshActivity() {
        activityPanel.removeAll();
        List<Map<String, String>> activities = portalService.getRecentActivity(currentStudent.getUserId());
        if (activities.isEmpty()) {
            activityPanel.add(createEmptyState("No recent activity"));
        } else {
            for (Map<String, String> act : activities) {
                activityPanel.add(createActivityItem(act));
                activityPanel.add(Box.createVerticalStrut(12));
            }
        }
        activityPanel.revalidate();
        activityPanel.repaint();
    }

    private JPanel createNotificationItem(String text) {
        JPanel p = new JPanel(new BorderLayout(15, 0));
        p.setBackground(ThemeManager.CARD);
        p.setBorder(new EmptyBorder(15, 20, 15, 20));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JLabel lbl = new JLabel("<html><body style='width: 250px'>" + text + "</body></html>");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(ThemeManager.TEXT);
        p.add(lbl, BorderLayout.CENTER);
        
        return p;
    }

    private JPanel createActivityItem(Map<String, String> act) {
        JPanel p = new JPanel(new BorderLayout(15, 0));
        p.setBackground(ThemeManager.CARD);
        p.setBorder(new EmptyBorder(15, 20, 15, 20));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        JPanel textSide = new JPanel(new GridLayout(2, 1, 0, 2));
        textSide.setOpaque(false);
        
        JLabel title = new JLabel(act.get("title"));
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(ThemeManager.TEXT);
        
        JLabel desc = new JLabel(act.get("desc"));
        desc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        desc.setForeground(ThemeManager.SUB_TEXT);
        
        textSide.add(title);
        textSide.add(desc);
        
        JLabel date = new JLabel(act.get("date"));
        date.setFont(new Font("SansSerif", Font.BOLD, 12));
        date.setForeground(ACCENT);
        
        p.add(textSide, BorderLayout.CENTER);
        p.add(date, BorderLayout.EAST);
        
        return p;
    }

    private JLabel createEmptyState(String msg) {
        JLabel l = new JLabel(msg);
        l.setFont(new Font("SansSerif", Font.ITALIC, 13));
        l.setForeground(ThemeManager.SUB_TEXT);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setBorder(new EmptyBorder(20, 0, 0, 0));
        return l;
    }

    private JPanel createStatCard(String title, String value, String icon, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(accent);
                g2.fillRect(0, 0, 4, getHeight());
                g2.dispose();
            }
        };
        card.setBackground(ThemeManager.CARD);
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        titleLbl.setForeground(ThemeManager.SUB_TEXT);

        JPanel valRow = new JPanel(new BorderLayout());
        valRow.setOpaque(false);
        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("SansSerif", Font.BOLD, 22));
        valLbl.setForeground(ThemeManager.TEXT);
        
        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 24));
        
        valRow.add(valLbl, BorderLayout.WEST);
        valRow.add(iconLbl, BorderLayout.EAST);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valRow, BorderLayout.CENTER);
        return card;
    }
}
