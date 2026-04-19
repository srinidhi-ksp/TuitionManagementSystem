package ui.admin;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import dao.TeacherDAO;
import dao.UserDAO;
import model.Teacher;

public class TeacherManagementFrame extends JPanel {

    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color ACCENT_DARK = new Color(0, 102, 204);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);

    private JTable teacherTable;
    private DefaultTableModel model;
    private List<Teacher> currentTeachers;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MM-yyyy");

    public TeacherManagementFrame() {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);
        add(createHeader(), BorderLayout.NORTH);
        add(createBody(),   BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BG);
        panel.setBorder(new EmptyBorder(28, 36, 12, 36));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(PAGE_BG);
        JLabel title = new JLabel("Teacher Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(TEXT_PRI);
        JLabel sub = new JLabel("Manage teaching staff records");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(TEXT_SEC);
        titles.add(title); titles.add(sub);
        panel.add(titles, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setBackground(PAGE_BG);
        right.add(makeSecondaryButton("↻  Refresh", e -> refreshTable()));
        right.add(makeAccentButton("+ Add New Teacher", e -> openAddTeacherModal()));
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JPanel createBody() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(PAGE_BG);
        wrapper.setBorder(new EmptyBorder(0, 36, 36, 36));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true));

        String[] cols = {"Teacher Name", "Specialization", "City", "Join Date", "Actions"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 4; }
        };
        teacherTable = new JTable(model);
        styleTable(teacherTable);

        refreshTable();

        TableActionEvent ev = new TableActionEvent() {
            @Override public void onEdit(int row)   { openAddTeacherModal(); }
            @Override public void onDelete(int row) {
                if (teacherTable.isEditing()) teacherTable.getCellEditor().stopCellEditing();
                int ok = JOptionPane.showConfirmDialog(null, "Delete this teacher?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    Teacher t = currentTeachers.get(row);
                    if (new TeacherDAO().deleteTeacher(t.getUserId())) {
                        new UserDAO().deleteUser(t.getUserId());
                        refreshTable();
                    } else {
                        JOptionPane.showMessageDialog(null, "Failed to delete.");
                    }
                }
            }
        };
        teacherTable.getColumnModel().getColumn(4).setCellRenderer(new TableActionCellRender());
        teacherTable.getColumnModel().getColumn(4).setCellEditor(new TableActionCellEditor(ev));

        JScrollPane scroll = new JScrollPane(teacherTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        card.add(buildCardHeader("All Teachers"), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        wrapper.add(card);
        return wrapper;
    }

    private void refreshTable() {
        model.setRowCount(0);
        currentTeachers = new TeacherDAO().getAllTeachers();
        UserDAO userDAO = new UserDAO();
        for (Teacher t : currentTeachers) {
            // Join date strategy:
            // 1. Use join_date from teachers doc if available
            // 2. Fallback → users.created_at by email (most reliable cross-collection match)
            // 3. Fallback → users.created_at by _id (works if teachers._id matches users._id)
            java.util.Date joinDateRaw = t.getJoinDate();
            if (joinDateRaw == null && t.getEmail() != null) {
                joinDateRaw = userDAO.getCreatedAtByEmail(t.getEmail());
            }
            if (joinDateRaw == null) {
                joinDateRaw = userDAO.getCreatedAt(t.getUserId());
            }
            String city    = t.getCity()   != null ? t.getCity()                   : "—";
            String joinStr = joinDateRaw   != null ? DATE_FMT.format(joinDateRaw)  : "—";
            String name    = t.getName()   != null ? t.getName() + " (#" + t.getUserId() + ")"
                                                   : "Unspecified (#" + t.getUserId() + ")";
            model.addRow(new Object[]{name, t.getSpecialization(), city, joinStr, ""});
        }
    }

    private void openAddTeacherModal() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add New Teacher", true);
        dialog.setSize(620, 500);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(CARD_BG);
        dialog.setLayout(new BorderLayout());
        dialog.add(buildModalTitleBar("Add New Teacher"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 20, 14));
        form.setBorder(new EmptyBorder(24, 24, 20, 24));
        form.setBackground(CARD_BG);

        JTextField nameField  = styledField();
        JTextField emailField = styledField();
        JPasswordField passField  = new JPasswordField();
        JPasswordField cPassField = new JPasswordField();
        JTextField specField  = styledField();
        JTextField joinField  = styledField("YYYY-MM-DD");
        JTextField doorField  = styledField();
        JTextField streetField= styledField();
        JTextField cityField  = styledField();
        JTextField pinField   = styledField();

        styleField(passField); styleField(cPassField);

        form.add(formRow("Full Name",        nameField));
        form.add(formRow("Email Address",    emailField));
        form.add(formRow("Password",         passField));
        form.add(formRow("Confirm Password", cPassField));
        form.add(formRow("Specialization",   specField));
        form.add(formRow("Join Date (YYYY-MM-DD)", joinField));
        form.add(formRow("Door No.",         doorField));
        form.add(formRow("Street Name",      streetField));
        form.add(formRow("City",             cityField));
        form.add(formRow("Pincode",          pinField));

        JPanel btnRow = buildModalButtons(dialog, () -> {
            try {
                String uId = "T" + String.format("%03d", (int)(Math.random() * 900) + 100);
                Teacher t = new Teacher();
                t.setUserId(uId);
                t.setName(nameField.getText().trim());
                t.setEmail(emailField.getText().trim());
                t.setPassword(new String(passField.getPassword()));
                t.setSpecialization(specField.getText().trim());
                t.setJoinDate(new SimpleDateFormat("yyyy-MM-dd").parse(joinField.getText().trim()));
                t.setDoorNo(Integer.parseInt(doorField.getText().trim()));
                t.setStreet(streetField.getText().trim());
                t.setCity(cityField.getText().trim());
                t.setPincode(Long.parseLong(pinField.getText().trim()));
                t.setAdminId("A001");

                if (new UserDAO().addUser(t)) {
                    if (new TeacherDAO().addTeacher(t)) {
                        JOptionPane.showMessageDialog(dialog, "Teacher added successfully!");
                        refreshTable();
                        dialog.dispose();
                    } else {
                        new UserDAO().deleteUser(uId);
                        JOptionPane.showMessageDialog(dialog, "Failed to add teacher record.");
                    }
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to create user account.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Please fill all fields with valid data.\n" + ex.getMessage());
            }
        });

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────
    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(44); t.setShowGrid(false); t.setShowHorizontalLines(true);
        t.setGridColor(new Color(235, 240, 248)); t.setIntercellSpacing(new Dimension(0,0));
        t.setBackground(CARD_BG);
        t.setSelectionBackground(new Color(74,144,226,25)); t.setSelectionForeground(TEXT_PRI);
        t.setFocusable(false);
        t.getTableHeader().setBackground(new Color(248,250,253));
        t.getTableHeader().setForeground(TEXT_SEC);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(230,235,245)));
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table,Object value,
                    boolean sel,boolean focus,int row,int col) {
                Component c = super.getTableCellRendererComponent(table,value,sel,focus,row,col);
                c.setBackground(sel?new Color(74,144,226,20):CARD_BG); c.setForeground(TEXT_PRI);
                setBorder(new EmptyBorder(0,16,0,0)); return c;
            }
        };
        for (int i=0;i<t.getColumnCount()-1;i++) t.getColumnModel().getColumn(i).setCellRenderer(r);
    }

    private JPanel buildCardHeader(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,new Color(230,235,245)),
            new EmptyBorder(14,20,14,20)));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 15)); lbl.setForeground(TEXT_PRI);
        p.add(lbl, BorderLayout.WEST); return p;
    }

    private JPanel buildModalTitleBar(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(NAV_BG); p.setBorder(new EmptyBorder(16,24,16,24));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 16)); lbl.setForeground(Color.WHITE);
        p.add(lbl, BorderLayout.WEST); return p;
    }

    private JPanel buildModalButtons(JDialog dialog, Runnable onSubmit) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        p.setBackground(new Color(244,247,249));
        p.add(makeSecondaryButton("Cancel", e -> dialog.dispose()));
        p.add(makeAccentButton("Save Teacher", e -> onSubmit.run()));
        return p;
    }

    private JPanel formRow(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0,5));
        p.setBackground(CARD_BG);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11)); lbl.setForeground(TEXT_SEC);
        if (comp instanceof JTextField)
            ((JTextField)comp).setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,210,225),1,true),
                new EmptyBorder(6,10,6,10)));
        comp.setPreferredSize(new Dimension(0,36));
        p.add(lbl, BorderLayout.NORTH); p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private JTextField styledField() { return styledField(""); }
    private JTextField styledField(String t) {
        JTextField f = new JTextField(t);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13)); f.setForeground(TEXT_PRI); return f;
    }
    private void styleField(JComponent c) {
        c.setFont(new Font("SansSerif", Font.PLAIN, 13));
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200,210,225),1,true),
            new EmptyBorder(6,10,6,10)));
        c.setPreferredSize(new Dimension(0,36));
    }

    private JButton makeAccentButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()?ACCENT_DARK:ACCENT);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
                super.paintComponent(g2); g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE); btn.setFont(new Font("SansSerif",Font.BOLD,13));
        btn.setPreferredSize(new Dimension(170,38)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false); if (al!=null) btn.addActionListener(al); return btn;
    }

    private JButton makeSecondaryButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text);
        btn.setBackground(CARD_BG); btn.setForeground(ACCENT);
        btn.setFont(new Font("SansSerif",Font.BOLD,13)); btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT,1,true),new EmptyBorder(6,16,6,16)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (al!=null) btn.addActionListener(al); return btn;
    }
}