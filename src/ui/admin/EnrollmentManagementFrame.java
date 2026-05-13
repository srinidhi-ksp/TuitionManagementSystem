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
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.StudentDAO;
import model.Batch;
import model.Enrollment;
import model.Student;

/**
 * Enrollment Management Module
 * Shows enrollments filtered by status: Active | Completed | Cancelled
 * Architecture: Enrollment data ONLY (no payment analytics)
 */
public class EnrollmentManagementFrame extends JPanel {

    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color ACCENT_DARK = new Color(0, 102, 204);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);
    private static final Color SUCCESS     = new Color(22, 163, 74);
    private static final Color WARNING     = new Color(234, 179, 8);
    private static final Color ERROR       = new Color(220, 38, 38);

    // Per-tab state
    private JTable activeTable, completedTable, cancelledTable;
    private DefaultTableModel activeModel, completedModel, cancelledModel;
    private List<Enrollment> activeEnrollments, completedEnrollments, cancelledEnrollments;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MM-yyyy hh:mm a");

    public EnrollmentManagementFrame() {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabbedPane(), BorderLayout.CENTER);
    }

    // ─────────────────────────── HEADER ───────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PAGE_BG);
        header.setBorder(new EmptyBorder(28, 36, 12, 36));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(PAGE_BG);
        JLabel titleLbl = new JLabel("Enrollment Management");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLbl.setForeground(TEXT_PRI);
        JLabel subLbl = new JLabel("View and manage student–batch enrollments by status");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subLbl.setForeground(TEXT_SEC);
        titles.add(titleLbl);
        titles.add(subLbl);
        header.add(titles, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btns.setBackground(PAGE_BG);
        btns.add(makeSecondaryButton("↻  Refresh All", e -> refreshAllTabs()));
        btns.add(makeAccentButton("+ Enroll Student", e -> openEnrollModal(null)));
        header.add(btns, BorderLayout.EAST);
        return header;
    }

    // ─────────────────────────── TABBED PANE ───────────────────────────
    private JPanel buildTabbedPane() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(PAGE_BG);
        wrapper.setBorder(new EmptyBorder(0, 36, 36, 36));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.setBackground(CARD_BG);

        tabs.addTab("✅  Active Enrollments",    buildStatusTab("ACTIVE"));
        tabs.addTab("✔  Completed Enrollments", buildStatusTab("COMPLETED"));
        tabs.addTab("✖  Cancelled Enrollments", buildStatusTab("CANCELLED"));

        wrapper.add(tabs, BorderLayout.CENTER);
        return wrapper;
    }

    // ─────────────────────────── STATUS TAB ───────────────────────────
    private JPanel buildStatusTab(String status) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BG);
        panel.setBorder(new EmptyBorder(16, 0, 0, 0));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true));

        String[] cols = {"#", "Student Name", "Student ID", "Batch", "Enrollment Date", "Status", "Actions"};
        DefaultTableModel tModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 6; }
        };

        JTable tbl = new JTable(tModel);
        styleTable(tbl);

        // Store references
        switch (status) {
            case "ACTIVE":
                activeTable = tbl;
                activeModel = tModel;
                break;
            case "COMPLETED":
                completedTable = tbl;
                completedModel = tModel;
                break;
            case "CANCELLED":
                cancelledTable = tbl;
                cancelledModel = tModel;
                break;
        }

        TableActionEvent ev = new TableActionEvent() {
            @Override public void onEdit(int row) {
                List<Enrollment> list = getListForStatus(status);
                if (list != null && row >= 0 && row < list.size())
                    openEnrollModal(list.get(row));
            }
            @Override public void onDelete(int row) {
                if (tbl.isEditing()) tbl.getCellEditor().stopCellEditing();
                int ok = JOptionPane.showConfirmDialog(null,
                    "Remove this enrollment?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    List<Enrollment> list = getListForStatus(status);
                    if (list != null && row >= 0 && row < list.size()) {
                        Enrollment e = list.get(row);
                        if (new EnrollmentDAO().deleteEnrollment(e.getEnrollmentId()))
                            refreshTab(status);
                        else JOptionPane.showMessageDialog(null, "Failed to delete.");
                    }
                }
            }
        };

        tbl.getColumnModel().getColumn(6).setCellRenderer(new TableActionCellRender());
        tbl.getColumnModel().getColumn(6).setCellEditor(new TableActionCellEditor(ev));
        tbl.getColumnModel().getColumn(5).setCellRenderer(new StatusPillRenderer());

        // Column widths
        tbl.getColumnModel().getColumn(0).setPreferredWidth(40);
        tbl.getColumnModel().getColumn(1).setPreferredWidth(180);
        tbl.getColumnModel().getColumn(2).setPreferredWidth(80);
        tbl.getColumnModel().getColumn(3).setPreferredWidth(150);
        tbl.getColumnModel().getColumn(4).setPreferredWidth(110);
        tbl.getColumnModel().getColumn(5).setPreferredWidth(100);
        tbl.getColumnModel().getColumn(6).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(tbl);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        // Card header with count label
        JPanel cardHeader = buildCardHeader(getLabelForStatus(status), tModel, status);
        card.add(cardHeader, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        panel.add(card, BorderLayout.CENTER);

        // Initial load
        refreshTab(status);
        return panel;
    }

    private JPanel buildCardHeader(String label, DefaultTableModel tModel, String status) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 245)),
            new EmptyBorder(14, 20, 14, 20)));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        lbl.setForeground(TEXT_PRI);
        p.add(lbl, BorderLayout.WEST);

        JButton refresh = makeSecondaryButton("↻  Refresh", e -> refreshTab(status));
        p.add(refresh, BorderLayout.EAST);
        return p;
    }

    // ─────────────────────────── DATA REFRESH ───────────────────────────
    private void refreshAllTabs() {
        refreshTab("ACTIVE");
        refreshTab("COMPLETED");
        refreshTab("CANCELLED");
    }

    private void refreshTab(String status) {
        DefaultTableModel tModel = getModelForStatus(status);
        if (tModel == null) return;

        tModel.setRowCount(0);
        List<Enrollment> enrollments = new EnrollmentDAO().getEnrollmentsByStatus(status);
        storeListForStatus(status, enrollments);

        StudentDAO sDao = new StudentDAO();
        BatchDAO bDao   = new BatchDAO();

        int idx = 1;
        for (Enrollment e : enrollments) {
            String rawId = e.getStudentUserId();

            // Try multiple lookup strategies
            Student stu = sDao.getStudentById(rawId);
            if (stu == null) stu = sDao.getStudentByUserId(rawId);

            String stuName = (stu != null && stu.getName() != null)
                ? stu.getName()
                : "(unknown)";
            String stuId = (stu != null) ? stu.getUserId() : rawId;

            Batch bat = bDao.getBatchById(e.getBatchId());
            String batchIdStr = String.format("B%03d", e.getBatchId());
            String displayId = (bat != null && bat.getBatchName() != null) ? batchIdStr : batchIdStr;
            String batchName = (bat != null) 
                ? (displayId + " - " + bat.getBatchName()) 
                : "Batch #" + displayId;
            String dateStr   = e.getEnrollmentDate() != null ? DATE_FMT.format(e.getEnrollmentDate()) : "--";
            String st        = e.getStatus() != null ? e.getStatus() : status;

            tModel.addRow(new Object[]{idx++, stuName, stuId, batchName, dateStr, st, ""});
        }
    }

    // ─────────────────────────── HELPERS ───────────────────────────
    private DefaultTableModel getModelForStatus(String status) {
        switch (status) {
            case "ACTIVE":    return activeModel;
            case "COMPLETED": return completedModel;
            case "CANCELLED": return cancelledModel;
            default: return null;
        }
    }

    private List<Enrollment> getListForStatus(String status) {
        switch (status) {
            case "ACTIVE":    return activeEnrollments;
            case "COMPLETED": return completedEnrollments;
            case "CANCELLED": return cancelledEnrollments;
            default: return null;
        }
    }

    private void storeListForStatus(String status, List<Enrollment> list) {
        switch (status) {
            case "ACTIVE":    activeEnrollments    = list; break;
            case "COMPLETED": completedEnrollments = list; break;
            case "CANCELLED": cancelledEnrollments = list; break;
        }
    }

    private String getLabelForStatus(String status) {
        switch (status) {
            case "ACTIVE":    return "Active Enrollments";
            case "COMPLETED": return "Completed Enrollments";
            case "CANCELLED": return "Cancelled Enrollments";
            default: return "Enrollments";
        }
    }

    // ─────────────────────────── MODAL ───────────────────────────
    private void openEnrollModal(Enrollment editTarget) {
        final boolean isEditMode = (editTarget != null);
        String title = isEditMode ? "Edit Enrollment" : "Enroll Student";

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setSize(550, 620);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(CARD_BG);
        dialog.setLayout(new BorderLayout());
        dialog.add(buildModalTitleBar(title), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 14));
        form.setBackground(CARD_BG);
        form.setBorder(new EmptyBorder(24, 28, 12, 28));

        JComboBox<String> studentCombo = new JComboBox<>();
        studentCombo.addItem("Select Student");
        for (Student s : new StudentDAO().getAllStudents())
            studentCombo.addItem(s.getUserId() + " – " + (s.getName() != null ? s.getName() : "(unnamed)"));

        JComboBox<Batch> batchCombo = new JComboBox<>();
        batchCombo.addItem(null); // Placeholder option
        for (Batch b : new BatchDAO().getAllBatches())
            batchCombo.addItem(b);

        batchCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("Select Batch");
                    setForeground(TEXT_SEC);
                }
                return this;
            }
        });

        DateChooser dateChooser = new DateChooser();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"ACTIVE", "COMPLETED", "CANCELLED"});
        
        // Validation feedback labels
        JLabel validationLabel = new JLabel("");
        validationLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        
        JLabel statusDetailsLabel = new JLabel("");
        statusDetailsLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));

        if (isEditMode) {
            for (int i = 0; i < studentCombo.getItemCount(); i++)
                if (studentCombo.getItemAt(i).startsWith(editTarget.getStudentUserId() + " –")) {
                    studentCombo.setSelectedIndex(i); break;
                }
            for (int i = 0; i < batchCombo.getItemCount(); i++) {
                Batch b = batchCombo.getItemAt(i);
                if (b != null && b.getBatchId() == editTarget.getBatchId()) {
                    batchCombo.setSelectedIndex(i); break;
                }
            }
            statusCombo.setSelectedItem(editTarget.getStatus() != null ? editTarget.getStatus() : "ACTIVE");
            if (editTarget.getEnrollmentDate() != null) dateChooser.setDate(editTarget.getEnrollmentDate());
        }

        form.add(makeFormRow("Student", studentCombo));
        form.add(makeFormRow("Batch", batchCombo));
        form.add(validationLabel);
        form.add(statusDetailsLabel);
        form.add(makeFormRow("Enrollment Date", dateChooser));
        form.add(makeFormRow("Status", statusCombo));

        // Save button - will be enabled/disabled based on validation
        JButton saveBtn = new JButton(isEditMode ? "Update Enrollment" : "Save Enrollment");
        final boolean[] isValid = {true}; // Track validation state
        
        // Shared validation logic for student and batch selection
        Runnable validateSelection = () -> {
            String selS = studentCombo.getSelectedItem() != null ? studentCombo.getSelectedItem().toString() : "Select Student";
            Object selectedBatch = batchCombo.getSelectedItem();
            Batch selB = selectedBatch instanceof Batch ? (Batch) selectedBatch : null;

            validationLabel.setText("");
            statusDetailsLabel.setText("");
            isValid[0] = true;

            if (!selS.startsWith("Select") && selB != null) {
                String studentId = selS.split(" – ")[0].trim();

                // ===== CHECK 1: DUPLICATE ENROLLMENT =====
                String duplicateStudentName = util.ScheduleConflictValidator.checkDuplicateEnrollment(studentId, selB.getBatchId());
                if (duplicateStudentName != null) {
                    validationLabel.setText("❌ Duplicate Enrollment!");
                    statusDetailsLabel.setText("This student is already enrolled in this batch.");
                    validationLabel.setForeground(ERROR);
                    statusDetailsLabel.setForeground(ERROR);
                    isValid[0] = false;
                } else {
                    // ===== CHECK 2: SCHEDULE CONFLICT =====
                    util.ScheduleConflictValidator.ConflictInfo conflict = util.ScheduleConflictValidator.checkStudentConflict(studentId, selB.getBatchId());

                    if (conflict != null) {
                        validationLabel.setText("⚠️ Schedule Conflict Detected!");
                        statusDetailsLabel.setText("Conflicts with: " + conflict.getFormattedMessage());
                        validationLabel.setForeground(WARNING);
                        statusDetailsLabel.setForeground(WARNING);
                        isValid[0] = false;
                    } else {
                        validationLabel.setText("✅ No conflicts");
                        validationLabel.setForeground(SUCCESS);
                        isValid[0] = true;
                    }
                }
            } else {
                validationLabel.setText("ℹ️ Select student and batch");
                validationLabel.setForeground(TEXT_SEC);
                isValid[0] = false;
            }

            saveBtn.setEnabled(isValid[0] && !selS.startsWith("Select") && selB != null);
        };

        // Real-time validation on batch or student selection
        batchCombo.addActionListener(e -> validateSelection.run());
        studentCombo.addActionListener(e -> validateSelection.run());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 16));
        btnRow.setBackground(PAGE_BG);
        btnRow.add(makeSecondaryButton("Cancel", e -> dialog.dispose()));
        
        saveBtn.setBackground(ACCENT);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        saveBtn.setFocusPainted(false);
        saveBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1, true), new EmptyBorder(6, 18, 6, 18)));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setEnabled(false); // Start disabled
        
        saveBtn.addActionListener(e -> {
            try {
                String selS = studentCombo.getSelectedItem().toString();
                Batch selB  = (Batch) batchCombo.getSelectedItem();
                
                if (selS.startsWith("Select") || selB == null) {
                    JOptionPane.showMessageDialog(dialog, "Please select a student and batch.", "Incomplete Form", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                String studentId = selS.split(" – ")[0].trim();
                
                // ===== FINAL VALIDATION BEFORE SAVE =====
                String duplicateCheck = util.ScheduleConflictValidator.checkDuplicateEnrollment(studentId, selB.getBatchId());
                if (duplicateCheck != null && !isEditMode) {
                    JOptionPane.showMessageDialog(dialog,
                        "❌ DUPLICATE ENROLLMENT!\n\n" +
                        "This student is already enrolled in this batch.\n\n" +
                        "Student: " + duplicateCheck + "\n" +
                        "Batch: " + selB.getBatchName() + "\n\n" +
                        "You cannot enroll the same student twice.",
                        "Duplicate Enrollment Error", JOptionPane.ERROR_MESSAGE);
                    return; // Stop enrollment
                }
                
                // Check schedule conflict
                util.ScheduleConflictValidator.ConflictInfo conflict = 
                    util.ScheduleConflictValidator.checkStudentConflict(studentId, selB.getBatchId());
                
                if (conflict != null && !isEditMode) {
                    JOptionPane.showMessageDialog(dialog,
                        "⚠️ SCHEDULE CONFLICT DETECTED!\n\n" +
                        "This student already has another class at this time.\n\n" +
                        "Conflicting Batch:\n" +
                        "  Name: " + conflict.batchName + "\n" +
                        "  Day: " + conflict.day + "\n" +
                        "  Time: " + conflict.startTime + " – " + conflict.endTime + "\n\n" +
                        "Please choose a different batch.",
                        "Schedule Conflict", JOptionPane.WARNING_MESSAGE);
                    return; // Stop enrollment
                }
                
                Enrollment en = isEditMode ? editTarget : new Enrollment();
                if (!isEditMode) en.setEnrollmentId((int)(System.currentTimeMillis() % 100000));
                en.setStudentUserId(studentId);
                en.setBatchId(selB.getBatchId());
                en.setStatus(statusCombo.getSelectedItem().toString());
                en.setEnrollmentDate(dateChooser.getDate());

                boolean ok = isEditMode
                    ? new EnrollmentDAO().updateEnrollment(en)
                    : new EnrollmentDAO().addEnrollment(en);
                if (ok) {
                    JOptionPane.showMessageDialog(dialog, "✅ Enrollment " + (isEditMode ? "updated" : "saved") + " successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    refreshAllTabs();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "❌ Failed to save enrollment.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "❌ Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        btnRow.add(saveBtn);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getViewport().setBackground(CARD_BG);
        dialog.add(formScroll, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ─────────────────────────── UI HELPERS ───────────────────────────
    private JPanel makeFormRow(String label, JComponent comp) {
        JPanel row = new JPanel(new BorderLayout(0, 5));
        row.setBackground(CARD_BG);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(TEXT_SEC);
        comp.setPreferredSize(new Dimension(comp.getPreferredSize().width, 38));
        row.add(lbl, BorderLayout.NORTH);
        row.add(comp, BorderLayout.CENTER);
        return row;
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(46);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(235, 240, 248));
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(CARD_BG);
        t.setSelectionBackground(new Color(74, 144, 226, 30));
        t.setSelectionForeground(TEXT_PRI);
        t.setFocusable(false);
        t.getTableHeader().setBackground(new Color(248, 250, 253));
        t.getTableHeader().setForeground(TEXT_SEC);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 245)));
        t.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer rend = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(table, value, sel, foc, r, c);
                comp.setBackground(sel ? new Color(74, 144, 226, 30) : CARD_BG);
                comp.setForeground(TEXT_PRI);
                setBorder(new EmptyBorder(0, 16, 0, 0));
                return comp;
            }
        };
        for (int i = 0; i < t.getColumnCount() - 1; i++)
            t.getColumnModel().getColumn(i).setCellRenderer(rend);
    }

    private JButton makeAccentButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT_DARK : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                super.paintComponent(g2); g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE); btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(180, 38)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false); if (al != null) btn.addActionListener(al); return btn;
    }

    private JButton makeSecondaryButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text);
        btn.setBackground(CARD_BG); btn.setForeground(ACCENT);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13)); btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1, true), new EmptyBorder(6, 18, 6, 18)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (al != null) btn.addActionListener(al);
        return btn;
    }

    private JPanel buildModalTitleBar(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(NAV_BG); p.setBorder(new EmptyBorder(16, 24, 16, 24));
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 16)); lbl.setForeground(Color.WHITE);
        p.add(lbl, BorderLayout.WEST); return p;
    }

    // ─────────────────────────── STATUS PILL RENDERER ───────────────────────────
    private static class StatusPillRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean foc, int row, int col) {
            String s = value != null ? value.toString().toUpperCase() : "ACTIVE";
            Color fg, bg;
            switch (s) {
                case "ACTIVE":    fg = new Color(22, 163, 74);  bg = new Color(34, 197, 94, 25);  break;
                case "COMPLETED": fg = new Color(37, 99, 235);  bg = new Color(74, 144, 226, 25); break;
                case "CANCELLED": fg = new Color(220, 38, 38);  bg = new Color(239, 68, 68, 25);  break;
                default:          fg = new Color(107, 122, 153);bg = new Color(200, 210, 225, 25);break;
            }
            final Color finalFg = fg, finalBg = bg;
            JLabel lbl = new JLabel(s, SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(finalBg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.dispose(); super.paintComponent(g);
                }
            };
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(finalFg);
            lbl.setOpaque(false);
            lbl.setBorder(new EmptyBorder(4, 12, 4, 12));
            JPanel wrap = new JPanel(new GridBagLayout());
            wrap.setBackground(sel ? new Color(74, 144, 226, 30) : Color.WHITE);
            wrap.add(lbl);
            return wrap;
        }
    }
}
