package ui.parent;

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
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import model.User;
import ui.LoginFrame;
import ui.student.AttendancePanel;
import ui.student.FeesPanel;
import ui.student.MyBatchesPanel;
import ui.student.MySubjectsPanel;
import ui.student.OverviewPanel;
import ui.student.ProfilePanel;
import ui.student.SyllabusProgressPanel;
import util.SessionManager;

public class ParentDashboard extends JFrame {

    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sidebarPanel;
    private JButton activeBtn = null;

    public ParentDashboard(User user) {
        setTitle("MRK Tuition – Parent Portal");
        setSize(1300, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(PAGE_BG);

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(PAGE_BG);

        // Register student panels (reused for parent as read-only)
        mainContentPanel.add(new OverviewPanel(), "Overview");
        mainContentPanel.add(new MySubjectsPanel(), "Student Subjects");
        mainContentPanel.add(new SyllabusProgressPanel(), "Syllabus Progress");
        mainContentPanel.add(new AttendancePanel(), "Attendance");
        mainContentPanel.add(new FeesPanel(), "Fees & Payments");
        mainContentPanel.add(new ProfilePanel(), "My Profile");

        add(createTopNavbar(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);
    }

    private JPanel createTopNavbar() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(NAV_BG);
        topPanel.setPreferredSize(new Dimension(0, 62));

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 12));
        logoPanel.setBackground(NAV_BG);
        JLabel logoText = new JLabel("MRK Tuition Parent Portal");
        logoText.setFont(new Font("SansSerif", Font.BOLD, 18));
        logoText.setForeground(Color.WHITE);
        logoPanel.add(logoText);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        userPanel.setBackground(NAV_BG);

        String userName = SessionManager.getInstance().getUserName();
        JButton profileBtn = new JButton("👤 " + (userName != null ? userName : "Parent") + " ▾");
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
        JMenuItem logoutItem = new JMenuItem("  Logout  ");
        logoutItem.setForeground(new Color(220, 50, 50));
        logoutItem.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        popup.add(logoutItem);
        profileBtn.addActionListener(e -> popup.show(profileBtn, 0, profileBtn.getHeight()));
        userPanel.add(profileBtn);

        topPanel.add(logoPanel, BorderLayout.WEST);
        topPanel.add(userPanel, BorderLayout.EAST);
        return topPanel;
    }

    private JPanel createSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setPreferredSize(new Dimension(240, 0));
        sidebarPanel.setBackground(NAV_BG);

        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        addSidebarItem("🏠", "Overview");
        addSidebarItem("📚", "Student Subjects");
        addSidebarItem("📊", "Syllabus Progress");
        addSidebarItem("📝", "Attendance");
        addSidebarItem("💰", "Fees & Payments");
        addSidebarItem("👤", "My Profile");

        sidebarPanel.add(Box.createGlue());
        
        JButton logoutBtn = createLogoutButton();
        sidebarPanel.add(logoutBtn);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        return sidebarPanel;
    }

    private void addSidebarItem(String icon, String label) {
        JButton btn = new JButton(icon + "    " + label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (this == activeBtn) {
                    g2.setColor(new Color(255, 255, 255, 30)); // NAV_ACTIVE_BG equivalent
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(ACCENT);
                    g2.fillRect(0, 0, 4, getHeight());
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 10));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setForeground(new Color(255, 255, 255, 180));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(240, 48));
        btn.setPreferredSize(new Dimension(240, 48));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 24, 0, 0));

        btn.addActionListener(e -> {
            if (activeBtn != null) activeBtn.setForeground(new Color(255, 255, 255, 180));
            activeBtn = btn;
            btn.setForeground(Color.WHITE);
            cardLayout.show(mainContentPanel, label);
            repaint();
        });

        sidebarPanel.add(btn);
        if (activeBtn == null && label.equals("Overview")) {
            activeBtn = btn;
            btn.setForeground(Color.WHITE);
        }
    }

    private JButton createLogoutButton() {
        JButton btn = new JButton("🚪    Logout");
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(new Color(255, 100, 100));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(240, 48));
        btn.setPreferredSize(new Dimension(240, 48));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 24, 0, 0));
        btn.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });
        return btn;
    }
}
