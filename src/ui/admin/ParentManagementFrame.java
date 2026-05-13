package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import dao.ParentDAO;
import dao.StudentDAO;
import dao.UserDAO;
import model.Parent;
import model.Student;
import model.User;

/**
 * ParentManagementFrame
 *
 * Loads parents from the 'parents' collection.
 * Phone is fetched from the 'users' collection via user_id.
 * Supports inline Edit of occupation and annual income.
 */
public class ParentManagementFrame extends JPanel {

    private static final Color NAV_BG   = new Color(10, 27, 63);
    private static final Color ACCENT   = new Color(74, 144, 226);
    private static final Color PAGE_BG  = new Color(244, 247, 249);
    private static final Color CARD_BG  = Color.WHITE;
    private static final Color TEXT_PRI = new Color(26, 35, 64);
    private static final Color TEXT_SEC = new Color(107, 122, 153);
    private static final Color ERROR_RED = new Color(220, 53, 69);

    private JTable parentTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    /** Loaded parents for row→parent mapping */
    private List<Parent> loadedParents = new ArrayList<>();

    public ParentManagementFrame() {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);
        add(createHeader(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
    }

    // ── Header ──────────────────────────────────────────────────────────────────

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(24, 36, 12, 36));
        panel.setBackground(PAGE_BG);

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(PAGE_BG);
        JLabel title = new JLabel("Parent Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(TEXT_PRI);
        JLabel sub = new JLabel("View and manage parent / guardian records");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(TEXT_SEC);
        titles.add(title);
        titles.add(sub);
        panel.add(titles, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setBackground(PAGE_BG);

        searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(220, 36));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) {
                filterTable(searchField.getText().toLowerCase().trim());
            }
        });

        JButton refreshBtn = makeAccentButton("↻  Refresh", ev -> refreshTable());
        rightPanel.add(searchField);
        rightPanel.add(refreshBtn);
        panel.add(rightPanel, BorderLayout.EAST);
        return panel;
    }

    // ── Body / Table ─────────────────────────────────────────────────────────────

    private JPanel createBody() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(0, 36, 36, 36));
        panel.setBackground(PAGE_BG);

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true));

        // Columns: Parent Name | Student(s) | Relation | Contact No. | Occupation | Annual Income | Edit
        String[] columns = {
            "Parent Name", "Student(s)", "Relation", "Contact No.", "Occupation", "Annual Income", "Action"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 6; }
        };

        parentTable = new JTable(tableModel);
        parentTable.setRowHeight(44);
        parentTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        parentTable.setIntercellSpacing(new Dimension(0, 0));
        parentTable.setShowGrid(false);
        parentTable.setShowHorizontalLines(true);
        parentTable.setGridColor(new Color(235, 240, 248));
        parentTable.setBackground(CARD_BG);
        parentTable.setSelectionBackground(new Color(74, 144, 226, 25));
        parentTable.setSelectionForeground(TEXT_PRI);
        parentTable.setFocusable(false);

        parentTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        parentTable.getTableHeader().setBackground(new Color(248, 250, 253));
        parentTable.getTableHeader().setForeground(TEXT_SEC);
        parentTable.getTableHeader().setBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 245)));

        // Default cell renderer
        javax.swing.table.DefaultTableCellRenderer cellRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                comp.setBackground(sel ? new Color(74, 144, 226, 20) : CARD_BG);
                comp.setForeground(TEXT_PRI);
                setBorder(new EmptyBorder(0, 14, 0, 0));
                return comp;
            }
        };
        for (int i = 0; i < 6; i++) parentTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);

        // Edit button column
        parentTable.getColumnModel().getColumn(6).setCellRenderer(new TableActionCellRender());
        parentTable.getColumnModel().getColumn(6).setCellEditor(new TableActionCellEditor(new TableActionEvent() {
            @Override public void onEdit(int row) { openParentDetailsDialog(row); }
            @Override public void onDelete(int row) { deleteParent(row); }
        }));
        parentTable.getColumnModel().getColumn(6).setPreferredWidth(100);

        refreshTable();

        JScrollPane scroll = new JScrollPane(parentTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);
        card.add(scroll, BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // ── Data Loading ──────────────────────────────────────────────────────────────

    private void refreshTable() {
        tableModel.setRowCount(0);
        loadedParents.clear();

        ParentDAO parentDAO = new ParentDAO();
        StudentDAO studentDAO = new StudentDAO();
        UserDAO userDAO = new UserDAO();

        List<Parent> parents = parentDAO.getAllParentsWithPhone();

        for (Parent p : parents) {
            loadedParents.add(p);

            // Fetch linked students
            List<Student> students = studentDAO.getStudentsByParentUserId(p.getUserId());
            String studentDisplay = students.isEmpty() ? "Not Available" : "";
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                studentDisplay += s.getName() + " (" + s.getUserId() + ")";
                if (i < students.size() - 1) studentDisplay += ", ";
            }

            String phone = p.getPhone() != null && !p.getPhone().isBlank() ? p.getPhone() : "—";

            String occupation = p.getOccupation() != null ? p.getOccupation() : "—";
            String income = p.getAnnualIncome() > 0
                ? String.format("₹%.0f", p.getAnnualIncome()) : "—";
            String relation = p.getRelationType() != null ? p.getRelationType() : "—";

            tableModel.addRow(new Object[]{
                safe(p.getName()), studentDisplay, safe(deriveRelation(p)), safe(phone), safe(occupation),
                p.getAnnualIncome() > 0 ? "₹" + String.format("%,.0f", p.getAnnualIncome()) : "Not Available", ""
            });
        }

        if (loadedParents.isEmpty()) {
            tableModel.addRow(new Object[]{"No parent data found", "—", "—", "—", "—", "—", ""});
        }
    }

    private void filterTable(String query) {
        tableModel.setRowCount(0);
        ParentDAO parentDAO = new ParentDAO();
        StudentDAO studentDAO = new StudentDAO();
        UserDAO userDAO = new UserDAO();
        List<Parent> all = parentDAO.getAllParentsWithPhone();
        loadedParents.clear();

        for (Parent p : all) {
            String name = p.getName() != null ? p.getName().toLowerCase() : "";
            boolean matches = query.isEmpty() || name.contains(query);
            if (!matches) continue;

            loadedParents.add(p);
            List<Student> students = studentDAO.getStudentsByParentUserId(p.getUserId());
            String studentDisplay = students.isEmpty() ? "Not Available" : "";
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                studentDisplay += s.getName() + " (" + s.getUserId() + ")";
                if (i < students.size() - 1) studentDisplay += ", ";
            }

            String phone = p.getPhone() != null && !p.getPhone().isBlank() ? p.getPhone() : "—";

            tableModel.addRow(new Object[]{
                p.getName(), studentDisplay,
                p.getRelationType() != null ? p.getRelationType() : "—",
                phone,
                p.getOccupation() != null ? p.getOccupation() : "—",
                p.getAnnualIncome() > 0 ? String.format("₹%.0f", p.getAnnualIncome()) : "—",
                ""
            });
        }
    }

    // ── Edit Dialog ───────────────────────────────────────────────────────────────

    private String safe(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null") || value.equals("â€”")
            ? "Not Available" : value;
    }

    private String deriveRelation(Parent parent) {
        String relation = parent != null ? parent.getRelationType() : null;
        if (relation != null && !relation.trim().isEmpty()) return relation;
        return "Guardian";
    }

    private void openEditDialog(int row) {
        if (row < 0 || row >= loadedParents.size()) return;
        Parent p = loadedParents.get(row);

        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            "Edit Parent — " + p.getName(), true);
        dialog.setSize(480, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // Title bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(NAV_BG);
        titleBar.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel titleLbl = new JLabel("Edit Parent — " + p.getName());
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setForeground(Color.WHITE);
        titleBar.add(titleLbl, BorderLayout.WEST);
        dialog.add(titleBar, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridLayout(3, 2, 16, 8));
        form.setBackground(CARD_BG);
        form.setBorder(new EmptyBorder(20, 24, 12, 24));

        JTextField nameField = styledField(); nameField.setText(p.getName() != null ? p.getName() : "");
        JTextField occField  = styledField(); occField.setText(p.getOccupation() != null ? p.getOccupation() : "");
        JTextField incField  = styledField();
        incField.setText(p.getAnnualIncome() > 0 ? String.valueOf((long) p.getAnnualIncome()) : "");

        form.add(formRow("Name", nameField));
        form.add(formRow("Occupation", occField));
        form.add(formRow("Annual Income (₹)", incField));
        dialog.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnRow.setBackground(PAGE_BG);
        btnRow.add(makeSecondaryButton("Cancel", ev -> dialog.dispose()));
        btnRow.add(makeAccentButton("✓ Save Changes", ev -> {
            String updatedName = nameField.getText().trim();
            String updatedOcc  = occField.getText().trim();
            String incText     = incField.getText().trim();

            if (updatedName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name cannot be empty."); return;
            }
            if (updatedOcc.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Occupation is required."); return;
            }
            double income = 0;
            try {
                income = incText.isEmpty() ? 0 : Double.parseDouble(incText);
                if (income <= 0) { JOptionPane.showMessageDialog(dialog, "Income must be > 0."); return; }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid income value."); return;
            }

            p.setName(updatedName);
            p.setOccupation(updatedOcc);
            p.setAnnualIncome(income);

            if (new ParentDAO().updateParent(p)) {
                JOptionPane.showMessageDialog(dialog, "Parent updated successfully!");
                refreshTable();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to update parent.");
            }
        }));
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private void openParentDetailsDialog(int row) {
        if (row < 0 || row >= loadedParents.size()) return;
        Parent p = loadedParents.get(row);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Parent - " + p.getName(), true);
        dialog.setSize(720, 430);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(NAV_BG);
        titleBar.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel titleLbl = new JLabel("Edit Parent - " + p.getName());
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setForeground(Color.WHITE);
        titleBar.add(titleLbl, BorderLayout.WEST);
        dialog.add(titleBar, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 16, 8));
        form.setBackground(CARD_BG);
        form.setBorder(new EmptyBorder(20, 24, 12, 24));

        JTextField nameField = styledField();
        nameField.setText(p.getName() != null ? p.getName() : "");
        JComboBox<String> relationCombo = new JComboBox<>(new String[]{"Father", "Mother", "Guardian"});
        if (p.getRelationType() != null) relationCombo.setSelectedItem(p.getRelationType());
        JTextField phoneField = styledField();
        phoneField.setText(p.getPhone() != null ? p.getPhone() : "");
        JTextField langField = styledField();
        langField.setText(p.getPreferredLanguage() != null ? p.getPreferredLanguage() : "");
        JTextField occField = styledField();
        occField.setText(p.getOccupation() != null ? p.getOccupation() : "");
        JTextField incField = styledField();
        incField.setText(p.getAnnualIncome() > 0 ? String.valueOf((long) p.getAnnualIncome()) : "");
        JTextField emergencyField = styledField();
        emergencyField.setText(p.getEmergencyContact() > 0 ? String.valueOf(p.getEmergencyContact()) : "");

        form.add(formRow("Name", nameField));
        form.add(formRow("Relation Type", relationCombo));
        form.add(formRow("Contact No.", phoneField));
        form.add(formRow("Preferred Language", langField));
        form.add(formRow("Occupation", occField));
        form.add(formRow("Annual Income", incField));
        form.add(formRow("Emergency Contact", emergencyField));
        dialog.add(form, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnRow.setBackground(PAGE_BG);
        btnRow.add(makeSecondaryButton("Cancel", ev -> dialog.dispose()));
        btnRow.add(makeAccentButton("Save Changes", ev -> {
            String updatedName = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String updatedOcc = occField.getText().trim();
            String incText = incField.getText().trim();
            String emergency = emergencyField.getText().trim();

            if (updatedName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name cannot be empty."); return;
            }
            if (!phone.matches("\\d{10}")) {
                JOptionPane.showMessageDialog(dialog, "Contact number must be 10 digits."); return;
            }
            if (updatedOcc.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Occupation is required."); return;
            }

            double income;
            try {
                income = incText.isEmpty() ? 0 : Double.parseDouble(incText);
                if (income <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Income must be > 0."); return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid income value."); return;
            }

            long emergencyContact;
            try {
                emergencyContact = emergency.isEmpty() ? Long.parseLong(phone) : Long.parseLong(emergency);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid emergency contact."); return;
            }

            p.setName(updatedName);
            p.setPhone(phone);
            p.setRelationType(relationCombo.getSelectedItem().toString());
            p.setPreferredLanguage(langField.getText().trim());
            p.setOccupation(updatedOcc);
            p.setAnnualIncome(income);
            p.setEmergencyContact(emergencyContact);

            if (new ParentDAO().updateParent(p)) {
                JOptionPane.showMessageDialog(dialog, "Parent updated successfully!");
                refreshTable();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Failed to update parent.");
            }
        }));
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void deleteParent(int row) {
        if (row < 0 || row >= loadedParents.size()) return;
        if (parentTable.isEditing()) parentTable.getCellEditor().stopCellEditing();

        Parent p = loadedParents.get(row);
        int ok = JOptionPane.showConfirmDialog(this,
            "Delete parent details for " + p.getName() + "?\nLinked student records will have their embedded parent details removed.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        if (new ParentDAO().deleteParent(p.getUserId())) {
            JOptionPane.showMessageDialog(this, "Parent deleted successfully.");
            refreshTable();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to delete parent.");
        }
    }

    private JPanel formRow(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(CARD_BG);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(TEXT_SEC);
        if (field instanceof JTextField) {
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        }
        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JTextField styledField() {
        JTextField f = new JTextField();
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setForeground(TEXT_PRI);
        return f;
    }

    private JButton makeAccentButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(0, 102, 204) : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g2); g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(170, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        if (al != null) btn.addActionListener(al);
        return btn;
    }

    private JButton makeSecondaryButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text);
        btn.setBackground(CARD_BG); btn.setForeground(ACCENT);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1, true),
            new EmptyBorder(6, 16, 6, 16)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (al != null) btn.addActionListener(al);
        return btn;
    }
}
