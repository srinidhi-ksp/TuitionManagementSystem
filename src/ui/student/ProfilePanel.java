package ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

import model.Student;
import dao.StudentDAO;
import util.SessionManager;

import java.text.SimpleDateFormat;

public class ProfilePanel extends JPanel {

    private JPanel contentPanel;
    private static final Color ACCENT = new Color(74, 144, 226);
    private static final Color BG_COLOR = new Color(244, 247, 249);
    private static final Color CARD_BG = Color.WHITE;

    public ProfilePanel() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_COLOR);
        header.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        JLabel title = new JLabel("My Profile");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(new Color(30, 40, 60));
        
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_COLOR);

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        loadProfileAsync();
    }

    private void loadProfileAsync() {
        contentPanel.removeAll();
        contentPanel.add(new JLabel("Loading profile..."));

        new SwingWorker<Student, Void>() {
            @Override
            protected Student doInBackground() throws Exception {
                String userId = SessionManager.getInstance().getUserId();
                if (userId == null) return null;
                return new StudentDAO().getStudentById(userId);
            }

            @Override
            protected void done() {
                try {
                    Student s = get();
                    contentPanel.removeAll();
                    
                    String smUserId = SessionManager.getInstance().getUserId();
                    String smUserName = SessionManager.getInstance().getUserName();
                    
                    String id = smUserId != null ? smUserId : "-";
                    String name = smUserName != null ? smUserName : "-";
                    
                    // 1. Header Card (Avatar + Name)
                    contentPanel.add(createHeaderCard(name, id, s));
                    contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
                    
                    if (s != null) {
                        // 2. Personal Info Card
                        JPanel personalCard = createSectionCard("Personal Details");
                        personalCard.add(createField("Email Address", s.getEmail() != null ? s.getEmail() : "-"));
                        personalCard.add(createField("Phone Number", s.getPhone() != null ? s.getPhone() : "-"));
                        String dobStr = s.getDob() != null ? new SimpleDateFormat("MMM dd, yyyy").format(s.getDob()) : "-";
                        personalCard.add(createField("Date of Birth", dobStr));
                        
                        String addr = getAddressString(s);
                        personalCard.add(createField("Address", addr));
                        contentPanel.add(personalCard);
                        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));

                        // 3. Academic Details
                        JPanel academicCard = createSectionCard("Academic Details");
                        academicCard.add(createField("Standard", s.getCurrentStd()));
                        academicCard.add(createField("Board", s.getBoard()));
                        String joinStr = s.getJoinDate() != null ? new SimpleDateFormat("MMM dd, yyyy").format(s.getJoinDate()) : "-";
                        academicCard.add(createField("Join Date", joinStr));
                        contentPanel.add(academicCard);
                        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
                        
                        // 4. Parent Details
                        JPanel parentCard = createSectionCard("Parent/Guardian Details");
                        parentCard.add(createField("Parent Name", s.getParentName()));
                        parentCard.add(createField("Contact Number", s.getParentPhone()));
                        parentCard.add(createField("Occupation", s.getParentOccupation()));
                        parentCard.add(createField("Relation", s.getParentRelation()));
                        contentPanel.add(parentCard);

                    } else {
                        JPanel errorCard = createSectionCard("Error");
                        errorCard.add(createField("Academic Details", "Not found in student database."));
                        contentPanel.add(errorCard);
                    }
                    
                    contentPanel.revalidate();
                    contentPanel.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                    contentPanel.removeAll();
                    contentPanel.add(new JLabel("Error loading profile."));
                    contentPanel.revalidate();
                    contentPanel.repaint();
                }
            }
        }.execute();
    }
    
    private String getAddressString(Student s) {
        String door = s.getDoorNo() != 0 ? String.valueOf(s.getDoorNo()) : "";
        String street = s.getStreet() != null ? s.getStreet() : "";
        String city = s.getCity() != null ? s.getCity() : "";
        String pin = s.getPincode() != 0 ? String.valueOf(s.getPincode()) : "";
        String addr = (!door.isEmpty() ? door + ", " : "") + 
                      (!street.isEmpty() ? street + ", " : "") + 
                      (!city.isEmpty() ? city + " - " : "") + pin;
        if(addr.trim().isEmpty() || addr.trim().equals("-") || addr.trim().equals(", ,  -")) {
            return "Address not provided";
        }
        return addr;
    }

    private JPanel createHeaderCard(String name, String id, Student s) {
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(CARD_BG);
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 235), 1, true),
            new EmptyBorder(25, 30, 25, 30)
        ));
        c.setMaximumSize(new Dimension(800, 150));
        
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        left.setBackground(CARD_BG);
        
        // Avatar Circle
        JLabel avatar = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 36));
                String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "S";
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(initial)) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(initial, x, y);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(80, 80));
        left.add(avatar);
        
        JPanel texts = new JPanel();
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        texts.setBackground(CARD_BG);
        
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("Arial", Font.BOLD, 22));
        nameLbl.setForeground(new Color(30, 40, 60));
        
        JLabel idLbl = new JLabel("Student ID: " + id);
        idLbl.setFont(new Font("Arial", Font.PLAIN, 14));
        idLbl.setForeground(new Color(100, 110, 130));
        
        texts.add(Box.createRigidArea(new Dimension(0, 10)));
        texts.add(nameLbl);
        texts.add(Box.createRigidArea(new Dimension(0, 8)));
        texts.add(idLbl);
        
        left.add(texts);
        c.add(left, BorderLayout.WEST);
        
        return c;
    }

    private JPanel createSectionCard(String title) {
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(CARD_BG);
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 235), 1, true),
            new EmptyBorder(25, 30, 25, 30)
        ));
        c.setMaximumSize(new Dimension(800, 300));
        
        JLabel tLbl = new JLabel(title);
        tLbl.setFont(new Font("Arial", Font.BOLD, 18));
        tLbl.setForeground(ACCENT);
        tLbl.setBorder(new EmptyBorder(0, 0, 20, 0));
        c.add(tLbl, BorderLayout.NORTH);
        
        JPanel grid = new JPanel(new GridLayout(0, 2, 20, 20));
        grid.setBackground(CARD_BG);
        c.add(grid, BorderLayout.CENTER);
        
        return c;
    }

    private JPanel createField(String labelText, String valueText) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        
        JLabel l = new JLabel(labelText);
        l.setForeground(new Color(110, 120, 140));
        l.setFont(new Font("Inter", Font.PLAIN, 12));
        l.setBorder(new EmptyBorder(0, 0, 5, 0));
        
        JLabel v = new JLabel(valueText != null && !valueText.isEmpty() ? valueText : "N/A");
        v.setFont(new Font("Inter", Font.BOLD, 15));
        v.setForeground(new Color(40, 50, 70));
        
        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }
}
