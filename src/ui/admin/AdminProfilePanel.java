package ui.admin;

import java.awt.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import dao.TeacherDAO;
import model.Teacher;
import model.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import java.util.Date;

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
        
        loadAdminData();

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
        String displayName = firstAvailable(
            teacher != null ? teacher.getName() : null,
            currentUser.getName(),
            currentUser.getEmail()
        );
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        nameLabel.setForeground(TEXT_PRI);

        JLabel emailLabel = new JLabel(firstAvailable(currentUser.getEmail(), teacher != null ? teacher.getEmail() : null));
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
        
        // Fetch real data from the teacher object if available
        String teacherId = (teacher != null && teacher.getUserId() != null) ? teacher.getUserId() : currentUser.getUserId();
        map.put("Teacher ID:", teacherId);
        
        String spec = (teacher != null && teacher.getSpecialization() != null) ? teacher.getSpecialization() : "Not Available";
        map.put("Specialization:", spec);
        
        String quals = (teacher != null && teacher.getQualifications() != null && !teacher.getQualifications().isEmpty()) 
                       ? String.join(", ", teacher.getQualifications()) : firstAvailable(teacher != null ? teacher.getHighestDegree() : null);
        map.put("Qualifications:", quals);
        
        // Use experience_years from database if available
        String experience = "Not Available";
        if (teacher != null && teacher.getExperience() > 0) {
            experience = teacher.getExperience() + " years";
        } else if (currentUser.getCreatedAt() != null) {
            // Fallback calculation from join date if experience_years is missing
            java.time.LocalDate joinDate = currentUser.getCreatedAt().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.Period period = java.time.Period.between(joinDate, today);

            int years = period.getYears();
            if (years > 0) {
                experience = years + (years == 1 ? " year" : " years");
            } else if (period.getMonths() > 0) {
                experience = period.getMonths() + (period.getMonths() == 1 ? " month" : " months");
            } else {
                experience = period.getDays() + (period.getDays() == 1 ? " day" : " days");
            }
        }
        map.put("Experience:", experience);

        String joinDateStr = "Not Available";
        if (teacher != null && teacher.getJoinDate() != null && !teacher.getJoinDate().equals("-")) {
            joinDateStr = teacher.getJoinDate();
        } else if (currentUser.getCreatedAt() != null) {
            joinDateStr = new java.text.SimpleDateFormat("dd-MM-yyyy hh:mm a").format(currentUser.getCreatedAt());
        }
        map.put("Join Date:", joinDateStr);
        
        return map;
    }

    private java.util.Map<String, String> getContactInfoMap() {
        java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
        map.put("Phone:", firstAvailable(teacher != null ? teacher.getPhone() : null, currentUser.getPhone()));
        map.put("Email:", firstAvailable(teacher != null ? teacher.getEmail() : null, currentUser.getEmail()));
        if (teacher != null && teacher.getCity() != null) {
            map.put("City:", teacher.getCity());
        }
        return map;
    }

    private String safe(String value) {
        return (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null") || value.equals("N/A") || value.equals("-"))
            ? "Not Available" : value;
    }

    private String firstAvailable(String... values) {
        for (String value : values) {
            String safeValue = safe(value);
            if (!"Not Available".equals(safeValue)) return safeValue;
        }
        return "Not Available";
    }

    private void loadAdminData() {
        if (currentUser == null) return;
        
        String adminId = currentUser.getUserId();
        System.out.println("[AdminProfilePanel] 🔍 Full merge for Admin: " + adminId);
        
        try {
            com.mongodb.client.MongoDatabase database = db.DBConnection.getDatabase();
            
            // 1. Get User Data
            MongoCollection<Document> userCol = database.getCollection("users");
            Document user = userCol.find(Filters.eq("_id", adminId)).first();

            if (user != null) {
                currentUser.setEmail(user.getString("email"));
                String fullName = user.getString("full_name");
                if (fullName == null) fullName = user.getString("name");
                currentUser.setName(fullName);
                
                Object phoneObj = user.get("phone");
                if (phoneObj == null) {
                    List<String> phones = user.getList("phones", String.class);
                    if (phones != null && !phones.isEmpty()) phoneObj = phones.get(0);
                }
                currentUser.setPhone(phoneObj != null ? phoneObj.toString() : null);

                List<String> roles = user.getList("roles", String.class);
                if (roles != null && !roles.isEmpty()) {
                    currentUser.setRoles(roles);
                    currentUser.setRole(roles.get(0));
                } else {
                    Object roleObj = user.get("role");
                    if (roleObj != null) currentUser.setRole(roleObj.toString());
                }
                System.out.println("[AdminProfilePanel] ✅ User data loaded: " + currentUser.getEmail());
            }
            
            // 2. Get Teacher Data (if admin is also a teacher)
            MongoCollection<Document> teacherCol = database.getCollection("teachers");
            Document teacherDoc = teacherCol.find(Filters.or(
                Filters.eq("user_id", adminId),
                Filters.eq("_id", adminId),
                Filters.eq("email", currentUser.getEmail())
            )).first();

            if (teacherDoc != null) {
                this.teacher = db.DocumentMapper.documentToTeacher(teacherDoc);
                System.out.println("[AdminProfilePanel] ✅ Teacher details merged: " + teacher.getSpecialization());
                System.out.println("[AdminProfilePanel] RAW DOC: " + teacherDoc.toJson());
            }
            
        } catch (Exception e) {
            System.err.println("[AdminProfilePanel] ❌ Error merging admin data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
