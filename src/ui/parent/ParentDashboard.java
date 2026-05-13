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
import java.awt.GridBagLayout;
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
import util.SessionManager;
import util.ThemeManager;

/**
 * Parent Portal Dashboard - Professional Card-based UI
 */
public class ParentDashboard extends JFrame {

    private static final Color NAV_BG      = new Color(2, 6, 23); // Consistent with Admin
    private static final Color ACCENT      = new Color(59, 130, 246);
    private static final Color PAGE_BG     = new Color(248, 250, 252);
    private static final Color TEXT_PRI    = new Color(249, 250, 251);
    private static final Color TEXT_SEC    = new Color(156, 163, 175);

    private CardLayout cardLayout;
    private JPanel mainContentPanel;
    private JPanel sidebarPanel;
    private JButton activeBtn = null;

    public ParentDashboard(User user) {
        // Fix for JPopupMenu overlapping in some environments
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        setTitle("MRK Tuition – Parent Portal");
        setSize(1400, 850);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setBackground(PAGE_BG);

        // Register functional parent panels
        mainContentPanel.add(new ParentOverviewPanel(), "Dashboard Overview");
        mainContentPanel.add(new ParentMarksPanel(), "Marks & Performance");
        mainContentPanel.add(new ParentFeesPanel(), "Fees & Payments");
        mainContentPanel.add(new ParentAttendancePanel(), "Attendance");
        mainContentPanel.add(new ParentProfilePanel(), "Profile");
        
        // Notifications
        mainContentPanel.add(new ParentNotificationPanel(), "Notifications");

        add(createTopNavbar(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);
        add(mainContentPanel, BorderLayout.CENTER);
        
        checkAndNotifyOverdueFees();
    }

    private void checkAndNotifyOverdueFees() {
        new javax.swing.SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                try {
                    com.mongodb.client.MongoDatabase database = db.DBConnection.getDatabase();
                    com.mongodb.client.FindIterable<org.bson.Document> pendingPayments = 
                        database.getCollection("payments").find(
                            com.mongodb.client.model.Filters.in(
                                "status", "PENDING", "PARTIAL"
                            )
                        );
                    
                    for (org.bson.Document payment : pendingPayments) {
                        Integer enrollmentId = payment.getInteger("enrollment_id");
                        if (enrollmentId == null) continue; // safety check
                        
                        double amount = payment.get("amount") instanceof Number ?
                                        ((Number) payment.get("amount")).doubleValue() : 0.0;
                        
                        org.bson.Document enrollment = database.getCollection("enrollments").find(
                            new org.bson.Document("_id", enrollmentId)
                        ).first();
                        if (enrollment == null) continue;
                        
                        String studentId = enrollment.getString("student_id");
                        Integer batchId = enrollment.getInteger("batch_id");
                        if (batchId == null) continue;
                        
                        org.bson.Document studentDoc = database.getCollection("students").find(
                            new org.bson.Document("_id", studentId)
                        ).first();
                        if (studentDoc == null) continue;
                        
                        String parentId = studentDoc.getString("parent_user_id");
                        if (parentId == null) {
                            org.bson.Document parentEmbed = (org.bson.Document) studentDoc.get("parent");
                            if (parentEmbed == null) continue;
                            parentId = parentEmbed.getString("parent_id");
                        }
                        String studentName = studentDoc.getString("full_name");
                        
                        org.bson.Document batch = database.getCollection("batches").find(
                            new org.bson.Document("_id", batchId)
                        ).first();
                        String batchName = batch != null ? 
                                           batch.getString("batch_name") : "your batch";
                        
                        service.NotificationService.getInstance().notifyFeeOverdue(
                            parentId, studentId, studentName, batchName, amount
                        );
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    private JPanel createTopNavbar() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(NAV_BG);
        topPanel.setPreferredSize(new Dimension(0, 68));
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(31, 41, 55)));

        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 28, 18));
        logoPanel.setOpaque(false);
        JLabel logoText = new JLabel("MRK TUITION PORTAL");
        logoText.setFont(new Font("SansSerif", Font.BOLD, 20));
        logoText.setForeground(Color.WHITE);
        logoPanel.add(logoText);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 14));
        userPanel.setOpaque(false);

        String userName = SessionManager.getInstance().getUserName();
        JButton profileBtn = new JButton("👤 " + (userName != null ? userName : "Parent") + " ▾");
        profileBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        profileBtn.setForeground(Color.WHITE);
        profileBtn.setBackground(new Color(255, 255, 255, 10));
        profileBtn.setFocusPainted(false);
        profileBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 40), 1, true),
            new EmptyBorder(8, 18, 8, 18)
        ));
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPopupMenu popup = new JPopupMenu();
        JMenuItem logoutItem = new JMenuItem("  Logout  ");
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
        sidebarPanel.setPreferredSize(new Dimension(260, 0));
        sidebarPanel.setBackground(NAV_BG);
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(31, 41, 55)));

        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        addSidebarItem("🏠", "Dashboard Overview");
        addSidebarItem("📈", "Marks & Performance");
        addSidebarItem("💰", "Fees & Payments");
        addSidebarItem("🗓️", "Attendance");
        addSidebarItem("🔔", "Notifications");
        addSidebarItem("👤", "Profile");

        sidebarPanel.add(Box.createGlue());
        
        JButton logoutBtn = createLogoutButton();
        sidebarPanel.add(logoutBtn);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        return sidebarPanel;
    }

    private void addSidebarItem(String icon, String label) {
        JButton btn = new JButton(icon + "    " + label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (this == activeBtn) {
                    g2.setColor(new Color(59, 130, 246, 40));
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
        btn.setForeground(label.equals("Dashboard Overview") && activeBtn == null ? Color.WHITE : TEXT_SEC);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(260, 52));
        btn.setPreferredSize(new Dimension(260, 52));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 24, 0, 10)); // Adjusted padding

        btn.addActionListener(e -> {
            if (activeBtn != null) activeBtn.setForeground(TEXT_SEC);
            activeBtn = btn;
            btn.setForeground(Color.WHITE);
            cardLayout.show(mainContentPanel, label);
            repaint();
        });

        sidebarPanel.add(btn);
        if (activeBtn == null && label.equals("Dashboard Overview")) {
            activeBtn = btn;
            btn.setForeground(Color.WHITE);
        }
    }

    private JButton createLogoutButton() {
        JButton btn = new JButton("🚪    Logout");
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(new Color(239, 68, 68));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(260, 52));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 32, 0, 0));
        btn.addActionListener(e -> { dispose(); new LoginFrame().setVisible(true); });
        return btn;
    }

}
