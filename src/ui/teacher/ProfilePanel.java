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
        String teacherId = SessionManager.getCurrentTeacherId();
        content.add(createField("Teacher ID", teacherId != null ? teacherId : "-"));
        content.add(createField("Full Name", SessionManager.getInstance().getUserName() != null ? SessionManager.getInstance().getUserName() : "-"));
        content.add(createField("Email Address", user.getEmail() != null ? user.getEmail() : "-"));

        if (teacher != null) {
            content.add(createField("Specialization", safe(teacher.getSpecialization())));
            String joinStr = teacher.getJoinDate() != null ? teacher.getJoinDate().toString().substring(0, 10) : "-";
            content.add(createField("Hire Date", joinStr));
            content.add(createField("City", safe(teacher.getCity())));
            content.add(createField("Pincode", String.valueOf(teacher.getPincode())));
        } else {
            content.add(createField("Employment Details", "Not found in database."));
        }
        content.revalidate();
        content.repaint();
    }

    private JPanel createField(String labelText, String valueText) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 5));
        p.setBackground(Color.WHITE);
        JLabel l = new JLabel(labelText);
        l.setForeground(Color.GRAY);
        l.setFont(new Font("Arial", Font.PLAIN, 12));

        JLabel v = new JLabel(valueText);
        v.setFont(new Font("Arial", Font.BOLD, 15));

        p.add(l);
        p.add(v);
        return p;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
