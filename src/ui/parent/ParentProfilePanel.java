package ui.parent;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import model.Parent;
import model.Student;
import service.ParentPortalService;
import util.SessionManager;
import util.ThemeManager;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import db.DBConnection;

public class ParentProfilePanel extends JPanel {

    private ParentPortalService portalService;
    private JPanel studentProfilesContainer;
    private JComboBox<String> studentSelector;
    private List<Student> linkedStudents;

    public ParentProfilePanel() {
        this.portalService = new ParentPortalService();
        setLayout(new BorderLayout(0, 30));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(32, 40, 40, 40));

        initHeader();
        initContent();
        loadInitialData();
    }

    private void initHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.BG);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(ThemeManager.BG);
        JLabel title = new JLabel("User Profile");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(ThemeManager.TEXT);
        JLabel sub = new JLabel("Manage your account details and view linked student information");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(ThemeManager.SUB_TEXT);
        titles.add(title);
        titles.add(sub);

        header.add(titles, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);
    }

    private void initContent() {
        JPanel center = new JPanel(new BorderLayout(0, 30));
        center.setBackground(ThemeManager.BG);

        // 1. Parent Profile Card
        center.add(createParentCard(), BorderLayout.NORTH);

        // 2. Student Profiles Section
        JPanel studentSection = new JPanel(new BorderLayout(0, 20));
        studentSection.setBackground(ThemeManager.BG);
        
        JLabel sTitle = new JLabel("Linked Student Profiles");
        sTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        sTitle.setForeground(ThemeManager.TEXT);
        studentSection.add(sTitle, BorderLayout.NORTH);

        studentProfilesContainer = new JPanel();
        studentProfilesContainer.setLayout(new BoxLayout(studentProfilesContainer, BoxLayout.Y_AXIS));
        studentProfilesContainer.setBackground(ThemeManager.BG);
        
        JScrollPane scroll = new JScrollPane(studentProfilesContainer);
        scroll.setBorder(null);
        scroll.setBackground(ThemeManager.BG);
        scroll.getViewport().setBackground(ThemeManager.BG);
        
        studentSection.add(scroll, BorderLayout.CENTER);
        center.add(studentSection, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
    }

    private JPanel createParentCard() {
        JPanel card = new JPanel(new BorderLayout(25, 0));
        card.setBackground(ThemeManager.CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true),
            new EmptyBorder(25, 30, 25, 30)
        ));

        JLabel icon = new JLabel("👤");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 48));
        card.add(icon, BorderLayout.WEST);

        JPanel info = new JPanel(new GridLayout(3, 1, 0, 5));
        info.setOpaque(false);
        
        String pId = SessionManager.getInstance().getUserId();
        dao.ParentDAO pDAO = new dao.ParentDAO();
        Parent p = pDAO.getByUserId(pId);

        String pName = (p != null && p.getName() != null) ? p.getName() : SessionManager.getInstance().getUserName();
        String pEmail = (p != null && p.getEmail() != null) ? p.getEmail() : "-";
        String pPhone = (p != null && p.getPhone() != null) ? p.getPhone() : "-";

        if (pEmail.equalsIgnoreCase("Not Available")) pEmail = "-";
        if (pPhone.equalsIgnoreCase("Not Available")) pPhone = "-";

        JLabel nameLabel = new JLabel(pName);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        nameLabel.setForeground(ThemeManager.TEXT);
        
        JLabel emailLabel = new JLabel("📧  " + pEmail);
        emailLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        emailLabel.setForeground(ThemeManager.SUB_TEXT);
        
        JLabel phoneLabel = new JLabel("📞  " + pPhone);
        phoneLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        phoneLabel.setForeground(ThemeManager.SUB_TEXT);

        info.add(nameLabel);
        info.add(emailLabel);
        info.add(phoneLabel);
        
        card.add(info, BorderLayout.CENTER);
        return card;
    }

    private void loadInitialData() {
        linkedStudents = portalService.getLinkedStudents(SessionManager.getInstance().getUserId());
        studentProfilesContainer.removeAll();
        
        if (linkedStudents == null || linkedStudents.isEmpty()) {
            JLabel emptyLabel = new JLabel("No students linked to this account");
            emptyLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
            emptyLabel.setForeground(ThemeManager.SUB_TEXT);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            studentProfilesContainer.add(Box.createVerticalGlue());
            studentProfilesContainer.add(emptyLabel);
            studentProfilesContainer.add(Box.createVerticalGlue());
        } else {
            for (Student s : linkedStudents) {
                studentProfilesContainer.add(createStudentItem(s));
                studentProfilesContainer.add(Box.createVerticalStrut(15));
            }
        }
        
        studentProfilesContainer.revalidate();
        studentProfilesContainer.repaint();
    }

    private JPanel createStudentItem(Student s) {
        JPanel p = new JPanel(new BorderLayout(20, 0));
        p.setBackground(ThemeManager.CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.DIVIDER, 1, true),
            new EmptyBorder(20, 25, 20, 25)
        ));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel text = new JPanel(new GridLayout(2, 3, 15, 8));
        text.setOpaque(false);
        
        JLabel name = new JLabel(s.getName() != null ? s.getName() : "Student");
        name.setFont(new Font("SansSerif", Font.BOLD, 16));
        name.setForeground(ThemeManager.TEXT);
        
        JLabel sid = new JLabel("🆔 ID: " + (s.getUserId() != null ? s.getUserId() : "-"));
        sid.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sid.setForeground(ThemeManager.SUB_TEXT);
        
        JLabel std = new JLabel("📚 Standard: " + (s.getCurrentStd() != null ? s.getCurrentStd() : "-"));
        std.setFont(new Font("SansSerif", Font.PLAIN, 13));
        std.setForeground(ThemeManager.SUB_TEXT);
        
        JLabel board = new JLabel("🏫 Board: " + (s.getBoard() != null ? s.getBoard() : "-"));
        board.setFont(new Font("SansSerif", Font.PLAIN, 13));
        board.setForeground(ThemeManager.SUB_TEXT);

        JLabel city = new JLabel("📍 City: " + (s.getCity() != null ? s.getCity() : "-"));
        city.setFont(new Font("SansSerif", Font.PLAIN, 13));
        city.setForeground(ThemeManager.SUB_TEXT);

        text.add(name);
        text.add(sid);
        text.add(std);
        text.add(board);
        text.add(city);
        
        p.add(text, BorderLayout.CENTER);
        return p;
    }
}
