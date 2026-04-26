package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Calendar;
import java.util.List;
import dao.BatchDAO;
import dao.SubjectDAO;
import dao.TeacherDAO;
import model.Batch;
import model.Subject;
import model.Teacher;
import util.BatchFormValidator;

public class BatchManagementFrame extends JPanel {

    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color ACCENT_DARK = new Color(0, 102, 204);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);

    private JTable batchTable;
    private DefaultTableModel model;
    private List<Batch> currentBatches;

    public BatchManagementFrame() {
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
        JLabel title = new JLabel("Batch Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(TEXT_PRI);
        JLabel sub = new JLabel("Manage class batches and schedules");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(TEXT_SEC);
        titles.add(title); titles.add(sub);
        panel.add(titles, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setBackground(PAGE_BG);
        right.add(makeSecondaryButton("↻  Refresh", e -> refreshTable()));
        right.add(makeAccentButton("+ Add New Batch", e -> openBatchModal(null)));
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

        String[] cols = {"Batch Name", "Subject ID", "Assigned Teacher", "Timing", "Mode", "Actions"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5; }
        };
        batchTable = new JTable(model);
        styleTable(batchTable);
        refreshTable();

        TableActionEvent ev = new TableActionEvent() {
            @Override public void onEdit(int row) {
                if (row >= 0 && row < currentBatches.size()) {
                    openBatchModal(currentBatches.get(row));
                }
            }
            @Override public void onDelete(int row) {
                if (batchTable.isEditing()) batchTable.getCellEditor().stopCellEditing();
                int ok = JOptionPane.showConfirmDialog(null, "Delete this batch?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    Batch b = currentBatches.get(row);
                    if (new BatchDAO().deleteBatch(b.getBatchId())) refreshTable();
                    else JOptionPane.showMessageDialog(null, "Failed to delete.");
                }
            }
        };
        batchTable.getColumnModel().getColumn(5).setCellRenderer(new TableActionCellRender());
        batchTable.getColumnModel().getColumn(5).setCellEditor(new TableActionCellEditor(ev));

        JScrollPane scroll = new JScrollPane(batchTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        card.add(buildCardHeader("All Batches"), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        wrapper.add(card);
        return wrapper;
    }

    private void refreshTable() {
        model.setRowCount(0);
        currentBatches = new BatchDAO().getAllBatches();
        for (Batch b : currentBatches) {
            String timing = b.getTiming() != null ? b.getTiming() : "—";
            model.addRow(new Object[]{
                b.getBatchName(), b.getSubjectId(), b.getTeacherUserId(),
                timing, b.getClassMode(), ""
            });
        }
    }

    private void openBatchModal(Batch editTarget) {
        final boolean isEditMode = (editTarget != null);
        String title = isEditMode ? "Edit Batch — " + editTarget.getBatchName() : "Add New Batch";

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setSize(600, 560);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(CARD_BG);
        dialog.setLayout(new BorderLayout());
        dialog.add(buildModalTitleBar(title), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 2, 20, 14));
        form.setBorder(new EmptyBorder(24, 24, 20, 24));
        form.setBackground(CARD_BG);

        JComboBox<String> subjectCombo = new JComboBox<>();
        subjectCombo.addItem("Select Subject");
        for (Subject s : new SubjectDAO().getAllSubjects())
            subjectCombo.addItem(s.getSubjectId() + " – " + s.getSubjectName());

        JComboBox<String> teacherCombo = new JComboBox<>();
        teacherCombo.addItem("Select Teacher");
        // Always load ALL teachers as per requirement
        for (Teacher t : new TeacherDAO().getAllTeachers()) {
            teacherCombo.addItem(t.getUserId() + " – " + (t.getName() != null ? t.getName() : t.getSpecialization()));
        }

        JTextField nameField  = styledField();
        TimeChooser startPicker = new TimeChooser();
        TimeChooser endPicker   = new TimeChooser();
        JTextField linkField  = styledField();
        JComboBox<String> modeCombo = new JComboBox<>(new String[]{"Select Mode", "Online", "Offline"});
        JComboBox<String> classLevelCombo = new JComboBox<>(new String[]{"Select Class", "Class 10", "Class 11", "Class 12"});

        if (isEditMode) {
            nameField.setText(editTarget.getBatchName());
            linkField.setText(editTarget.getMeetingLink());
            modeCombo.setSelectedItem(editTarget.getClassMode());
            classLevelCombo.setSelectedItem(editTarget.getCategory());
            
            // Pre-select combos
            for (int i=0; i<subjectCombo.getItemCount(); i++)
                if (subjectCombo.getItemAt(i).startsWith(editTarget.getSubjectId() + " –")) subjectCombo.setSelectedIndex(i);
            for (int i=0; i<teacherCombo.getItemCount(); i++)
                if (teacherCombo.getItemAt(i).startsWith(editTarget.getTeacherUserId() + " –")) teacherCombo.setSelectedIndex(i);
            
            if (editTarget.getStartTime() != null) startPicker.setTime(editTarget.getStartTime());
            if (editTarget.getEndTime() != null) endPicker.setTime(editTarget.getEndTime());
        } else {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 0);
            startPicker.setTime(cal.getTime());
            cal.set(Calendar.HOUR_OF_DAY, 11);
            endPicker.setTime(cal.getTime());
        }

        form.add(formRow("Batch Name",          nameField));
        form.add(formRow("Class/Standard",      classLevelCombo));
        form.add(formRow("Subject",             subjectCombo));
        form.add(formRow("Assigned Teacher",    teacherCombo));
        form.add(formRow("Class Mode",          modeCombo));
        form.add(formRow("Start Time",          startPicker));
        form.add(formRow("End Time",            endPicker));
        form.add(formRow("Meeting Link (opt.)", linkField));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        btnRow.setBackground(PAGE_BG);
        btnRow.add(makeSecondaryButton("Cancel", e -> dialog.dispose()));
        String btnText = isEditMode ? "Update Batch" : "Save Batch";
        btnRow.add(makeAccentButton(btnText, e -> {
            try {
                // ===== VALIDATE FORM =====
                String validationError = BatchFormValidator.validateBatchForm(
                    nameField.getText(),
                    classLevelCombo,
                    subjectCombo,
                    teacherCombo,
                    modeCombo,
                    startPicker.getTime(),
                    endPicker.getTime(),
                    linkField.getText()
                );

                if (validationError != null) {
                    BatchFormValidator.showError(dialog, validationError);
                    return; // Stop saving if validation fails
                }

                // ===== VALIDATION PASSED - SAVE DATA =====
                Batch b = isEditMode ? editTarget : new Batch();
                if (!isEditMode) b.setBatchId((int)(System.currentTimeMillis() % 100000));
                b.setBatchName(nameField.getText().trim());
                b.setCategory(classLevelCombo.getSelectedItem().toString());

                String selS = subjectCombo.getSelectedItem().toString();
                if (selS.contains(" – ")) b.setSubjectId(Integer.parseInt(selS.split(" – ")[0]));
                String selT = teacherCombo.getSelectedItem().toString();
                if (selT.contains(" – ")) b.setTeacherUserId(selT.split(" – ")[0]);

                b.setClassMode(modeCombo.getSelectedItem().toString());
                b.setStartTime(startPicker.getTime());
                b.setEndTime(endPicker.getTime());
                b.setTiming(startPicker.getTimeString() + " - " + endPicker.getTimeString());
                b.setMeetingLink(linkField.getText().trim());

                boolean ok = isEditMode ? new BatchDAO().updateBatch(b) : new BatchDAO().addBatch(b);
                if (ok) {
                    JOptionPane.showMessageDialog(dialog, "✅ Batch " + (isEditMode ? "updated" : "saved") + " successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshTable(); dialog.dispose();
                } else JOptionPane.showMessageDialog(dialog, "Failed to save batch.", "Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }));

        dialog.add(form, BorderLayout.CENTER);
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
        if (comp instanceof JTextField)
            comp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true), new EmptyBorder(6, 10, 6, 10)));
        comp.setPreferredSize(new Dimension(0, 36)); p.add(lbl, BorderLayout.NORTH); p.add(comp, BorderLayout.CENTER); return p;
    }

    private JTextField styledField() { return new JTextField() {{ setFont(new Font("SansSerif", Font.PLAIN, 13)); setForeground(TEXT_PRI); }}; }

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