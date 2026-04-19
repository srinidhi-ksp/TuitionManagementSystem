package ui.admin;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SettingsFrame extends JPanel {

    // ── Design tokens ──────────────────────────────────────────────────────────
    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color ACCENT_DARK = new Color(0, 102, 204);
    private static final Color ACCENT_LIGHT= new Color(232, 242, 255);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);
    private static final Color SUCCESS     = new Color(34, 197, 94);
    private static final Color DIVIDER     = new Color(230, 235, 245);
    private static final Color INPUT_BORDER= new Color(200, 212, 228);

    public SettingsFrame() {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    // ── Header ─────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PAGE_BG);
        header.setBorder(new EmptyBorder(28, 36, 12, 36));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(PAGE_BG);

        JLabel titleLbl = new JLabel("⚙  System Settings");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLbl.setForeground(TEXT_PRI);

        JLabel subLbl = new JLabel("Configure institution details, system preferences and notifications");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subLbl.setForeground(TEXT_SEC);

        titles.add(titleLbl);
        titles.add(subLbl);
        header.add(titles, BorderLayout.WEST);
        return header;
    }

    // ── Custom tabbed content ──────────────────────────────────────────────────
    private JPanel buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(PAGE_BG);
        wrapper.setBorder(new EmptyBorder(0, 36, 36, 36));

        // Custom tab bar
        String[] tabNames = {"⚙  General", "🖥  System Info", "🎨  Preferences"};
        JPanel[] tabPanels = {buildGeneralTab(), buildSystemInfoTab(), buildPreferencesTab()};

        CardLayout cl = new CardLayout();
        JPanel contentArea = new JPanel(cl);
        contentArea.setBackground(PAGE_BG);
        for (int i = 0; i < tabPanels.length; i++) {
            contentArea.add(tabPanels[i], tabNames[i]);
        }

        JPanel tabBar = buildTabBar(tabNames, contentArea, cl);

        wrapper.add(tabBar, BorderLayout.NORTH);
        wrapper.add(contentArea, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildTabBar(String[] names, JPanel contentArea, CardLayout cl) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setBackground(CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 0, 1, DIVIDER),
            new EmptyBorder(12, 20, 0, 20)
        ));

        JButton[] tabs = new JButton[names.length];
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            JButton tab = createTabButton(names[i], i == 0);
            tab.addActionListener(e -> {
                for (JButton b : tabs) setTabActive(b, false);
                setTabActive(tab, true);
                cl.show(contentArea, names[idx]);
            });
            tabs[i] = tab;
            bar.add(tab);
            if (i < names.length - 1) bar.add(Box.createHorizontalStrut(4));
        }
        return bar;
    }

    private JButton createTabButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, active ? 3 : 0, 0, ACCENT),
            new EmptyBorder(8, 18, 10, 18)
        ));
        btn.setBackground(active ? ACCENT_LIGHT : CARD_BG);
        btn.setForeground(active ? ACCENT : TEXT_SEC);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!btn.getForeground().equals(ACCENT)) {
                    btn.setBackground(new Color(245, 247, 252));
                }
            }
            public void mouseExited(MouseEvent e) {
                if (!btn.getForeground().equals(ACCENT)) {
                    btn.setBackground(CARD_BG);
                }
            }
        });
        return btn;
    }

    private void setTabActive(JButton btn, boolean active) {
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, active ? 3 : 0, 0, ACCENT),
            new EmptyBorder(8, 18, 10, 18)
        ));
        btn.setBackground(active ? ACCENT_LIGHT : CARD_BG);
        btn.setForeground(active ? ACCENT : TEXT_SEC);
    }

    // ── General Tab ────────────────────────────────────────────────────────────
    private JPanel buildGeneralTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(PAGE_BG);

        JPanel card = buildCard();
        JScrollPane scroll = new JScrollPane(card);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, DIVIDER));
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Section: Institution Details
        card.add(sectionHeader("🏫  Institution Details",
            "Basic information shown throughout the system."));
        card.add(vgap(16));
        card.add(twoColRow(
            labeledInput("Institution Name", styledInput("MRK Tuition Centre")),
            labeledInput("Contact Email",    styledInput("admin@mrktuition.com"))
        ));
        card.add(vgap(14));
        card.add(twoColRow(
            labeledInput("Phone Number",   styledInput("+91 98765 43210")),
            labeledInput("Website",        styledInput("www.mrktuition.com"))
        ));
        card.add(vgap(14));
        card.add(oneColRow(labeledInput("Address",
            styledInput("123 Main Street, Chennai, Tamil Nadu – 600001"))));
        card.add(vgap(28));

        // Section: Academic Year
        card.add(sectionDivider());
        card.add(vgap(20));
        card.add(sectionHeader("📅  Academic Year",
            "Define the current academic session."));
        card.add(vgap(16));
        String year = String.valueOf(LocalDate.now().getYear());
        card.add(twoColRow(
            labeledInput("Academic Year Start", styledInput(year + "-06-01")),
            labeledInput("Academic Year End",   styledInput((Integer.parseInt(year)+1) + "-03-31"))
        ));
        card.add(vgap(32));

        card.add(makeSaveButton("💾  Save General Settings"));
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    // ── System Info Tab ────────────────────────────────────────────────────────
    private JPanel buildSystemInfoTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(PAGE_BG);

        JPanel card = buildCard();
        JScrollPane scroll = new JScrollPane(card);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, DIVIDER));
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        card.add(sectionHeader("🖥  System Information",
            "Runtime environment and database connection details."));
        card.add(vgap(20));

        // Info rows
        String[][] sysInfo = {
            {"Application",   "MRK Tuition Management System"},
            {"Version",       "2.0.0 (April 2026)"},
            {"Java Version",  System.getProperty("java.version")},
            {"OS",            System.getProperty("os.name") + " " + System.getProperty("os.arch")},
            {"Database",      "MongoDB (Local)"},
            {"DB Name",       "tuitionManagementSystem"},
            {"Last Login",    LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))},
        };
        for (String[] row : sysInfo) {
            card.add(infoRow(row[0], row[1]));
            card.add(vgap(8));
        }

        card.add(vgap(24));
        card.add(sectionDivider());
        card.add(vgap(20));

        // Admin Access Section
        card.add(sectionHeader("🔐  Admin Access",
            "Change the admin account password."));
        card.add(vgap(16));
        JPasswordField oldPass = styledPassword();
        JPasswordField newPass = styledPassword();
        JPasswordField cfmPass = styledPassword();
        card.add(twoColRow(
            labeledInput("Current Password",  oldPass),
            labeledInput("New Password",      newPass)
        ));
        card.add(vgap(14));
        card.add(oneColRow(labeledInput("Confirm New Password", cfmPass)));
        card.add(vgap(28));

        JButton changePwd = makeSaveButton("🔑  Update Password");
        changePwd.addActionListener(null); // Override the default actionListener
        changePwd.addActionListener(e -> {
            String np = new String(newPass.getPassword());
            String cp = new String(cfmPass.getPassword());
            if (!np.equals(cp)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Error",
                    JOptionPane.ERROR_MESSAGE);
            } else if (np.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters.", "Error",
                    JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Password updated successfully!", "Updated",
                    JOptionPane.INFORMATION_MESSAGE);
                oldPass.setText(""); newPass.setText(""); cfmPass.setText("");
            }
        });
        card.add(changePwd);

        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    // ── Preferences Tab ────────────────────────────────────────────────────────
    private JPanel buildPreferencesTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(PAGE_BG);

        JPanel card = buildCard();
        JScrollPane scroll = new JScrollPane(card);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, DIVIDER));
        scroll.getViewport().setBackground(CARD_BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        card.add(sectionHeader("🎨  Display Preferences",
            "Customize the look and layout of the dashboard."));
        card.add(vgap(16));

        card.add(toggleRow("🌙  Dark Mode",
            "Switch to a dark theme for the entire dashboard", false));
        card.add(vgap(12));
        card.add(toggleRow("📌  Show Icon Labels",
            "Display text labels beside sidebar icons", true));
        card.add(vgap(12));
        card.add(toggleRow("📊  Compact Table Rows",
            "Reduce row height in all data tables", false));
        card.add(vgap(28));

        card.add(sectionDivider());
        card.add(vgap(20));
        card.add(sectionHeader("🔔  Notification Preferences",
            "Control which notifications the admin receives."));
        card.add(vgap(16));

        card.add(toggleRow("📧  Fee Due Alerts",
            "Email notification when student fees are due", true));
        card.add(vgap(12));
        card.add(toggleRow("📱  Attendance Summary SMS",
            "Send daily attendance summary via SMS", false));
        card.add(vgap(12));
        card.add(toggleRow("🎓  New Enrollment Alerts",
            "Notify admin when a new student is enrolled", true));
        card.add(vgap(32));

        card.add(makeSaveButton("💾  Save Preferences"));
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────
    private JPanel buildCard() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD_BG);
        p.setBorder(new EmptyBorder(28, 32, 32, 32));
        return p;
    }

    private JPanel sectionHeader(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD_BG);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 16));
        t.setForeground(TEXT_PRI);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("SansSerif", Font.PLAIN, 12));
        s.setForeground(TEXT_SEC);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setBorder(new EmptyBorder(4, 0, 0, 0));

        p.add(t);
        p.add(s);
        return p;
    }

    private JPanel sectionDivider() {
        JPanel p = new JPanel();
        p.setBackground(DIVIDER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JPanel infoRow(String label, String value) {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setBackground(new Color(248, 250, 253));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER, 1, true),
            new EmptyBorder(10, 16, 10, 16)
        ));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(TEXT_SEC);

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.PLAIN, 13));
        val.setForeground(TEXT_PRI);

        row.add(lbl);
        row.add(val);
        return row;
    }

    private JPanel toggleRow(String label, String desc, boolean on) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setBackground(CARD_BG);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DIVIDER, 1, true),
            new EmptyBorder(12, 16, 12, 16)
        ));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel textBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        textBlock.setBackground(CARD_BG);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(TEXT_PRI);
        JLabel dsc = new JLabel(desc);
        dsc.setFont(new Font("SansSerif", Font.PLAIN, 11));
        dsc.setForeground(TEXT_SEC);
        textBlock.add(lbl); textBlock.add(dsc);

        JToggleButton toggle = buildToggle(on);
        row.add(textBlock, BorderLayout.CENTER);
        row.add(toggle, BorderLayout.EAST);

        // Hover
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(new Color(248, 250, 253)); textBlock.setBackground(new Color(248, 250, 253)); }
            public void mouseExited(MouseEvent e)  { row.setBackground(CARD_BG); textBlock.setBackground(CARD_BG); }
        });
        return row;
    }

    private JToggleButton buildToggle(boolean on) {
        JToggleButton btn = new JToggleButton(on ? "ON" : "OFF", on) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected() ? ACCENT : new Color(200, 210, 225));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                // Circle knob
                int kx = isSelected() ? getWidth() - getHeight() + 2 : 2;
                g2.setColor(Color.WHITE);
                g2.fillOval(kx, 2, getHeight() - 4, getHeight() - 4);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(54, 26));
        btn.setMinimumSize(new Dimension(54, 26));
        btn.setMaximumSize(new Dimension(54, 26));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addItemListener(e -> btn.repaint());
        return btn;
    }

    private JTextField styledInput(String text) {
        JTextField f = new JTextField(text);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setForeground(TEXT_PRI);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(INPUT_BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        f.setPreferredSize(new Dimension(0, 40));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT, 2, true),
                    new EmptyBorder(7, 11, 7, 11)));
            }
            public void focusLost(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(INPUT_BORDER, 1, true),
                    new EmptyBorder(8, 12, 8, 12)));
            }
        });
        return f;
    }

    private JPasswordField styledPassword() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setForeground(TEXT_PRI);
        f.setEchoChar('●');
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(INPUT_BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        ));
        f.setPreferredSize(new Dimension(0, 40));
        return f;
    }

    private JPanel labeledInput(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(CARD_BG);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(TEXT_SEC);
        p.add(lbl, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JPanel twoColRow(JPanel col1, JPanel col2) {
        JPanel row = new JPanel(new GridLayout(1, 2, 20, 0));
        row.setBackground(CARD_BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        row.add(col1); row.add(col2);
        return row;
    }

    private JPanel oneColRow(JPanel col) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(CARD_BG);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        row.add(col, BorderLayout.CENTER);
        return row;
    }

    private JPanel vgap(int h) {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        p.setPreferredSize(new Dimension(0, h));
        return p;
    }

    private JButton makeSaveButton(String text) {
        JButton btn = new JButton(text) {
            private float hoverAlpha = 0f;
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base  = ACCENT;
                Color hover = ACCENT_DARK;
                Color mixed = getModel().isRollover() ? hover : base;
                g2.setColor(mixed);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(240, 42));
        btn.setMaximumSize(new Dimension(240, 42));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.addActionListener(e ->
            JOptionPane.showMessageDialog(this, "✅  Settings saved successfully!", "Saved",
                JOptionPane.INFORMATION_MESSAGE));
        return btn;
    }
}
