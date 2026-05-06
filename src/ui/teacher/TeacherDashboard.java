package ui.teacher;

import ui.LoginFrame;
import util.SessionManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TeacherDashboard extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sidebarPanel;

    // ── Theme constants (matching Admin Dashboard) ──
    private static final Color SIDEBAR_BG     = new Color(13, 27, 42);   // dark blue
    private static final Color SIDEBAR_HOVER  = new Color(27, 38, 59);   // lighter blue on hover
    private static final Color SIDEBAR_ACTIVE = new Color(37, 48, 69);   // active menu item
    private static final Color HEADER_BG      = new Color(13, 27, 42);   // dark blue header
    private static final Color CONTENT_BG     = new Color(248, 249, 250); // light grey content
    private static final Color ACCENT         = new Color(74, 144, 226);  // accent blue
    
    private JButton activeBtn = null;
    private model.User user;
    
    public TeacherDashboard(model.User user) {
        // Load full teacher profile to ensure correct teacher_id (T001) is used instead of login ID
        model.Teacher t = new dao.TeacherDAO().getTeacherById(user.getUserId());
        this.user = (t != null) ? t : user;
        
        setTitle("MRK Tuition - Teacher Workspace");
        setSize(1300, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(CONTENT_BG);

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(CONTENT_BG);

        // Sub-panels initialized passing the teacher object token where required
        mainContentPanel.add(new OverviewPanel(), "Dashboard");
        mainContentPanel.add(new MyBatchesPanel(user), "My Batches");
        mainContentPanel.add(new AttendanceModulePanel(), "Take Attendance");
        mainContentPanel.add(new TestsMarksPanel(user), "Tests & Marks");
        mainContentPanel.add(new SyllabusUpdatePanel(user), "Syllabus Progress");
        mainContentPanel.add(new StudentsListPanel(user), "My Students");
        mainContentPanel.add(new TeacherTimetablePanel(), "Timetable");
        mainContentPanel.add(new TeacherSalaryPanel(user), "My Salary");
        mainContentPanel.add(new ProfilePanel(user), "Profile");

        add(createTopNavbar(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);
    }

    private JPanel createTopNavbar() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(HEADER_BG);
        topPanel.setPreferredSize(new Dimension(0, 64));
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(27, 38, 59)));

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 16));
        logoPanel.setOpaque(false);
        JLabel logoIcon = new JLabel("T"); 
        logoIcon.setOpaque(true);
        logoIcon.setBackground(ACCENT);
        logoIcon.setForeground(Color.WHITE);
        logoIcon.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoIcon.setHorizontalAlignment(SwingConstants.CENTER);
        logoIcon.setPreferredSize(new Dimension(36, 36));
        logoIcon.setBorder(new EmptyBorder(0, 0, 0, 0));

        JLabel logoText = new JLabel("MRK Tuition Teacher Workspace");
        logoText.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logoText.setForeground(Color.WHITE);
        
        logoPanel.add(logoIcon);
        logoPanel.add(logoText);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 14));
        userPanel.setOpaque(false);
        
        String userName = SessionManager.getInstance().getUserName();
        JButton profileBtn = new JButton("🏫 " + (userName != null ? userName : "Teacher") + " ▾");
        profileBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        profileBtn.setForeground(Color.WHITE);
        profileBtn.setBackground(new Color(255, 255, 255, 10));
        profileBtn.setFocusPainted(false);
        profileBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 40), 1, true),
            BorderFactory.createEmptyBorder(8, 18, 8, 18)
        ));
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileBtn.setContentAreaFilled(false);
        
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBackground(Color.WHITE);
        
        JMenuItem profileItem = new JMenuItem("  My Profile  ");
        profileItem.setBackground(Color.WHITE);
        profileItem.addActionListener(e -> {
            cardLayout.show(mainContentPanel, "Profile");
        });
        
        JMenuItem logoutItem = new JMenuItem("  Logout  ");
        logoutItem.setBackground(Color.WHITE);
        logoutItem.setForeground(new Color(220, 50, 50));
        logoutItem.addActionListener(e -> {
            SessionManager.getInstance().clearSession();
            dispose();
            new LoginFrame().setVisible(true);
        });
        
        popupMenu.add(profileItem);
        popupMenu.addSeparator();
        popupMenu.add(logoutItem);
        
        profileBtn.addActionListener(e -> popupMenu.show(profileBtn, 0, profileBtn.getHeight()));

        userPanel.add(profileBtn);

        topPanel.add(logoPanel, BorderLayout.WEST);
        topPanel.add(userPanel, BorderLayout.EAST);

        return topPanel;
    }

    private JScrollPane createSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setPreferredSize(new Dimension(230, 0));
        sidebarPanel.setBackground(SIDEBAR_BG);
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(27, 38, 59)));

        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JLabel menuLabel = new JLabel("TEACHER OPERATIONS");
        menuLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        menuLabel.setForeground(new Color(255, 255, 255, 100));
        menuLabel.setBorder(new EmptyBorder(0, 20, 10, 0));
        menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarPanel.add(menuLabel);

        String[] menuItems = {
            "Dashboard", "My Batches", "Take Attendance", "Tests & Marks", 
            "Syllabus Progress", "My Students", "Timetable", "My Salary", "Profile"
        };
        String[] icons = {
            "\uD83C\uDFE0", "\uD83D\uDCCB", "\uD83D\uDCCA", "\uD83D\uDCDD", 
            "\uD83D\uDCC8", "\uD83D\uDC65", "\uD83D\uDCC5", "\uD83D\uDCB0", "\uD83D\uDC64"
        };

        for (int i = 0; i < menuItems.length; i++) {
            final String item = menuItems[i];
            String icon = icons[i];
            
            JButton btn = new JButton(icon + "    " + item) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (this == activeBtn) {
                        g2.setColor(SIDEBAR_ACTIVE);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        g2.setColor(ACCENT);
                        g2.fillRect(0, 0, 4, getHeight());
                    } else if (getModel().isRollover()) {
                        g2.setColor(SIDEBAR_HOVER);
                        g2.fillRect(0, 0, getWidth(), getHeight());
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            btn.setForeground(new Color(255, 255, 255, 180));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(230, 48));
            btn.setPreferredSize(new Dimension(230, 48));
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBorder(new EmptyBorder(0, 20, 0, 0));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (btn != activeBtn) {
                        btn.setBackground(SIDEBAR_HOVER);
                    }
                    btn.repaint();
                }
                public void mouseExited(MouseEvent e) {
                    if (btn != activeBtn) {
                        btn.setBackground(SIDEBAR_BG);
                    }
                    btn.repaint();
                }
            });

            btn.addActionListener(e -> {
                if (activeBtn != null) activeBtn.setForeground(new Color(255, 255, 255, 180));
                activeBtn = btn;
                btn.setForeground(Color.WHITE);
                cardLayout.show(mainContentPanel, item);
                sidebarPanel.repaint();
            });
            sidebarPanel.add(btn);

            if (activeBtn == null && item.equals("Dashboard")) {
                activeBtn = btn;
                btn.setForeground(Color.WHITE);
            }
        }
        
        sidebarPanel.add(Box.createGlue());

        // Logout button at bottom
        JButton logoutBtn = new JButton("🚪    Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logoutBtn.setForeground(new Color(255, 100, 100));
        logoutBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        logoutBtn.setMaximumSize(new Dimension(230, 48));
        logoutBtn.setPreferredSize(new Dimension(230, 48));
        logoutBtn.setOpaque(false);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setHorizontalAlignment(SwingConstants.LEFT);
        logoutBtn.setBorder(new EmptyBorder(0, 20, 0, 0));
        logoutBtn.addActionListener(e -> {
            SessionManager.getInstance().clearSession();
            dispose();
            new LoginFrame().setVisible(true);
        });
        sidebarPanel.add(logoutBtn);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JScrollPane sidebarScroll = new JScrollPane(sidebarPanel);
        sidebarScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.setBorder(null);

        return sidebarScroll;
    }
}
