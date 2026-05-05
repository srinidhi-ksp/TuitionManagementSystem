package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import model.User;
import model.Teacher;
import service.TeacherPortalService;
import util.SessionManager;

public class ProfilePanel extends JPanel {

    private User user;
    private JPanel content;

    public ProfilePanel(User user) {
        this.user = user;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("My Profile");
        title.setFont(new Font("Arial", Font.BOLD, 24));

        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        content = new JPanel(new GridLayout(0, 2, 20, 20));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        content.add(createField("Loading", "Please wait..."));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true));
        wrapper.add(content, BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
        loadProfileAsync();
    }

    private void loadProfileAsync() {
        new SwingWorker<Teacher, Void>() {
            @Override protected Teacher doInBackground() {
                return new TeacherPortalService().getTeacherProfile(SessionManager.getCurrentTeacherId());
            }
            @Override protected void done() {
                try {
                    renderProfile(get());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ProfilePanel.this, "Failed to load profile: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void renderProfile(Teacher teacher) {
        content.removeAll();
        // Use a structured 4x2 grid for a professional card feel
        content.setLayout(new GridLayout(4, 2, 30, 20));
        
        // Add a professional TitledBorder to create the "Card" effect
        content.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(220, 230, 240), 1),
                " Teacher Information ",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 16),
                new Color(74, 144, 226)
            ),
            new EmptyBorder(30, 30, 30, 30)
        ));

        String tid = SessionManager.getCurrentTeacherId();
        String name = (teacher != null && teacher.getName() != null) ? teacher.getName() : SessionManager.getInstance().getUserName();
        String email = user.getEmail();

        content.add(createField("Teacher ID", tid != null ? tid : "-"));
        content.add(createField("Full Name", name != null ? name : "-"));
        content.add(createField("Email Address", email != null ? email : "-"));

        if (teacher != null) {
            content.add(createField("Specialization", safe(teacher.getSpecialization())));
            content.add(createField("City", safe(teacher.getCity())));
            
            String joinStr = "-";
            if (teacher.getJoinDate() != null && !teacher.getJoinDate().equals("-")) {
                joinStr = teacher.getJoinDate();
            }
            content.add(createField("Join Date", joinStr));
            
            content.add(createField("Salary", teacher.getSalary() > 0 ? "₹" + (long)teacher.getSalary() : "-"));
            content.add(createField("Experience", teacher.getExperience() > 0 ? teacher.getExperience() + " Years" : "-"));
        } else {
            // Fill placeholders if teacher document is missing
            content.add(createField("Specialization", "-"));
            content.add(createField("City", "-"));
            content.add(createField("Join Date", "-"));
            content.add(createField("Salary", "-"));
            content.add(createField("Experience", "-"));
        }

        content.revalidate();
        content.repaint();
    }

    private JPanel createField(String labelText, String valueText) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 5));
        p.setBackground(Color.WHITE);
        
        JLabel l = new JLabel(labelText.toUpperCase());
        l.setForeground(new Color(100, 116, 139)); // Slate-500
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));

        JLabel v = new JLabel(valueText != null && !valueText.equalsIgnoreCase("null") ? valueText : "-");
        v.setForeground(new Color(30, 41, 59)); // Slate-800
        v.setFont(new Font("Segoe UI", Font.BOLD, 15));

        p.add(l);
        p.add(v);
        return p;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null") ? "-" : value;
    }
}
