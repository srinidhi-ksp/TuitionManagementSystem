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
    }

    private JPanel createTopNavbar() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230,230,230)));

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        logoPanel.setBackground(Color.WHITE);
        JLabel logoIcon = new JLabel("S"); 
        logoIcon.setOpaque(true);
        logoIcon.setBackground(brandColor);
        logoIcon.setForeground(Color.WHITE);
        logoIcon.setFont(new Font("Arial", Font.BOLD, 20));
        logoIcon.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel logoText = new JLabel("MRK Tuition Student Portal");
        logoText.setFont(new Font("Serif", Font.BOLD, 24));
        
        logoPanel.add(logoIcon);
        logoPanel.add(logoText);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 15));
        userPanel.setBackground(Color.WHITE);
        
        String userName = SessionManager.getInstance().getUserName();
        JButton profileBtn = new JButton("👤 " + (userName != null ? userName : "Student") + " ▾");
        profileBtn.setBackground(new Color(245, 245, 245));
        profileBtn.setForeground(Color.DARK_GRAY);
        profileBtn.setFont(new Font("Arial", Font.BOLD, 14));
        profileBtn.setFocusPainted(false);
        profileBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
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

    private JPanel createSidebar() {
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setPreferredSize(new Dimension(220, 0));
        sidebarPanel.setBackground(sidebarBg);
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(230,230,230)));

        JLabel menuLabel = new JLabel("STUDENT MENU");
        menuLabel.setFont(new Font("Arial", Font.BOLD, 12));
        menuLabel.setForeground(Color.GRAY);
        menuLabel.setBorder(new EmptyBorder(20, 20, 10, 0));
        menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarPanel.add(menuLabel);

        String[] menuItems = {
            "Dashboard", "My Subjects", "My Batches", "Syllabus Progress", 
            "Attendance", "Fees & Payments", "Profile"
        };

        for (String item : menuItems) {
            JButton btn = new JButton(" " + item);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(200, 45));
            btn.setBackground(sidebarBg);
            btn.setForeground(Color.DARK_GRAY);
            btn.setFont(new Font("Arial", Font.BOLD, 14));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBorder(new EmptyBorder(10, 20, 10, 20));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btn.addActionListener(e -> {
                for (Component comp : sidebarPanel.getComponents()) {
                    if (comp instanceof JButton) {
                        comp.setBackground(sidebarBg);
                        ((JButton)comp).setForeground(Color.DARK_GRAY);
                    }
                }
                btn.setBackground(new Color(235, 245, 255));
                btn.setForeground(brandColor);
                cardLayout.show(mainContentPanel, item);
                
                // Trigger refresh dynamically if panels have a reload mechanism (for now let them auto-load)
            });
            sidebarPanel.add(btn);
        }
        
        Component firstBtn = sidebarPanel.getComponent(1);
        if(firstBtn instanceof JButton) {
            firstBtn.setBackground(new Color(235, 245, 255));
            ((JButton)firstBtn).setForeground(brandColor);
        }

        return sidebarPanel;
    }
}
