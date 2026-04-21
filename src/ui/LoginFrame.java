package ui;

import service.AuthService;
import model.User;
import ui.admin.AdminDashboard;
import util.SessionManager;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class LoginFrame extends JFrame {

    // ── Design tokens ──────────────────────────────────────────────────────────
    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color ACCENT_DARK = new Color(0, 102, 204);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);

    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;

    public LoginFrame() {
        setTitle("MRK Tuition – Login");
        setSize(900, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(buildLeftDecoPanel());
        root.add(buildRightLoginPanel());
        add(root, BorderLayout.CENTER);
    }

    // ── Left decorative panel ──────────────────────────────────────────────────
    private JPanel buildLeftDecoPanel() {
        JPanel panel = new JPanel(null) {   // absolute layout for geometric art
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();

                // Background gradient
                GradientPaint gp = new GradientPaint(0, 0, NAV_BG, w, h, new Color(26, 54, 112));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // Geometric blocks — colorful interlocking squares (EdTech branding style)
                drawBlock(g2, 0.05f, 0.10f, 0.18f, w, h, ACCENT, 0.9f);
                drawBlock(g2, 0.23f, 0.10f, 0.18f, w, h, new Color(52, 211, 153), 0.85f);
                drawBlock(g2, 0.05f, 0.28f, 0.18f, w, h, new Color(167, 139, 250), 0.85f);
                drawBlock(g2, 0.23f, 0.28f, 0.18f, w, h, new Color(251, 146, 60), 0.9f);

                drawBlock(g2, 0.50f, 0.12f, 0.14f, w, h, new Color(251, 146, 60), 0.7f);
                drawBlock(g2, 0.64f, 0.12f, 0.14f, w, h, ACCENT, 0.6f);

                drawBlock(g2, 0.50f, 0.60f, 0.22f, w, h, new Color(52, 211, 153), 0.5f);
                drawBlock(g2, 0.72f, 0.60f, 0.22f, w, h, new Color(167, 139, 250), 0.55f);

                // Large faint circle
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillOval(-80, h / 2 - 150, 380, 380);

                g2.dispose();
            }

            private void drawBlock(Graphics2D g2, float cx, float cy, float size, int w, int h, Color c, float alpha) {
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha * 255)));
                int bw = (int)(size * w);
                int x  = (int)(cx * w);
                int y  = (int)(cy * h);
                int bh = bw; // square
                g2.fillRoundRect(x, y, bw, bh, 14, 14);
            }
        };
        panel.setBackground(NAV_BG);

        // Overlay text at bottom
        JPanel textBox = new JPanel(new GridLayout(3, 1, 0, 6));
        textBox.setOpaque(false);
        textBox.setBounds(0, 0, 450, 160); // positioned later

        JLabel brand = new JLabel("MRK Tuition");
        brand.setFont(new Font("SansSerif", Font.BOLD, 32));
        brand.setForeground(Color.WHITE);
        brand.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel tagline = new JLabel("Empowering Education, Elevating Futures");
        tagline.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tagline.setForeground(new Color(255, 255, 255, 180));
        tagline.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel sub = new JLabel("Admin Management System");
        sub.setFont(new Font("SansSerif", Font.BOLD, 12));
        sub.setForeground(new Color(74, 144, 226, 220));
        sub.setHorizontalAlignment(SwingConstants.CENTER);

        textBox.add(brand);
        textBox.add(tagline);
        textBox.add(sub);

        // Use a layered pane trick: add via BorderLayout at south
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.setBorder(new EmptyBorder(0, 0, 40, 0));
        south.add(textBox);
        panel.setLayout(new BorderLayout());
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    // ── Right login panel ──────────────────────────────────────────────────────
    private JPanel buildRightLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CARD_BG);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setPreferredSize(new Dimension(340, 430));

        // Title
        JLabel titleLbl = new JLabel("Welcome back");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLbl.setForeground(TEXT_PRI);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel("Sign in to access your portal");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subLbl.setForeground(TEXT_SEC);
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(titleLbl);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(subLbl);
        card.add(Box.createRigidArea(new Dimension(0, 28)));

        // Username
        card.add(makeInputLabel("Username / Email"));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        usernameField = makeTextField("Enter your username");
        card.add(usernameField);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        // Password
        card.add(makeInputLabel("Password"));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        passwordField = new JPasswordField() {
            {
                setFont(new Font("SansSerif", Font.PLAIN, 13));
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(210, 220, 235), 1, true),
                    new EmptyBorder(10, 14, 10, 14)
                ));
                setPreferredSize(new Dimension(340, 44));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
                setForeground(TEXT_PRI);
                setCaretColor(ACCENT);
                setEchoChar('●');
            }
        };
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(passwordField);
        card.add(Box.createRigidArea(new Dimension(0, 16)));

        // Role
        card.add(makeInputLabel("Login As"));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        roleCombo = new JComboBox<>(new String[]{"Admin", "Teacher", "Student", "Parent"});
        roleCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        roleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        roleCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        roleCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 220, 235), 1, true),
            new EmptyBorder(2, 10, 2, 10)
        ));
        card.add(roleCombo);
        card.add(Box.createRigidArea(new Dimension(0, 28)));

        // Login button (pill-shaped)
        JButton loginBtn = new JButton("Sign In") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT_DARK : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        loginBtn.setContentAreaFilled(false);
        loginBtn.setOpaque(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("SansSerif", Font.BOLD, 15));
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginBtn.setFocusPainted(false);
        loginBtn.addActionListener(e -> performLogin());
        card.add(loginBtn);

        // Keyboard enter
        getRootPane().setDefaultButton(loginBtn);

        panel.add(card);
        return panel;
    }

    private JLabel makeInputLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(TEXT_SEC);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private JTextField makeTextField(String placeholder) {
        JTextField f = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(180, 190, 210));
                    g2.setFont(getFont().deriveFont(Font.PLAIN));
                    g2.drawString(placeholder, 14, getHeight() / 2 + 5);
                    g2.dispose();
                }
            }
        };
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 220, 235), 1, true),
            new EmptyBorder(10, 14, 10, 14)
        ));
        f.setPreferredSize(new Dimension(340, 44));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setForeground(TEXT_PRI);
        f.setCaretColor(ACCENT);
        f.setAlignmentX(Component.CENTER_ALIGNMENT);
        // Focus border highlight
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT, 2, true),
                    new EmptyBorder(9, 13, 9, 13)));
            }
            public void focusLost(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(210, 220, 235), 1, true),
                    new EmptyBorder(10, 14, 10, 14)));
            }
        });
        return f;
    }

    // ── Login logic ────────────────────────────────────────────────────────────
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role     = roleCombo.getSelectedItem().toString();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User user = new AuthService().login(username, password, role);
        if (user != null) {
            String sessionUserId = user.getUserId();
            String sessionUserName = user.getName() != null ? user.getName() : user.getUserId();

            if ("Student".equalsIgnoreCase(role)) {
                model.Student s = new dao.StudentDAO().getStudentByUserId(user.getUserId());
                if (s == null) s = new dao.StudentDAO().getStudentById(user.getUserId());
                if (s != null) {
                    sessionUserId = s.getUserId();
                    if (s.getName() != null && !s.getName().trim().isEmpty()) {
                        sessionUserName = s.getName();
                    }
                }
            } else if ("Teacher".equalsIgnoreCase(role)) {
                model.Teacher t = new dao.TeacherDAO().getTeacherById(user.getUserId());
                if (t != null) {
                    sessionUserId = t.getUserId();
                    if (t.getName() != null && !t.getName().trim().isEmpty()) {
                        sessionUserName = t.getName();
                    }
                }
            }

            SessionManager.getInstance().setSession(sessionUserId, user.getRole(), sessionUserName);
            openDashboard(user);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials. Please try again.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openDashboard(User user) {
        String role = user.getRole();
        if ("Admin".equalsIgnoreCase(role)) {
            new AdminDashboard().setVisible(true);
            dispose();
        } else if ("Student".equalsIgnoreCase(role)) {
            new ui.student.StudentDashboard(user).setVisible(true);
            dispose();
        } else if ("Teacher".equalsIgnoreCase(role)) {
            new ui.teacher.TeacherDashboard(user).setVisible(true);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, role + " portal coming soon!");
        }
    }
}