package ui.admin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import dao.CounterDAO;
import dao.TeacherDAO;
import dao.TeacherFilterDAO;
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
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MMM-yyyy");

    // ── Filter components ──
    private JComboBox<String> specializationCombo, experienceCombo, cityCombo, salaryCombo;
    private JTextField searchField;

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

        // ── FILTER BAR ──
        card.add(createFilterBar(), BorderLayout.NORTH);

        String[] cols = {"Teacher Name", "Specialization", "Experience", "Salary", "Degree", "City", "Join Date", "Actions"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 7; }
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
        teacherTable.getColumnModel().getColumn(7).setCellRenderer(new TableActionCellRender());
        teacherTable.getColumnModel().getColumn(7).setCellEditor(new TableActionCellEditor(ev));

        JScrollPane scroll = new JScrollPane(teacherTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        card.add(scroll, BorderLayout.CENTER);
        wrapper.add(card);
        return wrapper;
    }

    private JPanel createFilterBar() {
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(CARD_BG);
        filterPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        TeacherFilterDAO filterDao = new TeacherFilterDAO();

        // Specialization filter
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.1;
        JLabel specLabel = new JLabel("Specialization:");
        specLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        specLabel.setForeground(TEXT_SEC);
        filterPanel.add(specLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.2;
        specializationCombo = new JComboBox<>();
        specializationCombo.addItem("All");
        for (String spec : filterDao.getAllSpecializations()) {
            specializationCombo.addItem(spec);
        }
        filterPanel.add(specializationCombo, gbc);

        // Experience filter
        gbc.gridx = 2; gbc.weightx = 0.1;
        JLabel expLabel = new JLabel("Experience:");
        expLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        expLabel.setForeground(TEXT_SEC);
        filterPanel.add(expLabel, gbc);

        gbc.gridx = 3; gbc.weightx = 0.2;
        experienceCombo = new JComboBox<>(new String[]{"All", "0-2 years", "3-5 years", "5+ years"});
        filterPanel.add(experienceCombo, gbc);

        // City filter
        gbc.gridx = 4; gbc.weightx = 0.1;
        JLabel cityLabel = new JLabel("City:");
        cityLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        cityLabel.setForeground(TEXT_SEC);
        filterPanel.add(cityLabel, gbc);

        gbc.gridx = 5; gbc.weightx = 0.2;
        cityCombo = new JComboBox<>();
        cityCombo.addItem("All");
        for (String city : filterDao.getAllCities()) {
            cityCombo.addItem(city);
        }
        filterPanel.add(cityCombo, gbc);

        // Second row
        gbc.gridy = 1;

        // Salary filter
        gbc.gridx = 0; gbc.weightx = 0.1;
        JLabel salaryLabel = new JLabel("Salary:");
        salaryLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        salaryLabel.setForeground(TEXT_SEC);
        filterPanel.add(salaryLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.2;
        salaryCombo = new JComboBox<>(new String[]{"All", "<20000", "20000-40000", "40000+"});
        filterPanel.add(salaryCombo, gbc);

        // Search field
        gbc.gridx = 2; gbc.weightx = 0.1;
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        searchLabel.setForeground(TEXT_SEC);
        filterPanel.add(searchLabel, gbc);

        gbc.gridx = 3; gbc.gridwidth = 2; gbc.weightx = 0.4;
        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
            new EmptyBorder(4, 8, 4, 8)
        ));
        filterPanel.add(searchField, gbc);

        // Buttons
        gbc.gridx = 5; gbc.gridwidth = 1; gbc.weightx = 0.2;
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnPanel.setOpaque(false);
        
        JButton applyBtn = makeAccentButton("Apply", e -> applyFilters());
        applyBtn.setPreferredSize(new Dimension(80, 28));
        
        JButton clearBtn = makeSecondaryButton("Clear", e -> clearFilters());
        clearBtn.setPreferredSize(new Dimension(80, 28));
        
        btnPanel.add(applyBtn);
        btnPanel.add(clearBtn);
        filterPanel.add(btnPanel, gbc);

        return filterPanel;
    }

    private void applyFilters() {
        model.setRowCount(0);
        TeacherFilterDAO filterDao = new TeacherFilterDAO();

        String specialization = (String) specializationCombo.getSelectedItem();
        String experience = (String) experienceCombo.getSelectedItem();
        String city = (String) cityCombo.getSelectedItem();
        String salary = (String) salaryCombo.getSelectedItem();
        String search = searchField.getText().trim();

        // Convert display text to filter values
        String expRange = null;
        if ("0-2 years".equals(experience)) expRange = "0-2";
        else if ("3-5 years".equals(experience)) expRange = "3-5";
        else if ("5+ years".equals(experience)) expRange = "5+";

        String salRange = null;
        if ("<20000".equals(salary)) salRange = "<20000";
        else if ("20000-40000".equals(salary)) salRange = "20000-40000";
        else if ("40000+".equals(salary)) salRange = "40000+";

        currentTeachers = filterDao.filterTeachers(
            "All".equalsIgnoreCase(specialization) ? null : specialization,
            expRange,
            "All".equalsIgnoreCase(city) ? null : city,
            salRange,
            search.isEmpty() ? null : search,
            null
        );

        populateTeacherTable();
    }

    private void clearFilters() {
        specializationCombo.setSelectedIndex(0);
        experienceCombo.setSelectedIndex(0);
        cityCombo.setSelectedIndex(0);
        salaryCombo.setSelectedIndex(0);
        searchField.setText("");
        refreshTable();
    }

    private void populateTeacherTable() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        dao.UserDAO userDAO = new dao.UserDAO();
        for (Teacher t : currentTeachers) {
            String joinStr = t.getJoinDate();
            
            // Fallback if joinDate is missing
            if ((joinStr == null || joinStr.equals("-")) && t.getEmail() != null) {
                java.util.Date fallback = userDAO.getCreatedAtByEmail(t.getEmail());
                if (fallback == null) fallback = userDAO.getCreatedAt(t.getUserId());
                if (fallback != null) joinStr = sdf.format(fallback);
            }
            if (joinStr == null) joinStr = "-";
            
            String city    = t.getCity()   != null ? t.getCity()                  : "--";
            String name    = t.getName()   != null ? t.getName()
                                                   : "Unspecified (#" + t.getUserId() + ")";
            String spec    = t.getSpecialization() != null ? t.getSpecialization() : "--";
            
            // Fix experience display: handle Integer and null
            String exp = t.getExperience() + (t.getExperience() == 1 ? " year" : " years");
            
            String salary  = t.getSalary() > 0 ? "₹" + ((long)t.getSalary()) : "—";
            String degree  = t.getHighestDegree() != null ? t.getHighestDegree() : "—";
            
            model.addRow(new Object[]{name, spec, exp, salary, degree, city, joinStr, ""});
        }
    }

    private void refreshTable() {
        model.setRowCount(0);
        currentTeachers = new TeacherDAO().getAllTeachers();
        populateTeacherTable();
    }

    private void openTeacherModal(Teacher editTarget) {
        final boolean isEditMode = (editTarget != null);
        String titleStr = isEditMode ? "Edit Teacher — " + editTarget.getName() : "Add New Teacher";

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), titleStr, true);
        dialog.setSize(720, 620);
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
        
        // ── NEW FIELDS ──
        JSpinner experienceSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));
        JTextField salaryField = styledField(); applyDecimalFilter(salaryField);
        JTextField degreeField = styledField();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "INACTIVE"});

        JLabel nameErr  = errLabel(); JLabel emailErr = errLabel();
        JLabel phoneErr = errLabel();
        JLabel passErr  = errLabel(); JLabel cPassErr = errLabel();
        JLabel cityErr  = errLabel(); JLabel salaryErr = errLabel();

        styleField(passField); 
        styleField(cPassField);
        styleField(experienceSpinner);
        styleField(statusCombo);

        if (isEditMode) {
            nameField.setText(editTarget.getName() != null ? editTarget.getName() : "");
            emailField.setText(editTarget.getEmail() != null ? editTarget.getEmail() : "");
            phoneField.setText(editTarget.getPhone() != null ? editTarget.getPhone() : "");
            specField.setText(editTarget.getSpecialization() != null ? editTarget.getSpecialization() : "");
            cityField.setText(editTarget.getCity() != null ? editTarget.getCity() : "");
            
            String jdStr = editTarget.getJoinDate();
            if (jdStr != null && !jdStr.equals("-")) {
                try {
                    joinChooser.setDate(new SimpleDateFormat("dd-MM-yyyy").parse(jdStr));
                } catch (Exception ex) {}
            }
            
            // ── NEW FIELDS ──
            experienceSpinner.setValue(editTarget.getExperience());
            salaryField.setText(editTarget.getSalary() > 0 ? String.valueOf((long)editTarget.getSalary()) : "");
            degreeField.setText(editTarget.getHighestDegree() != null ? editTarget.getHighestDegree() : "");
            statusCombo.setSelectedItem(editTarget.getStatus() != null ? editTarget.getStatus() : "ACTIVE");
            
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
        
        // ── NEW FORM ROWS ──
        form.add(formRow("Experience (Years)",         experienceSpinner));
        form.add(formRowWithErr("Salary (₹)",         salaryField, salaryErr));
        form.add(formRow("Highest Degree",             degreeField));
        form.add(formRow("Status",                     statusCombo));

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
                    cityField, salaryField, nameErr, emailErr, phoneErr, passErr, cPassErr, cityErr, salaryErr, isEditMode))
                return;
            try {
                String uId = isEditMode ? editTarget.getUserId() : new CounterDAO().getNextTeacherId();
                Teacher t = new Teacher();
                t.setUserId(uId); 
                t.setName(nameField.getText().trim());
                t.setEmail(emailField.getText().trim()); 
                t.setPhone(phoneField.getText().trim());
                t.setRole("TEACHER"); 
                t.setSpecialization(specField.getText().trim());
                
                java.util.Date chosenDate = joinChooser.getDate();
                if (chosenDate != null) {
                    t.setJoinDate(new SimpleDateFormat("dd-MM-yyyy").format(chosenDate));
                } else {
                    t.setJoinDate("-");
                }
                t.setCity(cityField.getText().trim()); 
                t.setAdminId("A001");
                
                // ── NEW FIELDS ──
                t.setExperience((Integer) experienceSpinner.getValue());
                if (!salaryField.getText().trim().isEmpty()) {
                    t.setSalary(Double.parseDouble(salaryField.getText().trim()));
                }
                t.setHighestDegree(degreeField.getText().trim());
                t.setStatus((String) statusCombo.getSelectedItem());

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
            JTextField cityField, JTextField salaryField,
            JLabel nameErr, JLabel emailErr, JLabel phoneErr, 
            JLabel passErr, JLabel cPassErr, JLabel cityErr, JLabel salaryErr, boolean isEditMode) {
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
        // ── Salary is mandatory and must be a positive number ──
        String salaryText = salaryField.getText().trim();
        if (salaryText.isEmpty()) {
            salaryErr.setText("Salary is required"); ok = false;
        } else {
            try {
                double salVal = Double.parseDouble(salaryText);
                if (salVal <= 0) { salaryErr.setText("Must be > 0"); ok = false; }
                else salaryErr.setText("");
            } catch (NumberFormatException e) {
                salaryErr.setText("Must be a number"); ok = false;
            }
        }
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

    private void applyDecimalFilter(JTextField f) {
        ((AbstractDocument) f.getDocument()).setDocumentFilter(new DocumentFilter() {
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
                Graphics2D g2=(Graphics2D)g.create(); 
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()?ACCENT_DARK:ACCENT); 
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                super.paintComponent(g2); 
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); 
        btn.setOpaque(false); 
        btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE); 
        btn.setFont(new Font("SansSerif",Font.BOLD,13));
        btn.setPreferredSize(new Dimension(200,38)); 
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false); 
        if (al!=null) btn.addActionListener(al); 
        return btn;
    }

    private JButton makeSecondaryButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text); 
        btn.setBackground(CARD_BG); 
        btn.setForeground(ACCENT);
        btn.setFont(new Font("SansSerif",Font.BOLD,13)); 
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT,1,true),
            new EmptyBorder(6,16,6,16)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
        if (al!=null) btn.addActionListener(al); 
        return btn;
    }
}