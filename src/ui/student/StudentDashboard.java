package ui.student;

import ui.LoginFrame;
import util.SessionManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StudentDashboard extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sidebarPanel;

    private Color brandColor = new Color(50, 150, 250); // Student Blue
    private Color sidebarBg = Color.WHITE;
    private Color bgLight = new Color(245, 247, 250);
    
    public StudentDashboard(model.User user) {
        
        setTitle("MRK Tuition - Student Portal");
        setSize(1300, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(bgLight);

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(bgLight);

        // Sub-panels
        mainContentPanel.add(new OverviewPanel(), "Dashboard");
        mainContentPanel.add(new MySubjectsPanel(), "My Subjects");
        mainContentPanel.add(new MyBatchesPanel(), "My Batches");
        mainContentPanel.add(new SyllabusProgressPanel(), "Syllabus Progress");
        mainContentPanel.add(new AttendancePanel(), "Attendance");
        mainContentPanel.add(new FeesPanel(), "Fees & Payments");
        mainContentPanel.add(new ProfilePanel(), "Profile");

        add(createTopNavbar(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);
        
        // Apply current theme
        util.ThemeUtil.apply(this);
    }

    private JPanel createTopNavbar() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setPreferredSize(new Dimension(0, 60)); // Standard height
        topPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
            new EmptyBorder(5, 15, 5, 15) // Consistent padding
        ));

        // Left Side: Logo/Title
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        logoPanel.setBackground(Color.WHITE);
        logoPanel.setOpaque(false);

        JLabel logoIcon = new JLabel("S"); 
        logoIcon.setOpaque(true);
        logoIcon.setBackground(brandColor);
        logoIcon.setForeground(Color.WHITE);
        logoIcon.setFont(new Font("SansSerif", Font.BOLD, 18));
        logoIcon.setHorizontalAlignment(SwingConstants.CENTER);
        logoIcon.setPreferredSize(new Dimension(36, 36));
        // Simple rounded circle effect for S icon
        logoIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JLabel logoText = new JLabel("MRK Tuition Student Portal");
        logoText.setFont(new Font("SansSerif", Font.BOLD, 20));
        logoText.setForeground(Color.DARK_GRAY);
        
        logoPanel.add(logoIcon);
        logoPanel.add(logoText);

        // Right Side: User Profile
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        userPanel.setBackground(Color.WHITE);
        userPanel.setOpaque(false);
        
        String userName = SessionManager.getInstance().getUserName();
        JButton profileBtn = new JButton("👤 " + (userName != null ? userName : "Student") + " ▾");
        profileBtn.setBackground(new Color(245, 247, 250));
        profileBtn.setForeground(Color.DARK_GRAY);
        profileBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        profileBtn.setFocusPainted(false);
        profileBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBackground(Color.WHITE);
        popupMenu.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 240)));
        
        JMenuItem logoutItem = new JMenuItem("  Logout  ");
        logoutItem.setForeground(new Color(220, 50, 50));
        logoutItem.addActionListener(e -> {
            SessionManager.getInstance().clearSession();
            dispose();
            new LoginFrame().setVisible(true);
        });
        
        popupMenu.add(new JMenuItem("  My Profile  "));
        popupMenu.addSeparator();
        popupMenu.add(logoutItem);
        profileBtn.addActionListener(e -> popupMenu.show(profileBtn, 0, profileBtn.getHeight()));
        
        userPanel.add(profileBtn);

        topPanel.add(logoPanel, BorderLayout.WEST);
        topPanel.add(userPanel, BorderLayout.EAST);
        return topPanel;
    }

    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(50, 150, 250);
    private JButton activeBtn = null;

    private JPanel createSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setName("sidebar");
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setPreferredSize(new Dimension(240, 0));
        sidebarPanel.setBackground(NAV_BG);

        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel menuLabel = new JLabel("STUDENT MENU");
        menuLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        menuLabel.setForeground(new Color(255, 255, 255, 60));
        menuLabel.setBorder(new EmptyBorder(0, 24, 8, 0));
        menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarPanel.add(menuLabel);

        String[] menuItems = {
            "Dashboard", "My Subjects", "My Batches", "Syllabus Progress", 
            "Attendance", "Fees & Payments", "Profile"
        };
        String[] icons = {"🏠", "📚", "📋", "📈", "📊", "💰", "👤"};

        for (int i = 0; i < menuItems.length; i++) {
            final String item = menuItems[i];
            String icon = icons[i];
            JButton btn = new JButton(icon + "    " + item) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (this == activeBtn) {
                        g2.setColor(new Color(255, 255, 255, 30));
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
                cardLayout.show(mainContentPanel, item);
                repaint();
            });

            sidebarPanel.add(btn);
            if (activeBtn == null && item.equals("Dashboard")) {
                activeBtn = btn;
                btn.setForeground(Color.WHITE);
            }
        }

        sidebarPanel.add(Box.createGlue());
        
        JButton logoutBtn = createLogoutButton();
        sidebarPanel.add(logoutBtn);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        return sidebarPanel;
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
            SessionManager.getInstance().clearSession();
            dispose();
            new LoginFrame().setVisible(true);
        });
        return btn;
    }
}
