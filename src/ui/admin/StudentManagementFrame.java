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
import dao.StudentDAO;
import dao.CounterDAO;
import dao.UserDAO;
import dao.ParentDAO;
import model.Student;
import model.Parent;
import model.User;

public class StudentManagementFrame extends JPanel {

    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color ACCENT_DARK = new Color(0, 102, 204);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);
    private static final Color ERROR_RED   = new Color(220, 53, 69);

    private JTable studentTable;
    private DefaultTableModel model;
    private List<Student> currentStudents;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MM-yyyy");

    public StudentManagementFrame() {
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
        JLabel title = new JLabel("Student Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(TEXT_PRI);
        JLabel sub = new JLabel("Manage student enrolment and details");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(TEXT_SEC);
        titles.add(title); titles.add(sub);
        panel.add(titles, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setBackground(PAGE_BG);
        right.add(makeSecondaryButton("↻  Refresh", e -> refreshTable()));
        right.add(makeAccentButton("+ Add New Student", e -> openStudentModal(null)));
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

        String[] cols = {"Student Name", "Standard", "Board", "City", "Join Date", "Actions"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5; }
        };
        studentTable = new JTable(model);
        styleTable(studentTable);
        refreshTable();

        TableActionEvent ev = new TableActionEvent() {
            @Override public void onEdit(int row) {
                if (row >= 0 && row < currentStudents.size()) {
                    Student s = currentStudents.get(row);
                    // Retrieve email from users table explicitly
                    User u = new UserDAO().getUserById(s.getUserId());
                    if (u != null && u.getEmail() != null) {
                        s.setEmail(u.getEmail());
                    }
                    openStudentModal(s);
                }
            }
            @Override public void onDelete(int row) {
                if (row < 0 || row >= currentStudents.size()) return;
                if (studentTable.isEditing()) studentTable.getCellEditor().stopCellEditing();
                int ok = JOptionPane.showConfirmDialog(null,
                    "Delete this student?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    Student s = currentStudents.get(row);
                    if (new StudentDAO().deleteStudent(s.getUserId())) {
                        new UserDAO().deleteUser(s.getUserId());
                        refreshTable();
                    } else {
                        JOptionPane.showMessageDialog(null, "Failed to delete student.");
                    }
                }
            }
        };
        studentTable.getColumnModel().getColumn(5).setCellRenderer(new TableActionCellRender());
        studentTable.getColumnModel().getColumn(5).setCellEditor(new TableActionCellEditor(ev));

        JScrollPane scroll = new JScrollPane(studentTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        card.add(buildCardHeader("All Students"), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        wrapper.add(card);
        return wrapper;
    }

    private void refreshTable() {
        model.setRowCount(0);
        currentStudents = new StudentDAO().getAllStudents();
        UserDAO userDAO = new UserDAO();
        for (Student s : currentStudents) {
            java.util.Date joinDateRaw = s.getJoinDate();
            if (joinDateRaw == null && s.getEmail() != null)
                joinDateRaw = userDAO.getCreatedAtByEmail(s.getEmail());
            if (joinDateRaw == null)
                joinDateRaw = userDAO.getCreatedAt(s.getUserId());
            String joinStr = joinDateRaw != null ? DATE_FMT.format(joinDateRaw) : "—";
            String city    = s.getCity()       != null ? s.getCity()       : "—";
            String std     = s.getCurrentStd() != null ? s.getCurrentStd() : "—";
            String board   = s.getBoard()      != null ? s.getBoard()      : "—";
            String name    = s.getName()       != null
                             ? s.getName() + " (#" + s.getUserId() + ")"
                             : "Unspecified (#" + s.getUserId() + ")";
            model.addRow(new Object[]{name, std, board, city, joinStr, ""});
        }
    }

    private void openStudentModal(Student editTarget) {
        final boolean isEditMode = (editTarget != null);
        String title = isEditMode ? "Edit Student — " + editTarget.getName() : "Add Student & Parent";

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setSize(750, 520);
        dialog.setLocationRelativeTo(this);

        CardLayout cl = new CardLayout();
        JPanel mainPanel = new JPanel(cl);

        // ── STEP 1: Student details ──────────────────────────────────────────
        JPanel step1 = new JPanel(new BorderLayout());
        step1.setBackground(CARD_BG);
        step1.add(buildModalTitleBar("Step 1 of 2 – Student Details"), BorderLayout.NORTH);

        JPanel sFrm = new JPanel(new GridLayout(0, 2, 20, 2));
        sFrm.setBorder(new EmptyBorder(16, 24, 12, 24));
        sFrm.setBackground(CARD_BG);

        JTextField  nameField   = styledField();
        JTextField  emailField  = styledField();
        JTextField  phoneField  = styledField(); applyDigitFilter(phoneField, 10);
        JPasswordField passField  = new JPasswordField();
        JPasswordField cPassField = new JPasswordField();
        JLabel nameErr  = errLabel(); JLabel emailErr = errLabel();
        JLabel phoneErr = errLabel();
        JLabel passErr  = errLabel(); JLabel cPassErr = errLabel();

        // Custom DateChooser instead of spinners
        DateChooser joinChooser = new DateChooser();
        DateChooser dobChooser  = new DateChooser(new Date(946665000000L)); // Default to 2000

        JTextField stdField   = styledField();
        JComboBox<String> boardCombo = new JComboBox<>(new String[]{"CBSE","ICSE","State Board"});
        JTextField cityField  = styledField();
        JLabel stdErr = errLabel(); JLabel cityErr = errLabel();

        styleField(passField); styleField(cPassField);
        applyLetterFilter(nameField, 60);

        if (isEditMode) {
            nameField.setText(editTarget.getName() != null ? editTarget.getName() : "");
            emailField.setText(editTarget.getEmail() != null ? editTarget.getEmail() : "");
            phoneField.setText(editTarget.getPhone() != null ? editTarget.getPhone() : "");
            stdField.setText(editTarget.getCurrentStd() != null ? editTarget.getCurrentStd() : "");
            cityField.setText(editTarget.getCity() != null ? editTarget.getCity() : "");
            if (editTarget.getBoard() != null) {
                for (int i = 0; i < boardCombo.getItemCount(); i++)
                    if (boardCombo.getItemAt(i).equals(editTarget.getBoard())) {
                        boardCombo.setSelectedIndex(i); break;
                    }
            }
            if (editTarget.getJoinDate() != null) joinChooser.setDate(editTarget.getJoinDate());
            if (editTarget.getDob()      != null) dobChooser.setDate(editTarget.getDob());
            passField.setText("••••••");
            cPassField.setText("••••••");
        }

        sFrm.add(formRowWithErr("Student Name",          nameField,   nameErr));
        sFrm.add(formRowWithErr("Email Address",         emailField,  emailErr));
        sFrm.add(formRowWithErr("Phone Number",          phoneField,  phoneErr));
        sFrm.add(formRowWithErr("Current Standard",      stdField,    stdErr));
        sFrm.add(isEditMode
            ? formRow("Password (leave blank = keep)", passField)
            : formRowWithErr("Password",               passField, passErr));
        sFrm.add(isEditMode
            ? formRow("Confirm Password",              cPassField)
            : formRowWithErr("Confirm Password",       cPassField, cPassErr));
        sFrm.add(formRow("Join Date",                    joinChooser));
        sFrm.add(formRow("Date of Birth",                dobChooser));
        sFrm.add(formRow("Board",                        boardCombo));
        sFrm.add(formRowWithErr("City",                  cityField,   cityErr));

        JPanel s1btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        s1btns.setBackground(PAGE_BG);
        s1btns.add(makeSecondaryButton("Cancel", e -> dialog.dispose()));

        if (isEditMode) {
            String existingPass = editTarget.getPassword();
            s1btns.add(makeAccentButton("✓ Update Student", e -> {
                if (!validateStudentStep(nameField, emailField, phoneField, passField, cPassField,
                        stdField, cityField, nameErr, emailErr, phoneErr, passErr, cPassErr, stdErr, cityErr, isEditMode))
                    return;
                try {
                    Student s = buildStudentFromForm(editTarget.getUserId(), nameField, emailField, phoneField,
                        passField, boardCombo, joinChooser, dobChooser, stdField, cityField, isEditMode, existingPass);
                    if (new StudentDAO().updateStudent(s)) {
                        JOptionPane.showMessageDialog(dialog, "Student updated successfully!");
                        refreshTable(); dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dialog, "Update failed.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
                }
            }));
        } else {
            s1btns.add(makeAccentButton("Next: Parent Details →", e -> {
                if (validateStudentStep(nameField, emailField, phoneField, passField, cPassField,
                        stdField, cityField, nameErr, emailErr, phoneErr, passErr, cPassErr, stdErr, cityErr, false))
                    cl.show(mainPanel, "STEP2");
            }));
        }

        step1.add(new JScrollPane(sFrm) {{
            setBorder(BorderFactory.createEmptyBorder());
            getViewport().setBackground(CARD_BG);
        }}, BorderLayout.CENTER);
        step1.add(s1btns, BorderLayout.SOUTH);

        // ── STEP 2: Parent details ──────────────────────────────────────────
        JPanel step2 = new JPanel(new BorderLayout());
        step2.setBackground(CARD_BG);
        step2.add(buildModalTitleBar("Step 2 of 2 – Parent / Guardian Details"), BorderLayout.NORTH);

        JPanel pFrm = new JPanel(new GridLayout(0, 2, 20, 4));
        pFrm.setBorder(new EmptyBorder(16, 24, 12, 24));
        pFrm.setBackground(CARD_BG);

        JTextField  pNameField = styledField(); applyLetterFilter(pNameField, 60);
        JComboBox<String> relCombo = new JComboBox<>(new String[]{"Father","Mother","Guardian"});
        JTextField  langField  = styledField();
        JTextField  occField   = styledField();
        JTextField  incField   = styledField(); applyDecimalFilter(incField);
        JTextField  emgField   = styledField(); applyDigitFilter(emgField, 10);
        JLabel pNameErr = errLabel(); JLabel emgErr = errLabel();

        pFrm.add(formRowWithErr("Parent/Guardian Name", pNameField, pNameErr));
        pFrm.add(formRow("Relation Type",               relCombo));
        pFrm.add(formRow("Preferred Language",          langField));
        pFrm.add(formRow("Occupation",                  occField));
        pFrm.add(formRow("Annual Income (₹)",           incField));
        pFrm.add(formRowWithErr("Emergency Contact (10 digits)", emgField, emgErr));

        JPanel s2btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        s2btns.setBackground(PAGE_BG);
        s2btns.add(makeSecondaryButton("← Back", e -> cl.show(mainPanel, "STEP1")));
        s2btns.add(makeAccentButton("✓ Save Student & Parent", e -> {
            boolean ok = true;
            if (pNameField.getText().trim().length() < 2) {
                pNameErr.setText("Parent name required"); ok = false;
            } else pNameErr.setText("");
            if (emgField.getText().trim().length() != 10) {
                emgErr.setText("10 digits required"); ok = false;
            } else emgErr.setText("");
            if (!ok) return;

            try {
                String sId = new CounterDAO().getNextStudentId();
                String pId = "P" + sId.substring(1);
                Student s = buildStudentFromForm(sId, nameField, emailField, phoneField, passField,
                    boardCombo, joinChooser, dobChooser, stdField, cityField, false, null);
                Parent p = new Parent();
                p.setUserId(pId); p.setName(pNameField.getText().trim());
                p.setEmail("p_" + emailField.getText().trim()); p.setPassword("pass123");
                p.setRelationType(relCombo.getSelectedItem().toString());
                p.setPreferredLanguage(langField.getText().trim()); p.setOccupation(occField.getText().trim());
                p.setAnnualIncome(incField.getText().trim().isEmpty() ? 0 : Double.parseDouble(incField.getText().trim()));
                p.setEmergencyContact(Long.parseLong(emgField.getText().trim()));
                s.setParent(p);
                if (new UserDAO().addUser(s) && new StudentDAO().addStudent(s)
                        && new UserDAO().addUser(p) && new ParentDAO().addParent(p)) {
                    JOptionPane.showMessageDialog(dialog, "Student and Parent saved!");
                    refreshTable(); dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to save.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        }));
        step2.add(new JScrollPane(pFrm) {{
            setBorder(BorderFactory.createEmptyBorder());
            getViewport().setBackground(CARD_BG);
        }}, BorderLayout.CENTER);
        step2.add(s2btns, BorderLayout.SOUTH);

        mainPanel.add(step1, "STEP1");
        mainPanel.add(step2, "STEP2");
        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private boolean validateStudentStep(
            JTextField nameField, JTextField emailField, JTextField phoneField,
            JPasswordField passField, JPasswordField cPassField,
            JTextField stdField, JTextField cityField,
            JLabel nameErr, JLabel emailErr, JLabel phoneErr, JLabel passErr, JLabel cPassErr,
            JLabel stdErr, JLabel cityErr, boolean isEditMode) {
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
        if (stdField.getText().trim().isEmpty()) {
            stdErr.setText("Required"); ok = false;
        } else stdErr.setText("");
        if (cityField.getText().trim().isEmpty()) {
            cityErr.setText("Required"); ok = false;
        } else cityErr.setText("");
        return ok;
    }

    private Student buildStudentFromForm(String userId,
            JTextField nameField, JTextField emailField, JTextField phoneField, JPasswordField passField,
            JComboBox<String> boardCombo, DateChooser joinChooser, DateChooser dobChooser,
            JTextField stdField, JTextField cityField, boolean isEditMode, String oldPass) {
        Student s = new Student();
        s.setUserId(userId); s.setName(nameField.getText().trim());
        s.setEmail(emailField.getText().trim()); s.setPhone(phoneField.getText().trim());
        s.setRole("STUDENT");
        String pw = new String(passField.getPassword());
        s.setPassword((isEditMode && (pw.equals("••••••") || pw.isEmpty())) ? oldPass : pw);
        s.setCurrentStd(stdField.getText().trim());
        s.setBoard(boardCombo.getSelectedItem().toString());
        s.setJoinDate(joinChooser.getDate());
        s.setDob(dobChooser.getDate());
        s.setCity(cityField.getText().trim());
        return s;
    }

    private void applyDigitFilter(JTextField f, int max) {
        ((AbstractDocument)f.getDocument()).setDocumentFilter(new DocumentFilter(){
            @Override public void replace(FilterBypass fb, int o, int l, String s, AttributeSet a) throws BadLocationException {
                String res = s.replaceAll("[^0-9]", "");
                if (fb.getDocument().getLength()-l+res.length() <= max) super.replace(fb, o, l, res, a);
            }
        });
    }

    private void applyLetterFilter(JTextField f, int max) {
        ((AbstractDocument)f.getDocument()).setDocumentFilter(new DocumentFilter(){
            @Override public void replace(FilterBypass fb, int o, int l, String s, AttributeSet a) throws BadLocationException {
                String res = s.replaceAll("[^a-zA-Z ]", "");
                if (fb.getDocument().getLength()-l+res.length() <= max) super.replace(fb, o, l, res, a);
            }
        });
    }

    private void applyDecimalFilter(JTextField f) {
        ((AbstractDocument)f.getDocument()).setDocumentFilter(new DocumentFilter(){
            @Override public void replace(FilterBypass fb, int o, int l, String s, AttributeSet a) throws BadLocationException {
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                String res = s.replaceAll("[^0-9.]", "");
                if (res.contains(".") && cur.contains(".")) res = res.replace(".", "");
                super.replace(fb, o, l, res, a);
            }
        });
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(44); t.setShowGrid(false); t.setShowHorizontalLines(true);
        t.setGridColor(new Color(235,240,248)); t.setIntercellSpacing(new Dimension(0,0));
        t.setBackground(CARD_BG); t.setSelectionBackground(new Color(74,144,226,25));
        t.setSelectionForeground(TEXT_PRI); t.setFocusable(false);
        t.getTableHeader().setBackground(new Color(248,250,253));
        t.getTableHeader().setForeground(TEXT_SEC);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0,new Color(230,235,245)));
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean focus, int row, int col) {
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
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("SansSerif",Font.BOLD,15)); lbl.setForeground(TEXT_PRI);
        p.add(lbl,BorderLayout.WEST); return p;
    }

    private JPanel buildModalTitleBar(String text) {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(NAV_BG); p.setBorder(new EmptyBorder(16,24,16,24));
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("SansSerif",Font.BOLD,16)); lbl.setForeground(Color.WHITE);
        p.add(lbl,BorderLayout.WEST); return p;
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
        JLabel lbl = new JLabel(label); lbl.setFont(new Font("SansSerif",Font.BOLD,11)); lbl.setForeground(TEXT_SEC);
        if (comp instanceof JTextField || comp instanceof JPasswordField)
            comp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200,210,225),1,true), new EmptyBorder(6,10,6,10)));
        comp.setPreferredSize(new Dimension(0,36)); p.add(lbl,BorderLayout.NORTH); p.add(comp,BorderLayout.CENTER); return p;
    }

    private JTextField styledField() {
        JTextField f = new JTextField(); f.setFont(new Font("SansSerif",Font.PLAIN,13)); f.setForeground(TEXT_PRI); return f;
    }

    private void styleField(JComponent c) {
        c.setFont(new Font("SansSerif",Font.PLAIN,13));
        c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200,210,225),1,true), new EmptyBorder(6,10,6,10)));
        c.setPreferredSize(new Dimension(0,36));
    }

    private JButton makeAccentButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()?ACCENT_DARK:ACCENT); g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
                super.paintComponent(g2); g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE); btn.setFont(new Font("SansSerif",Font.BOLD,13));
        btn.setPreferredSize(new Dimension(220,38)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false); if (al!=null) btn.addActionListener(al); return btn;
    }

    private JButton makeSecondaryButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text); btn.setBackground(CARD_BG); btn.setForeground(ACCENT);
        btn.setFont(new Font("SansSerif",Font.BOLD,13)); btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT,1,true),new EmptyBorder(6,16,6,16)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); if (al!=null) btn.addActionListener(al); return btn;
    }
}