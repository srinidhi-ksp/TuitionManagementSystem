package ui.admin;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import dao.DashboardDAO;
import ui.LoginFrame;

public class AdminDashboard extends JFrame {

    // ── Design tokens ──────────────────────────────────────────────────────────
    private static final Color NAV_BG           = new Color(2, 6, 23);     // #020617
    private static final Color NAV_ACTIVE_BG    = new Color(30, 41, 59);   // #1e293b
    private static final Color NAV_ACTIVE_BORDER= new Color(59, 130, 246); // #3b82f6 (Primary Blue)
    private static final Color ACCENT           = new Color(59, 130, 246); // #3b82f6 (Primary Blue)
    private static final Color ACCENT_DARK      = new Color(37, 99, 235);
    private static final Color PAGE_BG          = new Color(248, 250, 252);
    private static final Color CARD_BG          = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);
    private static final Color TOPBAR_BG   = new Color(10, 27, 63);

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sidebarPanel;
    private JButton activeBtn = null;

    // Stat labels (refreshed on demand)
    private JLabel stdCountLabel;
    private JLabel teacherCountLabel;
    private JLabel subCountLabel;
    private JLabel batchCountLabel;

    // Dynamic panels
    private JPanel alertsListPanel;
    private JPanel attendanceSummaryPanel;

    public AdminDashboard() {
        setTitle("MRK Tuition – Admin Dashboard");
        setSize(1340, 820);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(PAGE_BG);

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(PAGE_BG);

        // Register all panels
        mainContentPanel.add(createDashboardPanel(),          "Admin Dashboard");
        mainContentPanel.add(new StudentManagementFrame(),    "Students");
        mainContentPanel.add(new ParentManagementFrame(),     "Parents");
        mainContentPanel.add(new TeacherManagementFrame(),    "Teachers");
        mainContentPanel.add(new SubjectManagementFrame(),    "Subjects");
        mainContentPanel.add(new BatchManagementFrame(),      "Batches");
        mainContentPanel.add(new EnrollmentManagementFrame(), "Enrollment");
        mainContentPanel.add(new AttendanceManagementFrame(), "Attendance");
        mainContentPanel.add(new FeesManagementPanel(), "Fees & Payments");
        mainContentPanel.add(new SettingsFrame(),             "Settings");

        add(createTopNavbar(),  BorderLayout.NORTH);
        add(createSidebar(),    BorderLayout.WEST);
        add(mainContentPanel,   BorderLayout.CENTER);
    }

    // ── Top Navbar ─────────────────────────────────────────────────────────────
    private JPanel createTopNavbar() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(TOPBAR_BG);
        topPanel.setPreferredSize(new Dimension(0, 60)); // Fixed height
        topPanel.setBorder(new EmptyBorder(5, 15, 5, 15)); // Padding

        // Logo (Left side)
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        logoPanel.setBackground(TOPBAR_BG);
        logoPanel.setOpaque(false);

        JLabel logoIcon = new JLabel("M") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        logoIcon.setOpaque(false);
        logoIcon.setForeground(Color.WHITE);
        logoIcon.setFont(new Font("SansSerif", Font.BOLD, 18));
        logoIcon.setHorizontalAlignment(SwingConstants.CENTER);
        logoIcon.setPreferredSize(new Dimension(36, 36));

        JLabel logoText = new JLabel("MRK Tuition");
        logoText.setFont(new Font("SansSerif", Font.BOLD, 20));
        logoText.setForeground(Color.WHITE);

        logoPanel.add(logoIcon);
        logoPanel.add(logoText);

        // User Profile Section (Right side)
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        userPanel.setBackground(TOPBAR_BG);
        userPanel.setOpaque(false);

        JButton profileBtn = new JButton("👤  Admin  ▾");
        profileBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        profileBtn.setForeground(Color.WHITE);
        profileBtn.setBackground(new Color(255, 255, 255, 25));
        profileBtn.setFocusPainted(false);
        profileBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 1, true),
            new EmptyBorder(6, 16, 6, 16)
        ));
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(CARD_BG);
        popup.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 240)));
        JMenuItem logoutItem = new JMenuItem(" Logout ");
        logoutItem.setForeground(new Color(220, 50, 50));
        logoutItem.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        popup.add(new JMenuItem(" My Profile "));
        popup.add(new JMenuItem(" Account Settings "));
        popup.addSeparator();
        popup.add(logoutItem);
        profileBtn.addActionListener(e -> popup.show(profileBtn, 0, profileBtn.getHeight()));
        
        userPanel.add(profileBtn);

        topPanel.add(logoPanel, BorderLayout.WEST);
        topPanel.add(userPanel, BorderLayout.EAST);
        return topPanel;
    }

    // ── Sidebar ────────────────────────────────────────────────────────────────
    private JPanel createSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setName("sidebar");
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setPreferredSize(new Dimension(240, 0));
        sidebarPanel.setBackground(NAV_BG);

        sidebarPanel.add(Box.createVerticalStrut(20));

        addSidebarSection("MANAGEMENT");
        addSidebarItem("🏠", "Admin Dashboard");
        addSidebarItem("👩‍🎓", "Students");
        addSidebarItem("👨‍🏫", "Teachers");
        addSidebarItem("👪", "Parents");
        addSidebarItem("📚", "Subjects");
        addSidebarItem("📋", "Batches");
        
        sidebarPanel.add(Box.createVerticalStrut(10));
        
        addSidebarSection("OPERATIONS");
        addSidebarItem("📝", "Enrollment");
        addSidebarItem("📊", "Attendance");
        addSidebarItem("💰", "Fees & Payments");

        sidebarPanel.add(Box.createVerticalStrut(10));
        
        addSidebarSection("SYSTEM");
        addSidebarItem("⚙", "Settings");

        sidebarPanel.add(Box.createGlue());
        
        // Logout Button
        JButton logoutBtn = createLogoutButton();
        sidebarPanel.add(logoutBtn);
        sidebarPanel.add(Box.createVerticalStrut(20));

        return sidebarPanel;
    }

    private void addSidebarSection(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(new Color(255, 255, 255, 60));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 24, 5, 0));
        sidebarPanel.add(lbl);
    }

    private void addSidebarItem(String icon, String label) {
        JButton btn = new JButton(icon + "    " + label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (this == activeBtn) {
                    g2.setColor(NAV_ACTIVE_BG);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(NAV_ACTIVE_BORDER);
                    g2.fillRect(0, 0, 4, getHeight());
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 15));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setForeground(new Color(255, 255, 255, 180));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Uniform sizing
        Dimension btnSize = new Dimension(240, 45);
        btn.setMaximumSize(btnSize);
        btn.setPreferredSize(btnSize);
        btn.setMinimumSize(btnSize);
        
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        
        // Consistent padding
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 15));

        btn.addActionListener(e -> {
            if (activeBtn != null) activeBtn.setForeground(new Color(255, 255, 255, 180));
            activeBtn = btn;
            btn.setForeground(Color.WHITE);
            cardLayout.show(mainContentPanel, label);
            if (label.equals("Admin Dashboard")) refreshDashboard();
            repaint();
        });

        sidebarPanel.add(btn);
        if (activeBtn == null && label.equals("Admin Dashboard")) {
            activeBtn = btn;
            btn.setForeground(Color.WHITE);
        }
    }

    private JButton createLogoutButton() {
        JButton btn = new JButton("🚪    Logout");
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(new Color(255, 100, 100));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        Dimension btnSize = new Dimension(240, 45);
        btn.setMaximumSize(btnSize);
        btn.setPreferredSize(btnSize);
        
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 15));
        
        btn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        return btn;
    }

    // ── Dashboard Panel ────────────────────────────────────────────────────────
    private JPanel createDashboardPanel() {
        JPanel dash = new JPanel(new BorderLayout(0, 24));
        dash.setBackground(PAGE_BG);
        dash.setBorder(new EmptyBorder(32, 36, 32, 36));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PAGE_BG);
        header.setBorder(new EmptyBorder(0, 0, 4, 0));
        JLabel title    = new JLabel("Admin Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(TEXT_PRI);
        JLabel subtitle = new JLabel("Overview of MRK Tuition Management System");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_SEC);
        JPanel titleStack = new JPanel(new GridLayout(2, 1, 0, 4));
        titleStack.setBackground(PAGE_BG);
        titleStack.add(title);
        titleStack.add(subtitle);
        header.add(titleStack, BorderLayout.WEST);

        // Stat cards
        DashboardDAO dao = new DashboardDAO();
        stdCountLabel     = makeCountLabel(String.valueOf(dao.getTotalStudents()));
        teacherCountLabel = makeCountLabel(String.valueOf(dao.getTotalTeachers()));
        subCountLabel     = makeCountLabel(String.valueOf(dao.getTotalSubjects()));
        batchCountLabel   = makeCountLabel(String.valueOf(dao.getTotalBatches()));

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        statsPanel.setBackground(PAGE_BG);
        statsPanel.add(createStatCard("Total Students",  stdCountLabel,     new Color(74, 144, 226),  "👩‍🎓"));
        statsPanel.add(createStatCard("Total Teachers",  teacherCountLabel, new Color(52, 211, 153),  "👨‍🏫"));
        statsPanel.add(createStatCard("Total Subjects",  subCountLabel,     new Color(167, 139, 250), "📚"));
        statsPanel.add(createStatCard("Total Batches",   batchCountLabel,   new Color(251, 146, 60),  "📋"));

        JPanel topSection = new JPanel(new BorderLayout(0, 20));
        topSection.setBackground(PAGE_BG);
        topSection.add(header,     BorderLayout.NORTH);
        topSection.add(statsPanel, BorderLayout.CENTER);

        // Attendance + Alerts
        JPanel lowerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        lowerPanel.setBackground(PAGE_BG);
        Map<String, Integer> attData = dao.getAttendanceSummary();
        attendanceSummaryPanel = buildAttendanceSummaryCard(attData);
        List<String> alerts = dao.getSystemAlerts();
        alertsListPanel = buildAlertsCard(alerts);
        lowerPanel.add(attendanceSummaryPanel);
        lowerPanel.add(alertsListPanel);

        dash.add(topSection,  BorderLayout.NORTH);
        dash.add(lowerPanel,  BorderLayout.CENTER);
        return dash;
    }

    // ── Stat Card ──────────────────────────────────────────────────────────────
    private JLabel makeCountLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 34));
        lbl.setForeground(TEXT_PRI);
        return lbl;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accent, String icon) {
        JPanel card = new JPanel(new BorderLayout()) {
            private Color shadowColor = new Color(0, 0, 0, 12);
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Shadow
                g2.setColor(shadowColor);
                g2.fillRoundRect(4, 6, getWidth() - 8, getHeight() - 6, 16, 16);
                // Card
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(22, 24, 22, 24));

        // Hover effect: elevate shadow
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBorder(new EmptyBorder(20, 24, 24, 24));
                card.repaint();
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBorder(new EmptyBorder(22, 24, 22, 24));
                card.repaint();
            }
        });

        JPanel textPanel = new JPanel(new GridLayout(3, 1, 0, 4));
        textPanel.setBackground(CARD_BG);
        textPanel.setOpaque(false);

        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        titleLbl.setForeground(TEXT_SEC);

        JLabel iconLbl = new JLabel(icon + "  ");
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 28));
        iconLbl.setForeground(accent);

        textPanel.add(iconLbl);
        textPanel.add(valueLabel);
        textPanel.add(titleLbl);

        card.add(textPanel, BorderLayout.CENTER);

        // Left accent stripe
        JPanel stripe = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(accent);
                g2.fillRoundRect(0, 16, 4, getHeight() - 32, 4, 4);
                g2.dispose();
            }
        };
        stripe.setOpaque(false);
        stripe.setPreferredSize(new Dimension(8, 0));
        card.add(stripe, BorderLayout.WEST);

        return card;
    }

    // ── Attendance Summary Card ────────────────────────────────────────────────
    private JPanel buildAttendanceSummaryCard(Map<String, Integer> data) {
        JPanel card = createBaseCard("📊  Attendance Overview");

        JPanel bars = new JPanel(new GridLayout(3, 1, 0, 12));
        bars.setBackground(CARD_BG);
        bars.setBorder(new EmptyBorder(16, 24, 24, 24));

        int present = data.getOrDefault("PRESENT", 0);
        int absent  = data.getOrDefault("ABSENT",  0);
        int leave   = data.getOrDefault("LEAVE",   0);
        int total   = Math.max(present + absent + leave, 1);

        bars.add(makeAttRow("Present", present, total, new Color(52, 211, 153)));
        bars.add(makeAttRow("Absent",  absent,  total, new Color(248, 113, 113)));
        bars.add(makeAttRow("Leave",   leave,   total, new Color(251, 191, 36)));

        card.add(bars, BorderLayout.CENTER);
        return card;
    }

    private JPanel makeAttRow(String label, int count, int total, Color color) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(CARD_BG);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(TEXT_PRI);
        lbl.setPreferredSize(new Dimension(70, 20));

        JLabel countLbl = new JLabel(String.valueOf(count));
        countLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        countLbl.setForeground(color);
        countLbl.setPreferredSize(new Dimension(36, 20));
        countLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        int pct = (int) Math.round((count * 100.0) / total);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(pct);
        bar.setString(pct + "%");
        bar.setStringPainted(true);
        bar.setForeground(color);
        bar.setBackground(new Color(240, 242, 248));
        bar.setBorder(BorderFactory.createEmptyBorder());
        bar.setFont(new Font("SansSerif", Font.BOLD, 10));
        bar.setPreferredSize(new Dimension(0, 20));

        row.add(lbl,      BorderLayout.WEST);
        row.add(bar,      BorderLayout.CENTER);
        row.add(countLbl, BorderLayout.EAST);
        return row;
    }

    // ── Alerts Card ────────────────────────────────────────────────────────────
    private JPanel buildAlertsCard(List<String> alerts) {
        JPanel card = createBaseCard("🔔  System Alerts");

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(CARD_BG);
        list.setBorder(new EmptyBorder(8, 24, 16, 24));

        for (String alert : alerts) {
            Color c = alert.startsWith("✅") ? new Color(34, 197, 94)
                    : alert.startsWith("❌") ? new Color(239, 68, 68)
                    : TEXT_SEC;
            JLabel row = new JLabel(alert);
            row.setFont(new Font("SansSerif", Font.PLAIN, 13));
            row.setForeground(c);
            row.setBorder(new EmptyBorder(6, 0, 6, 0));
            list.add(row);
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ── Base card factory ──────────────────────────────────────────────────────
    private JPanel createBaseCard(String headerText) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fillRoundRect(4, 6, getWidth() - 8, getHeight() - 6, 16, 16);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);

        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setOpaque(false);
        cardHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 240, 250)),
            new EmptyBorder(16, 24, 16, 24)
        ));
        JLabel hLbl = new JLabel(headerText);
        hLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        hLbl.setForeground(TEXT_PRI);
        cardHeader.add(hLbl, BorderLayout.WEST);
        card.add(cardHeader, BorderLayout.NORTH);
        return card;
    }

    // ── Refresh ────────────────────────────────────────────────────────────────
    private void refreshDashboard() {
        DashboardDAO dao = new DashboardDAO();
        if (stdCountLabel     != null) stdCountLabel.setText(String.valueOf(dao.getTotalStudents()));
        if (teacherCountLabel != null) teacherCountLabel.setText(String.valueOf(dao.getTotalTeachers()));
        if (subCountLabel     != null) subCountLabel.setText(String.valueOf(dao.getTotalSubjects()));
        if (batchCountLabel   != null) batchCountLabel.setText(String.valueOf(dao.getTotalBatches()));

        if (attendanceSummaryPanel != null) {
            attendanceSummaryPanel.removeAll();
            JPanel fresh = buildAttendanceSummaryCard(dao.getAttendanceSummary());
            attendanceSummaryPanel.setLayout(fresh.getLayout());
            for (Component c : fresh.getComponents())
                attendanceSummaryPanel.add(c, ((BorderLayout) fresh.getLayout()).getConstraints(c));
            attendanceSummaryPanel.revalidate();
            attendanceSummaryPanel.repaint();
        }
        if (alertsListPanel != null) {
            alertsListPanel.removeAll();
            JPanel fresh = buildAlertsCard(dao.getSystemAlerts());
            alertsListPanel.setLayout(fresh.getLayout());
            for (Component c : fresh.getComponents())
                alertsListPanel.add(c, ((BorderLayout) fresh.getLayout()).getConstraints(c));
            alertsListPanel.revalidate();
            alertsListPanel.repaint();
        }
    }
}