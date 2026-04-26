package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import dao.EnrollmentDAO;
import dao.StudentDAO;
import dao.BatchDAO;
import model.Enrollment;
import model.Student;
import model.Batch;

public class EnrollmentManagementFrame extends JPanel {

    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color ACCENT_DARK = new Color(0, 102, 204);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);

    private JTable table;
    private DefaultTableModel tableModel;
    private List<Enrollment> currentEnrollments;

    public EnrollmentManagementFrame() {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PAGE_BG);
        header.setBorder(new EmptyBorder(28, 36, 12, 36));

        JPanel titles = new JPanel(new GridLayout(2,1,0,4));
        titles.setBackground(PAGE_BG);
        JLabel titleLbl = new JLabel("Enrollment Management");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLbl.setForeground(TEXT_PRI);
        JLabel subLbl = new JLabel("Manage student–batch enrollments");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subLbl.setForeground(TEXT_SEC);
        titles.add(titleLbl); titles.add(subLbl);
        header.add(titles, BorderLayout.WEST);

        JButton enrollBtn = makeAccentButton("+ Enroll Student", e -> openEnrollModal(null));
        header.add(enrollBtn, BorderLayout.EAST);
        return header;
    }

    private JPanel buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(PAGE_BG);
        wrapper.setBorder(new EmptyBorder(0, 36, 36, 36));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true));

        String[] cols = {"#", "Student Name", "Batch Assigned", "Enrollment Date", "Status", "Actions"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 5; }
        };
        table = new JTable(tableModel);
        styleTable(table);

        TableActionEvent ev = new TableActionEvent() {
            @Override public void onEdit(int row) {
                if (row >= 0 && row < currentEnrollments.size()) {
                    openEnrollModal(currentEnrollments.get(row));
                }
            }
            @Override public void onDelete(int row) {
                if (table.isEditing()) table.getCellEditor().stopCellEditing();
                int ok = JOptionPane.showConfirmDialog(null, "Remove this enrollment?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    Enrollment e = currentEnrollments.get(row);
                    if (new EnrollmentDAO().deleteEnrollment(e.getEnrollmentId())) refreshTable();
                    else JOptionPane.showMessageDialog(null, "Failed to delete.");
                }
            }
        };
        table.getColumnModel().getColumn(5).setCellRenderer(new TableActionCellRender());
        table.getColumnModel().getColumn(5).setCellEditor(new TableActionCellEditor(ev));
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusPillRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        card.add(buildTableHeader(), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        wrapper.add(card, BorderLayout.CENTER);

        refreshTable();
        return wrapper;
    }

    private JPanel buildTableHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 245)),
            new EmptyBorder(14, 20, 14, 20)));
        JLabel lbl = new JLabel("All Enrollments");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        lbl.setForeground(TEXT_PRI);
        p.add(lbl, BorderLayout.WEST);

        JButton refresh = makeSecondaryButton("↻  Refresh", e -> refreshTable());
        p.add(refresh, BorderLayout.EAST);
        return p;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        currentEnrollments = new EnrollmentDAO().getAllEnrollments();
        StudentDAO sDao = new StudentDAO();
        BatchDAO   bDao = new BatchDAO();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        int idx = 1;
        for (Enrollment e : currentEnrollments) {
            String rawStudentId = e.getStudentUserId();
            Student stu = sDao.getStudentById(rawStudentId);
            if (stu == null) stu = sDao.getStudentByUserId(rawStudentId);

            String stuName = (stu != null && stu.getName() != null) ? stu.getName() : "ID: " + rawStudentId;
            Batch bat = bDao.getBatchById(e.getBatchId());
            String batchName = bat != null ? bat.getBatchName() : "Batch #" + e.getBatchId();
            String dateStr = e.getEnrollmentDate() != null ? sdf.format(e.getEnrollmentDate()) : "—";
            
            tableModel.addRow(new Object[]{idx++, stuName, batchName, dateStr, e.getStatus(), ""});
        }
    }

    private void openEnrollModal(Enrollment editTarget) {
        final boolean isEditMode = (editTarget != null);
        String title = isEditMode ? "Edit Enrollment" : "Enroll Student";

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setSize(480, 480);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(CARD_BG);
        dialog.setLayout(new BorderLayout());
        dialog.add(buildModalTitleBar(title), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 12));
        form.setBackground(CARD_BG);
        form.setBorder(new EmptyBorder(24, 24, 12, 24));

        JComboBox<String> studentCombo = new JComboBox<>();
        studentCombo.addItem("Select Student");
        for (Student s : new StudentDAO().getAllStudents())
            studentCombo.addItem(s.getUserId() + " – " + (s.getName() != null ? s.getName() : "(unnamed)"));

        // Load all batches without filtering
        JComboBox<Batch> batchCombo = new JComboBox<>();
        List<Batch> batches = new BatchDAO().getAllBatches();
        for (Batch batch : batches) {
            batchCombo.addItem(batch);
        }

        DateChooser dateChooser = new DateChooser();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Active", "Inactive", "Completed", "Cancelled"});

        if (isEditMode) {
            for (int i=0; i<studentCombo.getItemCount(); i++)
                if (studentCombo.getItemAt(i).startsWith(editTarget.getStudentUserId() + " –")) {
                    studentCombo.setSelectedIndex(i);
                    break;
                }
            
            // Re-select batch
            for (int i=0; i<batchCombo.getItemCount(); i++) {
                Batch b = batchCombo.getItemAt(i);
                if (b != null && b.getBatchId() == editTarget.getBatchId()) {
                    batchCombo.setSelectedIndex(i);
                    break;
                }
            }
                
            statusCombo.setSelectedItem(editTarget.getStatus());
            if (editTarget.getEnrollmentDate() != null) dateChooser.setDate(editTarget.getEnrollmentDate());
        }

        form.add(makeFormRow("Student", studentCombo));
        form.add(makeFormRow("Batch", batchCombo));
        form.add(makeFormRow("Enrollment Date", dateChooser));
        form.add(makeFormRow("Status", statusCombo));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 16));
        btnRow.setBackground(PAGE_BG);
        btnRow.add(makeSecondaryButton("Cancel", e -> dialog.dispose()));
        String btnText = isEditMode ? "Update Enrollment" : "Save Enrollment";
        btnRow.add(makeAccentButton(btnText, e -> {
            try {
                String selS = studentCombo.getSelectedItem().toString();
                Batch selB = (Batch) batchCombo.getSelectedItem();
                
                if (selS.startsWith("Select") || selB == null) {
                    JOptionPane.showMessageDialog(dialog, "Please select student and a valid batch."); return;
                }
                
                Enrollment en = isEditMode ? editTarget : new Enrollment();
                if (!isEditMode) en.setEnrollmentId((int)(System.currentTimeMillis() % 100000));
                en.setStudentUserId(selS.split(" – ")[0].trim());
                en.setBatchId(selB.getBatchId());
                en.setStatus(statusCombo.getSelectedItem().toString());
                en.setEnrollmentDate(dateChooser.getDate());

                boolean ok = isEditMode ? new EnrollmentDAO().updateEnrollment(en) : new EnrollmentDAO().addEnrollment(en);
                if (ok) {
                    JOptionPane.showMessageDialog(dialog, "Saved!"); refreshTable(); dialog.dispose();
                } else JOptionPane.showMessageDialog(dialog, "Failed.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        }));

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPanel makeFormRow(String label, JComponent comp) {
        JPanel row = new JPanel(new BorderLayout(0, 4)); row.setBackground(CARD_BG);
        JLabel lbl = new JLabel(label); lbl.setFont(new Font("SansSerif", Font.BOLD, 12)); lbl.setForeground(TEXT_SEC);
        comp.setPreferredSize(new Dimension(comp.getPreferredSize().width, 38));
        row.add(lbl, BorderLayout.NORTH); row.add(comp, BorderLayout.CENTER); return row;
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 13)); t.setRowHeight(44);
        t.setShowGrid(false); t.setShowHorizontalLines(true); t.setGridColor(new Color(235, 240, 248));
        t.setIntercellSpacing(new Dimension(0, 0)); t.setBackground(CARD_BG);
        t.setSelectionBackground(new Color(74, 144, 226, 30)); t.setSelectionForeground(TEXT_PRI);
        t.setFocusable(false);
        t.getTableHeader().setBackground(new Color(248, 250, 253));
        t.getTableHeader().setForeground(TEXT_SEC);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0, new Color(230,235,245)));
        DefaultTableCellRenderer rend = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(table, value, sel, foc, r, c);
                comp.setBackground(sel ? new Color(74,144,226,30) : CARD_BG); comp.setForeground(TEXT_PRI);
                setBorder(new EmptyBorder(0, 16, 0, 0)); return comp;
            }
        };
        for (int i = 0; i < t.getColumnCount() - 1; i++) t.getColumnModel().getColumn(i).setCellRenderer(rend);
    }

    private JButton makeAccentButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT_DARK : ACCENT); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                super.paintComponent(g2); g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setOpaque(false); btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE); btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(170, 38)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false); if (al != null) btn.addActionListener(al); return btn;
    }

    private JButton makeSecondaryButton(String text, java.awt.event.ActionListener al) {
        JButton btn = new JButton(text); btn.setBackground(CARD_BG); btn.setForeground(ACCENT);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13)); btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ACCENT, 1, true), new EmptyBorder(6, 18, 6, 18)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); if (al != null) btn.addActionListener(al); return btn;
    }

    private JPanel buildModalTitleBar(String text) {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(NAV_BG); p.setBorder(new EmptyBorder(16, 24, 16, 24));
        JLabel lbl = new JLabel(text); lbl.setFont(new Font("SansSerif", Font.BOLD, 16)); lbl.setForeground(Color.WHITE);
        p.add(lbl, BorderLayout.WEST); return p;
    }

    private static class StatusPillRenderer implements TableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean foc, int row, int col) {
            String s = value != null ? value.toString() : "Active";
            boolean active = "Active".equalsIgnoreCase(s);
            JLabel lbl = new JLabel(s, SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(active ? new Color(34,197,94,30) : new Color(239,68,68,25)); g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.dispose(); super.paintComponent(g);
                }
            };
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11)); lbl.setForeground(active ? new Color(22,163,74) : new Color(220,38,38));
            lbl.setOpaque(false); lbl.setBorder(new EmptyBorder(4, 12, 4, 12));
            JPanel wrap = new JPanel(new GridBagLayout()); wrap.setBackground(sel ? new Color(74,144,226,30) : Color.WHITE); wrap.add(lbl); return wrap;
        }
    }
}
