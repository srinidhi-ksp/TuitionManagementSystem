package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import dao.SubjectDAO;
import model.Subject;

public class SubjectManagementFrame extends JPanel {

    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(30, 190, 160); // Green-teal from user's screenshot
    private static final Color ACCENT_DARK = new Color(20, 150, 130);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);

    private JTable subjectTable;
    private DefaultTableModel model;
    private List<Subject> currentSubjects;

    public SubjectManagementFrame() {
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
        JLabel title = new JLabel("Subject Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(TEXT_PRI);
        JLabel sub = new JLabel("Manage curriculum subjects and fees");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(TEXT_SEC);
        titles.add(title); titles.add(sub);
        panel.add(titles, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setBackground(PAGE_BG);
        right.add(makeSecondaryButton("↻  Refresh", e -> refreshTable()));
        right.add(makeAccentButton("+ Add New Subject", e -> openSubjectModal(null)));
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

        String[] cols = {"Subject Name", "Category", "Syllabus Version", "Monthly Fee", "Status", "Actions"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5; }
        };
        subjectTable = new JTable(model);
        styleTable(subjectTable);
        refreshTable();

        TableActionEvent ev = new TableActionEvent() {
            @Override public void onEdit(int row) {
                if (row >= 0 && row < currentSubjects.size()) {
                    openSubjectModal(currentSubjects.get(row));
                }
            }
            @Override public void onDelete(int row) {
                if (subjectTable.isEditing()) subjectTable.getCellEditor().stopCellEditing();
                int ok = JOptionPane.showConfirmDialog(null, "Delete this subject?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    Subject s = currentSubjects.get(row);
                    if (new SubjectDAO().deleteSubject(s.getSubjectId())) refreshTable();
                    else JOptionPane.showMessageDialog(null, "Failed to delete.");
                }
            }
        };
        subjectTable.getColumnModel().getColumn(5).setCellRenderer(new TableActionCellRender());
        subjectTable.getColumnModel().getColumn(5).setCellEditor(new TableActionCellEditor(ev));

        JScrollPane scroll = new JScrollPane(subjectTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        card.add(buildCardHeader("All Subjects"), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        wrapper.add(card);
        return wrapper;
    }

    private void refreshTable() {
        model.setRowCount(0);
        currentSubjects = new SubjectDAO().getAllSubjects();
        for (Subject s : currentSubjects) {
            model.addRow(new Object[]{
                s.getSubjectName(), s.getCategory(), s.getSyllabusVersion(),
                "Rs. " + s.getMonthlyFee(), s.getStatus(), ""
            });
        }
    }

    private void openSubjectModal(Subject editTarget) {
        final boolean isEditMode = (editTarget != null);
        String title = isEditMode ? "Edit Subject — " + editTarget.getSubjectName() : "Add New Subject";

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setSize(600, 480);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(CARD_BG);
        dialog.setLayout(new BorderLayout());
        dialog.add(buildModalTitleBar(title), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 20, 15));
        form.setBorder(new EmptyBorder(24, 24, 12, 24));
        form.setBackground(CARD_BG);

        JTextField nameField = styledField();
        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{"Science", "Mathematics", "Commerce", "Languages", "Arts"});
        JTextField versionField = styledField("v1.0");
        JTextField feeField = styledField();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Active", "Inactive"});
        JTextArea descArea = new JTextArea(3, 20);
        descArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200,200,200)),
            BorderFactory.createEmptyBorder(5,5,5,5)
        ));

        if (isEditMode) {
            nameField.setText(editTarget.getSubjectName());
            categoryCombo.setSelectedItem(editTarget.getCategory());
            versionField.setText(editTarget.getSyllabusVersion());
            feeField.setText(String.valueOf(editTarget.getMonthlyFee()));
            statusCombo.setSelectedItem(editTarget.getStatus());
            descArea.setText(editTarget.getDescription());
        }

        form.add(formRow("Subject Name", nameField));
        form.add(formRow("Category", categoryCombo));
        form.add(formRow("Syllabus Version", versionField));
        form.add(formRow("Monthly Fee", feeField));
        form.add(formRow("Status", statusCombo));
        form.add(formRow("Description", new JScrollPane(descArea)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        btnRow.setBackground(PAGE_BG);
        btnRow.add(makeSecondaryButton("Cancel", e -> dialog.dispose()));
        String btnText = isEditMode ? "Update Subject" : "Submit";
        btnRow.add(makeAccentButton(btnText, e -> {
            try {
                Subject s = isEditMode ? editTarget : new Subject();
                if (!isEditMode) s.setSubjectId((int)(System.currentTimeMillis() % 100000));
                s.setSubjectName(nameField.getText().trim());
                s.setCategory(categoryCombo.getSelectedItem().toString());
                s.setSyllabusVersion(versionField.getText().trim());
                s.setMonthlyFee(Double.parseDouble(feeField.getText().trim()));
                s.setStatus(statusCombo.getSelectedItem().toString());
                s.setDescription(descArea.getText().trim());

                boolean success = isEditMode ? new SubjectDAO().updateSubject(s) : new SubjectDAO().addSubject(s);
                if (success) {
                    JOptionPane.showMessageDialog(dialog, "Subject " + (isEditMode?"Updated":"Saved") + "!");
                    refreshTable(); dialog.dispose();
                } else JOptionPane.showMessageDialog(dialog, "Operation failed.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid info and numbers.");
            }
        }));

        dialog.add(new JScrollPane(form) {{ setBorder(null); getViewport().setBackground(CARD_BG); }}, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(44); t.setShowGrid(false); t.setShowHorizontalLines(true);
        t.setGridColor(new Color(235, 240, 248)); t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(CARD_BG); t.setSelectionBackground(new Color(74, 144, 226, 25));
        t.setSelectionForeground(TEXT_PRI); t.setFocusable(false);
        t.getTableHeader().setBackground(new Color(248, 250, 253));
        t.getTableHeader().setForeground(TEXT_SEC);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 245)));
        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, sel, focus, row, col);
                c.setBackground(sel ? new Color(74, 144, 226, 20) : CARD_BG); c.setForeground(TEXT_PRI);
                setBorder(new EmptyBorder(0, 16, 0, 0)); return c;
            }
        };
        for (int i = 0; i < t.getColumnCount() - 1; i++) t.getColumnModel().getColumn(i).setCellRenderer(r);
    }

    private JPanel buildCardHeader(String text) {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 245)), new EmptyBorder(14, 20, 14, 20)));
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("SansSerif", Font.BOLD, 15)); lbl.setForeground(TEXT_PRI);
        p.add(lbl, BorderLayout.WEST); return p;
    }

    private JPanel buildModalTitleBar(String text) {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(NAV_BG); p.setBorder(new EmptyBorder(16, 24, 16, 24));
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("SansSerif", Font.BOLD, 16)); lbl.setForeground(Color.WHITE);
        p.add(lbl, BorderLayout.WEST); return p;
    }

    private JPanel formRow(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 5)); p.setBackground(CARD_BG);
        JLabel lbl = new JLabel(label); lbl.setFont(new Font("SansSerif", Font.BOLD, 11)); lbl.setForeground(TEXT_SEC);
        if (comp instanceof JTextField || comp instanceof JPasswordField)
            comp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true), new EmptyBorder(6, 10, 6, 10)));
        comp.setPreferredSize(new Dimension(0, 36)); p.add(lbl, BorderLayout.NORTH); p.add(comp, BorderLayout.CENTER); return p;
    }

    private JTextField styledField() { return styledField(""); }
    private JTextField styledField(String text) {
        JTextField f = new JTextField(text); f.setFont(new Font("SansSerif", Font.PLAIN, 13)); f.setForeground(TEXT_PRI); return f;
    }

    private JButton makeAccentButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT_DARK : ACCENT); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g2); g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE); btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(160, 38)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false); if (al != null) btn.addActionListener(al); return btn;
    }

    private JButton makeSecondaryButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text); btn.setBackground(CARD_BG); btn.setForeground(ACCENT);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13)); btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT, 1, true), new EmptyBorder(6, 16, 6, 16)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); if (al != null) btn.addActionListener(al); return btn;
    }
}