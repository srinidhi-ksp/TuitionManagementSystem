package ui.admin;

import java.awt.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import dao.TeacherDAO;
import model.Teacher;
import model.User;

/**
 * Admin Profile Panel
 * Displays admin information (as teacher) with professional card-based layout
 */
public class AdminProfilePanel extends JPanel {

    private static final Color NAV_BG = new Color(2, 6, 23);
    private static final Color ACCENT = new Color(59, 130, 246);
    private static final Color PAGE_BG = new Color(248, 250, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_PRI = new Color(26, 35, 64);
    private static final Color TEXT_SEC = new Color(107, 122, 153);
    private static final Color TEXT_LIGHT = new Color(200, 210, 225);

    private User currentUser;
    private TeacherDAO teacherDAO;
    private Teacher teacher;

    public AdminProfilePanel(User user) {
        this.currentUser = user;
        this.teacherDAO = new TeacherDAO();
        
        System.out.println("[AdminProfilePanel] Initializing for User ID: " + user.getUserId());
        this.teacher = teacherDAO.getByUserId(user.getUserId());
        System.out.println("[AdminProfilePanel] Teacher data found: " + teacher);

        setLayout(new BorderLayout());
        setBackground(PAGE_BG);
        setBorder(new EmptyBorder(32, 36, 32, 36));

        initUI();
    }

    private void initUI() {
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setBackground(PAGE_BG);
        
        JLabel title = new JLabel("Admin Profile");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(TEXT_PRI);
        
        JLabel subtitle = new JLabel("View and manage your profile information");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_SEC);
        
        header.add(title);
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(PAGE_BG);
        mainPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        // Sections
        mainPanel.add(createProfileHeaderCard());
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(createDetailCard("🎓 Teacher Information", getTeacherInfoMap()));
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(createDetailCard("📞 Contact Details", getContactInfoMap()));

        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.setBorder(null);
        scroll.setBackground(PAGE_BG);
        scroll.getViewport().setBackground(PAGE_BG);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel createProfileHeaderCard() {
        JPanel card = new JPanel(new BorderLayout(25, 0));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true),
            new EmptyBorder(30, 30, 30, 30)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel avatar = new JLabel("👤");
        avatar.setFont(new Font("SansSerif", Font.PLAIN, 64));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(avatar, BorderLayout.WEST);

        JPanel info = new JPanel(new GridLayout(3, 1, 0, 5));
        info.setOpaque(false);

        // Display teacher name if available, fallback to user name
        String displayName = (teacher != null && teacher.getName() != null) ? teacher.getName() : safe(currentUser.getName());
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        nameLabel.setForeground(TEXT_PRI);

        JLabel emailLabel = new JLabel(safe(currentUser.getEmail()));
        emailLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        emailLabel.setForeground(TEXT_SEC);

        JLabel roleLabel = new JLabel("Role: ADMIN / TEACHER");
        roleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        roleLabel.setForeground(ACCENT);

        info.add(nameLabel);
        info.add(emailLabel);
        info.add(roleLabel);
        card.add(info, BorderLayout.CENTER);

        return card;
    }

    private JPanel createDetailCard(String title, java.util.Map<String, String> data) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true),
            new EmptyBorder(25, 25, 25, 25)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRI);
        titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 20);
        gbc.weightx = 0.3;
        gbc.gridy = 0;

        for (java.util.Map.Entry<String, String> entry : data.entrySet()) {
            gbc.gridx = 0;
            gbc.weightx = 0.3;
            JLabel keyLabel = new JLabel(entry.getKey());
            keyLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            keyLabel.setForeground(TEXT_SEC);
            grid.add(keyLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.7;
            JLabel valLabel = new JLabel(safe(entry.getValue()));
            valLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
            valLabel.setForeground(TEXT_PRI);
            grid.add(valLabel, gbc);

            gbc.gridy++;
        }

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private java.util.Map<String, String> getTeacherInfoMap() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        map.put("Teacher ID:", currentUser.getUserId());
        map.put("Specialization:", teacher != null ? teacher.getSpecialization() : "N/A");
        map.put("Qualifications:", (teacher != null && teacher.getQualifications() != null) ? String.join(", ", teacher.getQualifications()) : "N/A");
        
        // Calculate Experience accurately using java.time
        String experience = "N/A";
        if (currentUser.getCreatedAt() != null) {
            java.time.LocalDate joinDate = currentUser.getCreatedAt().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.Period period = java.time.Period.between(joinDate, today);

            int years = period.getYears();
            int months = period.getMonths();
            int days = period.getDays();

            if (years > 0) {
                experience = years + (years == 1 ? " year" : " years");
            } else if (months > 0) {
                experience = months + (months == 1 ? " month" : " months");
            } else {
                experience = days + (days == 1 ? " day" : " days");
            }
        }
        map.put("Experience:", experience);

        map.put("Join Date:", (teacher != null && teacher.getJoinDate() != null) ? new java.text.SimpleDateFormat("dd MMM yyyy").format(teacher.getJoinDate()) : "N/A");
        return map;
    }

    private java.util.Map<String, String> getContactInfoMap() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        map.put("Phone:", (teacher != null && teacher.getPhone() != null) ? teacher.getPhone() : safe(currentUser.getPhone()));
        map.put("Email:", currentUser.getEmail());
        if (teacher != null && teacher.getCity() != null) {
            map.put("City:", teacher.getCity());
        }
        return map;
    }

    private String safe(String value) {
        return (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) ? "N/A" : value;
    }
}
