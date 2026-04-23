package ui.admin;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SettingsFrame extends JPanel {

    // ── Design tokens (Dynamic) ────────────────────────────────────────────────
    private Color NAV_BG      = new Color(10, 27, 63);
    private Color ACCENT      = new Color(74, 144, 226);
    private Color ACCENT_DARK = new Color(0, 102, 204);
    private Color PAGE_BG     = util.ThemeManager.BACKGROUND; 
    private Color CARD_BG     = util.ThemeManager.CARD;
    private Color TEXT_PRI    = util.ThemeManager.TEXT_PRIMARY;
    private Color TEXT_SEC    = util.ThemeManager.TEXT_SECONDARY;
    private Color SUCCESS     = new Color(34, 197, 94);
    private Color DIVIDER     = util.ThemeManager.DIVIDER;
    private Color HOVER_BG    = util.ThemeManager.isDarkMode ? new Color(45, 45, 65) : new Color(241, 245, 249);
    private Color INPUT_BORDER= util.ThemeManager.DIVIDER;

    private void updateTokens() {
        PAGE_BG     = util.ThemeManager.BG;
        CARD_BG     = util.ThemeManager.CARD;
        TEXT_PRI    = util.ThemeManager.TEXT;
        TEXT_SEC    = util.ThemeManager.SUB_TEXT;
        DIVIDER     = util.ThemeManager.DIVIDER;
        HOVER_BG    = util.ThemeManager.isDarkMode ? new Color(45, 45, 65) : new Color(241, 245, 249);
        INPUT_BORDER= util.ThemeManager.DIVIDER;
    }

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
        header.setBorder(new EmptyBorder(32, 40, 16, 40));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 6));
        titles.setBackground(PAGE_BG);

        JLabel titleLbl = new JLabel("System Settings");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLbl.setForeground(TEXT_PRI);

        JLabel subLbl = new JLabel("Configure institution details, system preferences and notifications");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
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
        wrapper.setBorder(new EmptyBorder(0, 40, 40, 40));

        // Custom tab bar
        String[] tabNames = {"General", "System Info", "Preferences"};
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
        bar.setBackground(PAGE_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
            new EmptyBorder(0, 0, 0, 0)
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
            if (i < names.length - 1) bar.add(Box.createHorizontalStrut(16));
        }
        return bar;
    }

    private JButton createTabButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        setTabActive(btn, active);
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!btn.getForeground().equals(ACCENT)) {
                    btn.setForeground(TEXT_PRI);
                }
            }
            public void mouseExited(MouseEvent e) {
                if (!btn.getForeground().equals(ACCENT)) {
                    btn.setForeground(TEXT_SEC);
                }
            }
        });
        return btn;
    }

    private void setTabActive(JButton btn, boolean active) {
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, active ? 3 : 0, 0, ACCENT),
            new EmptyBorder(12, 16, 12, 16)
        ));
        btn.setBackground(PAGE_BG);
        btn.setForeground(active ? ACCENT : TEXT_SEC);
    }

    // ── General Tab ────────────────────────────────────────────────────────────
    private JPanel buildGeneralTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(PAGE_BG);

        JPanel card = buildCard();
        JScrollPane scroll = new JScrollPane(card);
        scroll.setBorder(new EmptyBorder(24, 0, 0, 0)); // Space above card
        scroll.getViewport().setBackground(PAGE_BG);
        scroll.setBackground(PAGE_BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Section: Institution Details
        card.add(sectionHeader("Institution Details",
            "Basic information shown throughout the system."));
        card.add(vgap(24));
        card.add(twoColRow(
            labeledInput("Institution Name", styledInput("MRK Tuition Centre")),
            labeledInput("Contact Email",    styledInput("admin@mrktuition.com"))
        ));
        card.add(vgap(16));
        card.add(twoColRow(
            labeledInput("Phone Number",   styledInput("+91 98765 43210")),
            labeledInput("Website",        styledInput("www.mrktuition.com"))
        ));
        card.add(vgap(16));
        card.add(oneColRow(labeledInput("Address",
            styledInput("123 Main Street, Chennai, Tamil Nadu – 600001"))));
        card.add(vgap(36));

        // Section: Academic Year
        card.add(sectionDivider());
        card.add(vgap(28));
        card.add(sectionHeader("Academic Year",
            "Define the current academic session."));
        card.add(vgap(24));
        String year = String.valueOf(LocalDate.now().getYear());
        card.add(twoColRow(
            labeledInput("Academic Year Start", styledInput(year + "-06-01")),
            labeledInput("Academic Year End",   styledInput((Integer.parseInt(year)+1) + "-03-31"))
        ));
        card.add(vgap(40));

        card.add(makeSaveButton("Save General Settings"));
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    // ── System Info Tab ────────────────────────────────────────────────────────
    private JPanel buildSystemInfoTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(PAGE_BG);

        JPanel card = buildCard();
        JScrollPane scroll = new JScrollPane(card);
        scroll.setBorder(new EmptyBorder(24, 0, 0, 0));
        scroll.getViewport().setBackground(PAGE_BG);
        scroll.setBackground(PAGE_BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        card.add(sectionHeader("System Information",
            "Runtime environment and database connection details."));
        card.add(vgap(24));

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
            card.add(vgap(12));
        }

        card.add(vgap(32));
        card.add(sectionDivider());
        card.add(vgap(28));

        // Admin Access Section
        card.add(sectionHeader("Admin Access",
            "Change the admin account password."));
        card.add(vgap(24));
        JPasswordField oldPass = styledPassword();
        JPasswordField newPass = styledPassword();
        JPasswordField cfmPass = styledPassword();
        card.add(twoColRow(
            labeledInput("Current Password",  oldPass),
            labeledInput("New Password",      newPass)
        ));
        card.add(vgap(16));
        card.add(oneColRow(labeledInput("Confirm New Password", cfmPass)));
        card.add(vgap(40));

        JButton changePwd = makeSaveButton("Update Password");
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
        scroll.setBorder(new EmptyBorder(24, 0, 0, 0));
        scroll.getViewport().setBackground(PAGE_BG);
        scroll.setBackground(PAGE_BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        card.add(sectionHeader("Display Preferences",
            "Customize the look and layout of the dashboard."));
        card.add(vgap(24));

        card.add(toggleRow("Dark Mode",
            "Switch to a dark theme for the entire dashboard", util.ThemeManager.isDarkMode, e -> {
                util.ThemeManager.toggleTheme();
                updateTokens(); // Refresh local tokens
                Window win = SwingUtilities.getWindowAncestor(this);
                if (win != null) {
                    util.ThemeUtil.apply(win);
                    SwingUtilities.updateComponentTreeUI(win);
                }
            }));
        card.add(vgap(16));
        card.add(toggleRow("Show Icon Labels",
            "Display text labels beside sidebar icons", true, null));
        card.add(vgap(16));
        card.add(toggleRow("Compact Table Rows",
            "Reduce row height in all data tables", false, null));
        card.add(vgap(36));

        card.add(sectionDivider());
        card.add(vgap(28));
        card.add(sectionHeader("Notification Preferences",
            "Control which notifications the admin receives."));
        card.add(vgap(24));

        card.add(toggleRow("Fee Due Alerts",
            "Email notification when student fees are due", true, null));
        card.add(vgap(16));
        card.add(toggleRow("Attendance Summary SMS",
            "Send daily attendance summary via SMS", false, null));
        card.add(vgap(16));
        card.add(toggleRow("New Enrollment Alerts",
            "Notify admin when a new student is enrolled", true, null));
        card.add(vgap(40));

        card.add(makeSaveButton("Save Preferences"));
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────
    private JPanel buildCard() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Soft shadow
                g2.setColor(new Color(0, 0, 0, 8));
                g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 6, 16, 16);
                g2.setColor(new Color(0, 0, 0, 4));
                g2.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 3, 16, 16);
                
                // Card background
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() - 2, 12, 12);
                
                // Card border
                g2.setColor(DIVIDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 12, 12);
                
                g2.dispose();
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBackground(CARD_BG);
        p.setBorder(new EmptyBorder(32, 40, 40, 40));
        return p;
    }

    private JPanel sectionHeader(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(CARD_BG);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 18));
        t.setForeground(TEXT_PRI);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s = new JLabel(subtitle);
        s.setFont(new Font("SansSerif", Font.PLAIN, 14));
        s.setForeground(TEXT_SEC);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setBorder(new EmptyBorder(6, 0, 0, 0));

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
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(DIVIDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBackground(CARD_BG);
        row.setBorder(new EmptyBorder(14, 20, 14, 20));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(TEXT_SEC);

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.PLAIN, 14));
        val.setForeground(TEXT_PRI);

        row.add(lbl);
        row.add(val);
        
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(HOVER_BG); row.repaint(); }
            public void mouseExited(MouseEvent e)  { row.setBackground(CARD_BG); row.repaint(); }
        });
        return row;
    }

    private JPanel toggleRow(String label, String desc, boolean on, java.awt.event.ActionListener al) {
        JPanel row = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(DIVIDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBackground(CARD_BG);
        row.setBorder(new EmptyBorder(14, 20, 14, 20));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JPanel textBlock = new JPanel(new GridLayout(2, 1, 0, 4));
        textBlock.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(TEXT_PRI);
        JLabel dsc = new JLabel(desc);
        dsc.setFont(new Font("SansSerif", Font.PLAIN, 13));
        dsc.setForeground(TEXT_SEC);
        textBlock.add(lbl); textBlock.add(dsc);

        // DO NOT REDESIGN TOGGLE SWITCHES
        JToggleButton toggle = buildToggle(on, al);
        
        row.add(textBlock, BorderLayout.CENTER);
        row.add(toggle, BorderLayout.EAST);

        // Hover
        row.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { row.setBackground(HOVER_BG); row.repaint(); }
            public void mouseExited(MouseEvent e)  { row.setBackground(CARD_BG); row.repaint(); }
        });
        return row;
    }

    // STRICT CONSTRAINT: KEEP TOGGLE EXACTLY AS IS
    private JToggleButton buildToggle(boolean on, java.awt.event.ActionListener al) {
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
        if (al != null) btn.addActionListener(al);
        btn.addActionListener(e -> btn.setText(btn.isSelected() ? "ON" : "OFF"));
        return btn;
    }

    private JTextField styledInput(String text) {
        JTextField f = new JTextField(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                super.paintComponent(g);
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? ACCENT : INPUT_BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                g2.dispose();
            }
        };
        f.setOpaque(false);
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setForeground(TEXT_PRI);
        f.setBorder(new EmptyBorder(10, 14, 10, 14));
        f.setPreferredSize(new Dimension(0, 42));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.repaint(); }
            public void focusLost(FocusEvent e) { f.repaint(); }
        });
        return f;
    }

    private JPasswordField styledPassword() {
        JPasswordField f = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                super.paintComponent(g);
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? ACCENT : INPUT_BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                g2.dispose();
            }
        };
        f.setOpaque(false);
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setForeground(TEXT_PRI);
        f.setEchoChar('●');
        f.setBorder(new EmptyBorder(10, 14, 10, 14));
        f.setPreferredSize(new Dimension(0, 42));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.repaint(); }
            public void focusLost(FocusEvent e) { f.repaint(); }
        });
        return f;
    }

    private JPanel labeledInput(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(CARD_BG);
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(TEXT_SEC);
        p.add(lbl, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JPanel twoColRow(JPanel col1, JPanel col2) {
        JPanel row = new JPanel(new GridLayout(1, 2, 24, 0));
        row.setBackground(CARD_BG);
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        row.add(col1); row.add(col2);
        return row;
    }

    private JPanel oneColRow(JPanel col) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(CARD_BG);
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        row.add(col, BorderLayout.CENTER);
        return row;
    }

    private JPanel vgap(int h) {
        JPanel p = new JPanel();
        p.setBackground(CARD_BG);
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        p.setPreferredSize(new Dimension(0, h));
        return p;
    }

    private JButton makeSaveButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base  = ACCENT;
                Color hover = ACCENT_DARK;
                Color mixed = getModel().isRollover() ? hover : base;
                g2.setColor(mixed);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8); // 8px radius
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setPreferredSize(new Dimension(220, 44));
        btn.setMaximumSize(new Dimension(220, 44));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.addActionListener(e ->
            JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Saved",
                JOptionPane.INFORMATION_MESSAGE));
        return btn;
    }
}
