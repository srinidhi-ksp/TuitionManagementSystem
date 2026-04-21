package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

import model.Student;
import dao.StudentDAO;
import util.SessionManager;

public class ProfilePanel extends JPanel {

    private JPanel content;

    public ProfilePanel() {
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

        loadProfileAsync();

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

    private void loadProfileAsync() {
        content.removeAll();
        content.add(new JLabel("Loading profile..."));

        new SwingWorker<Student, Void>() {
            @Override
            protected Student doInBackground() throws Exception {
                String userId = SessionManager.getInstance().getUserId();
                if (userId == null) return null;
                return new StudentDAO().getStudentByUserId(userId);
            }

            @Override
            protected void done() {
                try {
                    Student s = get();
                    content.removeAll();
                    
                    String smUserId = SessionManager.getInstance().getUserId();
                    String smUserName = SessionManager.getInstance().getUserName();
                    
                    content.add(createField("Student ID", smUserId != null ? smUserId : "-"));
                    content.add(createField("Full Name", smUserName != null ? smUserName : "-"));
                    
                    if (s != null) {
                        content.add(createField("Email Address", s.getEmail() != null ? s.getEmail() : "-"));
                        content.add(createField("Standard", s.getCurrentStd()));
                        content.add(createField("Board", s.getBoard()));
                        String joinStr = s.getJoinDate() != null ? s.getJoinDate().toString().substring(0, 10) : "-";
                        content.add(createField("Join Date", joinStr));
                        String door = s.getDoorNo() != 0 ? String.valueOf(s.getDoorNo()) : "";
                        String street = s.getStreet() != null ? s.getStreet() : "";
                        String city = s.getCity() != null ? s.getCity() : "";
                        String pin = s.getPincode() != 0 ? String.valueOf(s.getPincode()) : "";
                        
                        String addr = (!door.isEmpty() ? door + ", " : "") + 
                                      (!street.isEmpty() ? street + ", " : "") + 
                                      (!city.isEmpty() ? city + " - " : "") + 
                                      pin;
                        if(addr.trim().isEmpty() || addr.trim().equals(", ,  -")) {
                            addr = "Address not provided";
                        }
                        content.add(createField("Address", addr));
                    } else {
                        content.add(createField("Academic Details", "Not found in student database."));
                    }
                    
                    content.revalidate();
                    content.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                    content.removeAll();
                    content.add(new JLabel("Error loading profile."));
                    content.revalidate();
                    content.repaint();
                }
            }
        }.execute();
    }
}
