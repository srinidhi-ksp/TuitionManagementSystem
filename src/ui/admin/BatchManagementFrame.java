package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import dao.BatchDAO;
import dao.SubjectDAO;
import dao.TeacherDAO;
import dao.TimeslotDAO;
import model.Batch;
import model.ScheduleEntry;
import model.Subject;
import model.Teacher;
import model.Timeslot;

/**
 * Batch Management frame — updated per spec §4.
 * Changes: replaced Start/End time spinners with Timeslot JComboBox,
 *          replaced single Day dropdown with 7 JCheckBox multi-day selector,
 *          teacher conflict check uses new hasTeacherConflict() with timeslotId.
 */
public class BatchManagementFrame extends JPanel {

    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color ACCENT_DARK = new Color(0, 102, 204);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);

    private static final String[] DAY_NAMES = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

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

        String[] cols = {"Batch Name", "Subject", "Assigned Teacher", "Schedule", "Mode", "Actions"};
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
        SubjectDAO subjectDAO = new SubjectDAO();
        TeacherDAO teacherDAO = new TeacherDAO();
        for (Batch b : currentBatches) {
            String schedule = buildScheduleSummary(b);
            Subject subject = subjectDAO.getSubjectById(b.getSubjectId());
            Teacher teacher = teacherDAO.getTeacherById(b.getTeacherUserId());
            model.addRow(new Object[]{
                safe(b.getBatchName()),
                subject != null ? safe(subject.getSubjectName()) : "Not Available",
                teacher != null ? safe(teacher.getName()) : "Not Available",
                schedule, safe(b.getClassMode()), ""
            });
        }
    }

    /** Build a short schedule summary string for the table. */
    private String buildScheduleSummary(Batch b) {
        if (b.getScheduleEntries() != null && !b.getScheduleEntries().isEmpty()) {
            List<String> days = new ArrayList<>();
            for (ScheduleEntry e : b.getScheduleEntries()) days.add(e.getDay());
            String tsId = b.getScheduleEntries().get(0).getTimeslotId();
            // Resolve label
            Timeslot ts = new TimeslotDAO().findById(tsId);
            String label = ts != null ? ts.getLabel() : tsId;
            return String.join(" + ", days) + " | " + safe(label);
        }
        return b.getTiming() != null ? b.getTiming() : "—";
    }

    // ── Batch add/edit dialog ─────────────────────────────────────────────────

    private String safe(String value) {
        return value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null") || value.equals("â€”")
            ? "Not Available" : value;
    }

    private String safeDay(String day) {
        return day == null || day.trim().isEmpty() ? "Not Available" : day.trim().toUpperCase();
    }

    private String formatTime(String value) {
        if (value == null || value.trim().isEmpty()) return "Not Available";
        String text = value.trim();
        java.util.List<java.text.SimpleDateFormat> parsers = java.util.Arrays.asList(
            new java.text.SimpleDateFormat("HH:mm:ss"),
            new java.text.SimpleDateFormat("HH:mm"),
            new java.text.SimpleDateFormat("hh:mm a")
        );
        for (java.text.SimpleDateFormat parser : parsers) {
            try {
                parser.setLenient(false);
                return new java.text.SimpleDateFormat("hh:mm a").format(parser.parse(text)).toUpperCase();
            } catch (Exception ignored) {}
        }
        return text;
    }

    private void openBatchModal(Batch editTarget) {
        final boolean isEditMode = (editTarget != null);
        String title = isEditMode ? "Edit Batch — " + editTarget.getBatchName() : "Add New Batch";

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setSize(640, 600);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(CARD_BG);
        dialog.setLayout(new BorderLayout());
        dialog.add(buildModalTitleBar(title), BorderLayout.NORTH);

        // ── Form ──────────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridLayout(0, 2, 20, 14));
        form.setBorder(new EmptyBorder(24, 24, 20, 24));
        form.setBackground(CARD_BG);

        // Subject combo
        JComboBox<String> subjectCombo = new JComboBox<>();
        subjectCombo.addItem("Select Subject");
        for (Subject s : new SubjectDAO().getAllSubjects())
            subjectCombo.addItem(s.getSubjectId() + " – " + s.getSubjectName());

        // Teacher combo
        JComboBox<String> teacherCombo = new JComboBox<>();
        teacherCombo.addItem("Select Teacher");
        for (Teacher t : new TeacherDAO().getAllTeachers()) {
            teacherCombo.addItem(t.getUserId() + " – " + (t.getName() != null ? t.getName() : t.getSpecialization()));
        }

        // Class level
        JComboBox<String> classLevelCombo = new JComboBox<>(
            new String[]{"Select Class", "Class 8", "Class 9", "Class 10", "Class 11", "Class 12"});

        // Class mode
        JComboBox<String> modeCombo = new JComboBox<>(new String[]{"Select Mode", "Online", "Offline"});

        // Meeting link
        JTextField linkField = styledField();

        // Batch name
        JTextField nameField = styledField();

        // ── Time Slot combo — §4.1 ────────────────────────────────────────────
        List<Timeslot> slots = new TimeslotDAO().findAllOrderedByStart();
        JComboBox<Timeslot> timeslotCombo = new JComboBox<>(slots.toArray(new Timeslot[0]));
        timeslotCombo.setRenderer((list, value, index, isSelected, hasFocus) -> {
            JLabel lbl = new JLabel(value != null ? value.getLabel() : "");
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            lbl.setOpaque(isSelected);
            if (isSelected) {
                lbl.setBackground(list.getSelectionBackground());
                lbl.setForeground(list.getSelectionForeground());
            }
            return lbl;
        });

        // ── Multi-day checkboxes — §4.2 ───────────────────────────────────────
        JPanel dayPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dayPanel.setBackground(CARD_BG);
        JCheckBox[] dayBoxes = new JCheckBox[DAY_NAMES.length];
        for (int i = 0; i < DAY_NAMES.length; i++) {
            dayBoxes[i] = new JCheckBox(DAY_NAMES[i]);
            dayBoxes[i].setFont(new Font("SansSerif", Font.PLAIN, 12));
            dayBoxes[i].setBackground(CARD_BG);
            dayPanel.add(dayBoxes[i]);
        }

        // ── Pre-populate for edit mode ────────────────────────────────────────
        if (isEditMode) {
            nameField.setText(editTarget.getBatchName());
            linkField.setText(editTarget.getMeetingLink());
            modeCombo.setSelectedItem(editTarget.getClassMode());
            classLevelCombo.setSelectedItem(editTarget.getCategory());

            // Pre-select subject and teacher
            for (int i = 0; i < subjectCombo.getItemCount(); i++)
                if (subjectCombo.getItemAt(i).startsWith(editTarget.getSubjectId() + " –"))
                    subjectCombo.setSelectedIndex(i);
            for (int i = 0; i < teacherCombo.getItemCount(); i++)
                if (teacherCombo.getItemAt(i).startsWith(editTarget.getTeacherUserId() + " –"))
                    teacherCombo.setSelectedIndex(i);

            // Pre-select timeslot
            if (editTarget.getTimeslotId() != null) {
                for (int i = 0; i < timeslotCombo.getItemCount(); i++) {
                    if (timeslotCombo.getItemAt(i).getId().equals(editTarget.getTimeslotId())) {
                        timeslotCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }

            // Pre-check days from scheduleEntries
            List<String> existingDays = editTarget.getDays();
            for (int i = 0; i < DAY_NAMES.length; i++) {
                dayBoxes[i].setSelected(existingDays.contains(DAY_NAMES[i]));
            }
        }

        // ── Layout rows — §4.3 ────────────────────────────────────────────────
        form.add(formRow("Batch Name",           nameField));
        form.add(formRow("Class / Standard",     classLevelCombo));
        form.add(formRow("Subject",              subjectCombo));
        form.add(formRow("Assigned Teacher",     teacherCombo));
        form.add(formRow("Class Mode",           modeCombo));
        form.add(formRow("Time Slot",            timeslotCombo));
        form.add(formRow("Days of Week",         dayPanel));
        form.add(formRow("Meeting Link (opt.)",  linkField));

        // ── Button row ────────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        btnRow.setBackground(PAGE_BG);
        btnRow.add(makeSecondaryButton("Cancel", e -> dialog.dispose()));
        String btnText = isEditMode ? "Update Batch" : "Save Batch";
        btnRow.add(makeAccentButton(btnText, e -> {
            try {
                // ── 1. Basic validation ───────────────────────────────────────
                if (nameField.getText().trim().isEmpty()) {
                    showError(dialog, "Batch name is required.");
                    return;
                }
                if (subjectCombo.getSelectedIndex() == 0) {
                    showError(dialog, "Please select a subject.");
                    return;
                }
                if (teacherCombo.getSelectedIndex() == 0) {
                    showError(dialog, "Please select a teacher.");
                    return;
                }
                if (modeCombo.getSelectedIndex() == 0) {
                    showError(dialog, "Please select a class mode.");
                    return;
                }
                if (classLevelCombo.getSelectedIndex() == 0) {
                    showError(dialog, "Please select a class / standard.");
                    return;
                }

                // ── 2. At least one day checked — §4.2 validation ─────────────
                List<String> selectedDays = new ArrayList<>();
                for (int i = 0; i < DAY_NAMES.length; i++) {
                    if (dayBoxes[i].isSelected()) selectedDays.add(DAY_NAMES[i]);
                }
                if (selectedDays.isEmpty()) {
                    showError(dialog, "Please select at least one day of the week.");
                    return;
                }

                // ── 3. Get selected timeslot ──────────────────────────────────
                Timeslot selectedTs = (Timeslot) timeslotCombo.getSelectedItem();
                if (selectedTs == null) {
                    showError(dialog, "Please select a time slot.");
                    return;
                }

                // ── 4. Extract teacher ID ─────────────────────────────────────
                String selT = teacherCombo.getSelectedItem().toString();
                String teacherId = selT.contains(" – ") ? selT.split(" – ")[0] : null;

                // ── 5. Teacher conflict check — §5 ────────────────────────────
                if (teacherId != null) {
                    int excludeId = isEditMode ? editTarget.getBatchId() : -1;
                    Batch conflict = new BatchDAO().hasTeacherConflict(
                        teacherId, selectedTs.getId(), selectedDays, excludeId);

                    if (conflict != null) {
                        // Find the conflicting day
                        String conflictDay = selectedDays.stream()
                            .filter(d -> conflict.getDays().contains(d))
                            .findFirst().orElse(selectedDays.get(0));

                        JOptionPane.showMessageDialog(dialog,
                            "⚠ Teacher Conflict Detected\n\n"
                            + "\"" + selT.split(" – ")[1] + "\" is already assigned to\n"
                            + "\"" + conflict.getBatchName() + "\"\n"
                            + "on " + conflictDay + " at " + selectedTs.getLabel() + ".\n\n"
                            + "A teacher cannot be assigned to two batches\n"
                            + "on the same day and time slot.\n\n"
                            + "Please select a different teacher or time slot.",
                            "Teacher Conflict", JOptionPane.WARNING_MESSAGE);
                        return; // Keep form open
                    }
                }

                // ── 6. Build and save the batch ───────────────────────────────
                Batch b = isEditMode ? editTarget : new Batch();
                if (!isEditMode) b.setBatchId((int)(System.currentTimeMillis() % 100000));
                b.setBatchName(nameField.getText().trim());
                b.setCategory(classLevelCombo.getSelectedItem().toString());

                String selS = subjectCombo.getSelectedItem().toString();
                if (selS.contains(" – ")) b.setSubjectId(Integer.parseInt(selS.split(" – ")[0]));
                if (selT.contains(" – ")) b.setTeacherUserId(selT.split(" – ")[0]);

                b.setClassMode(modeCombo.getSelectedItem().toString());
                b.setMeetingLink(linkField.getText().trim());
                b.setTimeslotId(selectedTs.getId());

                // Build scheduleEntries
                List<ScheduleEntry> entries = new ArrayList<>();
                for (String day : selectedDays) {
                    entries.add(new ScheduleEntry(day, selectedTs.getId()));
                }
                b.setScheduleEntries(entries);

                // Also set timing string for legacy display compat
                b.setTiming(String.join("+", selectedDays) + " " + selectedTs.getLabel());

                // Extract standard from category
                String cat = b.getCategory();
                if (cat != null && cat.toLowerCase().startsWith("class ")) {
                    b.setStandard(cat.substring(6).trim());
                } else {
                    b.setStandard(cat);
                }

                boolean ok = isEditMode ? new BatchDAO().updateBatch(b) : new BatchDAO().addBatch(b);
                if (ok) {
                    JOptionPane.showMessageDialog(dialog,
                        "✅ Batch " + (isEditMode ? "updated" : "saved") + " successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshTable();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to save batch.", "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }));

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    private void showError(JDialog parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Validation Error", JOptionPane.WARNING_MESSAGE);
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
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 245)),
            new EmptyBorder(14, 20, 14, 20)));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 15)); lbl.setForeground(TEXT_PRI);
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
            comp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        comp.setPreferredSize(new Dimension(0, 36));
        p.add(lbl, BorderLayout.NORTH); p.add(comp, BorderLayout.CENTER); return p;
    }

    private JTextField styledField() {
        return new JTextField() {{ setFont(new Font("SansSerif", Font.PLAIN, 13)); setForeground(TEXT_PRI); }};
    }

    private JButton makeAccentButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT_DARK : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
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
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1, true), new EmptyBorder(6, 16, 6, 16)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); if (al != null) btn.addActionListener(al); return btn;
    }
}
