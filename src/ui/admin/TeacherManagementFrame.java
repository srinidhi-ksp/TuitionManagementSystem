package ui.admin;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import dao.TeacherDAO;
import dao.CounterDAO;
import dao.UserDAO;
import model.Teacher;
import model.User;

public class TeacherManagementFrame extends JPanel {

    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color ACCENT_DARK = new Color(0, 102, 204);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);
    private static final Color ERROR_RED   = new Color(220, 53, 69);

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
        right.add(makeAccentButton("+ Add New Teacher", e -> openTeacherModal(null)));
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
            @Override public void onEdit(int row) {
                if (row >= 0 && row < currentTeachers.size()) {
                    Teacher t = currentTeachers.get(row);
                    // Retrieve email from users table explicitly
                    User u = new UserDAO().getUserById(t.getUserId());
                    if (u != null && u.getEmail() != null) {
                        t.setEmail(u.getEmail());
                    }
                    openTeacherModal(t);
                }
            }
            @Override public void onDelete(int row) {
                if (teacherTable.isEditing()) teacherTable.getCellEditor().stopCellEditing();
                int ok = JOptionPane.showConfirmDialog(null,
                    "Delete this teacher?", "Confirm", JOptionPane.YES_NO_OPTION);
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
            java.util.Date joinDateRaw = t.getJoinDate();
            if (joinDateRaw == null && t.getEmail() != null)
                joinDateRaw = userDAO.getCreatedAtByEmail(t.getEmail());
            if (joinDateRaw == null)
                joinDateRaw = userDAO.getCreatedAt(t.getUserId());
            String city    = t.getCity()   != null ? t.getCity()                  : "—";
            String joinStr = joinDateRaw   != null ? DATE_FMT.format(joinDateRaw) : "—";
            String name    = t.getName()   != null ? t.getName() + " (#" + t.getUserId() + ")"
                                                   : "Unspecified (#" + t.getUserId() + ")";
            model.addRow(new Object[]{name, t.getSpecialization(), city, joinStr, ""});
        }
    }

    private void openTeacherModal(Teacher editTarget) {
        final boolean isEditMode = (editTarget != null);
        String titleStr = isEditMode ? "Edit Teacher — " + editTarget.getName() : "Add New Teacher";

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), titleStr, true);
        dialog.setSize(660, 480);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(CARD_BG);
        dialog.setLayout(new BorderLayout());
        dialog.add(buildModalTitleBar(titleStr), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 20, 2));
        form.setBorder(new EmptyBorder(16, 24, 12, 24));
        form.setBackground(CARD_BG);

        JTextField  nameField   = styledField(); applyLetterFilter(nameField, 60);
        JTextField  emailField  = styledField();
        JTextField  phoneField  = styledField(); applyDigitFilter(phoneField, 10);
        JPasswordField passField  = new JPasswordField();
        JPasswordField cPassField = new JPasswordField();
        JTextField  specField   = styledField();
        
        DateChooser joinChooser = new DateChooser();
        JTextField  cityField   = styledField();

        JLabel nameErr  = errLabel(); JLabel emailErr = errLabel();
        JLabel phoneErr = errLabel();
        JLabel passErr  = errLabel(); JLabel cPassErr = errLabel();
        JLabel cityErr  = errLabel();

        styleField(passField); styleField(cPassField);

        if (isEditMode) {
            nameField.setText(editTarget.getName() != null ? editTarget.getName() : "");
            emailField.setText(editTarget.getEmail() != null ? editTarget.getEmail() : "");
            phoneField.setText(editTarget.getPhone() != null ? editTarget.getPhone() : "");
            specField.setText(editTarget.getSpecialization() != null ? editTarget.getSpecialization() : "");
            cityField.setText(editTarget.getCity() != null ? editTarget.getCity() : "");
            if (editTarget.getJoinDate() != null) joinChooser.setDate(editTarget.getJoinDate());
            passField.setText("••••••");
            cPassField.setText("••••••");
        }

        form.add(formRowWithErr("Full Name",          nameField,   nameErr));
        form.add(formRowWithErr("Email Address",      emailField,  emailErr));
        form.add(formRowWithErr("Phone Number",       phoneField,  phoneErr));
        form.add(formRow("Specialization",             specField));
        form.add(isEditMode
            ? formRow("Password (leave blank = keep)", passField)
            : formRowWithErr("Password",              passField, passErr));
        form.add(isEditMode
            ? formRow("Confirm Password",             cPassField)
            : formRowWithErr("Confirm Password",      cPassField, cPassErr));
        form.add(formRow("Join Date",                  joinChooser));
        form.add(formRowWithErr("City",               cityField, cityErr));

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getViewport().setBackground(CARD_BG);
        dialog.add(formScroll, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        btnRow.setBackground(PAGE_BG);
        btnRow.add(makeSecondaryButton("Cancel", e -> dialog.dispose()));
        String btnLabel = isEditMode ? "✓ Update Teacher" : "✓ Save Teacher";
        btnRow.add(makeAccentButton(btnLabel, e -> {
            if (!validateTeacherForm(nameField, emailField, phoneField, passField, cPassField,
                    cityField, nameErr, emailErr, phoneErr, passErr, cPassErr, cityErr, isEditMode))
                return;
            try {
                String uId = isEditMode ? editTarget.getUserId() : new CounterDAO().getNextTeacherId();
                Teacher t = new Teacher();
                t.setUserId(uId); t.setName(nameField.getText().trim());
                t.setEmail(emailField.getText().trim()); t.setPhone(phoneField.getText().trim());
                t.setRole("TEACHER"); t.setSpecialization(specField.getText().trim());
                t.setJoinDate(joinChooser.getDate());
                t.setCity(cityField.getText().trim()); t.setAdminId("A001");

                String pw = new String(passField.getPassword());
                t.setPassword((isEditMode && (pw.equals("••••••") || pw.isEmpty())) ? editTarget.getPassword() : pw);

                if (isEditMode) {
                    if (new TeacherDAO().updateTeacher(t)) {
                        JOptionPane.showMessageDialog(dialog, "Teacher updated successfully!");
                        refreshTable(); dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Update failed.");
                    }
                } else {
                    if (new UserDAO().addUser(t) && new TeacherDAO().addTeacher(t)) {
                        JOptionPane.showMessageDialog(dialog, "Teacher " + uId + " added!");
                        refreshTable(); dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Failed to add.");
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        }));

        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private boolean validateTeacherForm(
            JTextField nameField, JTextField emailField, JTextField phoneField,
            JPasswordField passField, JPasswordField cPassField,
            JTextField cityField, JLabel nameErr, JLabel emailErr, JLabel phoneErr, 
            JLabel passErr, JLabel cPassErr, JLabel cityErr, boolean isEditMode) {
        boolean ok = true;
        if (!nameField.getText().trim().matches("^[A-Za-z ]{3,}$")) {
            nameErr.setText("Only letters, min 3 chars"); ok = false;
        } else nameErr.setText("");
        if (!emailField.getText().trim().matches("^[\\w.+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            emailErr.setText("Invalid email"); ok = false;
        } else emailErr.setText("");
        if (!phoneField.getText().trim().matches("^\\d{10}$")) {
            phoneErr.setText("Exactly 10 digits required"); ok = false;
        } else phoneErr.setText("");
        if (!isEditMode) {
            if (new String(passField.getPassword()).length() < 6) {
                passErr.setText("Min 6 chars"); ok = false;
            } else passErr.setText("");
            if (!new String(passField.getPassword()).equals(new String(cPassField.getPassword()))) {
                cPassErr.setText("Mismatch"); ok = false;
            } else cPassErr.setText("");
        }
        if (cityField.getText().trim().isEmpty()) {
            cityErr.setText("City is required"); ok = false;
        } else cityErr.setText("");
        return ok;
    }

    private void applyLetterFilter(JTextField f, int max) {
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void replace(FilterBypass fb, int o, int l, String s, AttributeSet a) throws BadLocationException {
                String res = s.replaceAll("[^a-zA-Z ]", "");
                if (fb.getDocument().getLength() - l + res.length() <= max) super.replace(fb, o, l, res, a);
            }
        });
    }

    private void applyDigitFilter(JTextField f, int max) {
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override public void replace(FilterBypass fb, int o, int l, String s, AttributeSet a) throws BadLocationException {
                String res = s.replaceAll("[^0-9]", "");
                if (fb.getDocument().getLength() - l + res.length() <= max) super.replace(fb, o, l, res, a);
            }
        });
    }

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
            @Override public Component getTableCellRendererComponent(JTable table,Object value, boolean sel,boolean focus,int row,int col) {
                Component c = super.getTableCellRendererComponent(table,value,sel,focus,row,col);
                c.setBackground(sel ? new Color(74,144,226,20) : CARD_BG); c.setForeground(TEXT_PRI);
                setBorder(new EmptyBorder(0,16,0,0)); return c;
            }
        };
        for (int i = 0; i < t.getColumnCount()-1; i++) t.getColumnModel().getColumn(i).setCellRenderer(r);
    }

    private JPanel buildCardHeader(String text) {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(230,235,245)), new EmptyBorder(14,20,14,20)));
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("SansSerif", Font.BOLD, 15)); lbl.setForeground(TEXT_PRI);
        p.add(lbl, BorderLayout.WEST); return p;
    }

    private JPanel buildModalTitleBar(String text) {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(NAV_BG); p.setBorder(new EmptyBorder(16,24,16,24));
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("SansSerif", Font.BOLD, 16)); lbl.setForeground(Color.WHITE);
        p.add(lbl, BorderLayout.WEST); return p;
    }

    private JLabel errLabel() {
        JLabel lbl = new JLabel(""); lbl.setFont(new Font("SansSerif", Font.PLAIN, 10)); lbl.setForeground(ERROR_RED); return lbl;
    }

    private JPanel formRowWithErr(String label, JComponent comp, JLabel errLbl) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0)); wrapper.setBackground(CARD_BG);
        wrapper.add(formRow(label, comp), BorderLayout.CENTER); wrapper.add(errLbl, BorderLayout.SOUTH); return wrapper;
    }

    private JPanel formRow(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0,5)); p.setBackground(CARD_BG);
        JLabel lbl = new JLabel(label); lbl.setFont(new Font("SansSerif", Font.BOLD, 11)); lbl.setForeground(TEXT_SEC);
        if (comp instanceof JTextField || comp instanceof JPasswordField)
            comp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200,210,225),1,true), new EmptyBorder(6,10,6,10)));
        comp.setPreferredSize(new Dimension(0,36)); p.add(lbl, BorderLayout.NORTH); p.add(comp, BorderLayout.CENTER); return p;
    }

    private JTextField styledField() {
        JTextField f = new JTextField(); f.setFont(new Font("SansSerif", Font.PLAIN, 13)); f.setForeground(TEXT_PRI); return f;
    }

    private void styleField(JComponent c) {
        c.setFont(new Font("SansSerif", Font.PLAIN, 13));
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200,210,225),1,true), new EmptyBorder(6,10,6,10)));
        c.setPreferredSize(new Dimension(0,36));
    }

    private JButton makeAccentButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()?ACCENT_DARK:ACCENT); g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
                super.paintComponent(g2); g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE); btn.setFont(new Font("SansSerif",Font.BOLD,13));
        btn.setPreferredSize(new Dimension(200,38)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false); if (al!=null) btn.addActionListener(al); return btn;
    }

    private JButton makeSecondaryButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text); btn.setBackground(CARD_BG); btn.setForeground(ACCENT);
        btn.setFont(new Font("SansSerif",Font.BOLD,13)); btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT,1,true),new EmptyBorder(6,16,6,16)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); if (al!=null) btn.addActionListener(al); return btn;
    }
}