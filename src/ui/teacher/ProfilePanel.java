package ui.teacher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

import model.User;
import model.Teacher;
import dao.TeacherDAO;

public class ProfilePanel extends JPanel {

    private User user;

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

        JPanel content = new JPanel(new GridLayout(0, 2, 20, 20));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        Teacher t = null;
        List<Teacher> allTeachers = new TeacherDAO().getAllTeachers();
        if (allTeachers != null) {
            for (Teacher th : allTeachers) {
                if (th.getUserId().equals(user.getUserId())) {
                    t = th;
                    break;
                }
            }
        }

        content.add(createField("Teacher ID", user.getUserId()));
        content.add(createField("Full Name", user.getName() != null ? user.getName() : "-"));
        content.add(createField("Email Address", user.getEmail() != null ? user.getEmail() : "-"));
        
        if (t != null) {
            content.add(createField("Specialization", t.getSpecialization()));
            String joinStr = t.getJoinDate() != null ? t.getJoinDate().toString().substring(0, 10) : "-";
            content.add(createField("Hire Date", joinStr));
            content.add(createField("City", t.getCity() != null ? t.getCity() : "-"));
            content.add(createField("Pincode", String.valueOf(t.getPincode())));
        } else {
            content.add(createField("Employment Details", "Not found in database."));
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true));
        wrapper.add(content, BorderLayout.CENTER);
        
        add(wrapper, BorderLayout.CENTER);
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
}
