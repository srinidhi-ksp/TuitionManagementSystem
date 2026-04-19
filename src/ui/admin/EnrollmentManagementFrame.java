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

    // ── Design tokens ──────────────────────────────────────────────────────────
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

    // ── Header ─────────────────────────────────────────────────────────────────
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
        titles.add(titleLbl);
        titles.add(subLbl);
        header.add(titles, BorderLayout.WEST);

        JButton enrollBtn = makeAccentButton("+ Enroll Student");
        enrollBtn.addActionListener(e -> openEnrollModal());
        header.add(enrollBtn, BorderLayout.EAST);
        return header;
    }

    // ── Content card ───────────────────────────────────────────────────────────
    private JPanel buildContent() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(PAGE_BG);
        wrapper.setBorder(new EmptyBorder(0, 36, 36, 36));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true),
            new EmptyBorder(0, 0, 0, 0)
        ));

        // Table columns
        String[] cols = {"#", "Student Name", "Batch Assigned", "Enrollment Date", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);

        // Status pill renderer
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
            new EmptyBorder(14, 20, 14, 20)
        ));
        JLabel lbl = new JLabel("All Enrollments");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        lbl.setForeground(TEXT_PRI);
        p.add(lbl, BorderLayout.WEST);

        JButton refresh = makeSecondaryButton("↻  Refresh");
        refresh.addActionListener(e -> refreshTable());
        p.add(refresh, BorderLayout.EAST);
        return p;
    }

    // ── Refresh ────────────────────────────────────────────────────────────────
    private void refreshTable() {
        tableModel.setRowCount(0);
        currentEnrollments = new EnrollmentDAO().getAllEnrollments();
        StudentDAO sDao = new StudentDAO();
        BatchDAO   bDao = new BatchDAO();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        int idx = 1;
        for (Enrollment e : currentEnrollments) {
            String rawStudentId = e.getStudentUserId(); // e.g. "S001", "U01", or null

            // ── Multi-strategy student name lookup ──────────────────────────
            Student stu = null;
            if (rawStudentId != null && !rawStudentId.isEmpty()) {
                // Strategy 1: direct _id match (covers "S001" stored as _id)
                stu = sDao.getStudentById(rawStudentId);
                // Strategy 2: user_id field match (covers "U01" → students.user_id)
                if (stu == null) stu = sDao.getStudentByUserId(rawStudentId);
            }

            String stuName;
            if (stu != null && stu.getName() != null && !stu.getName().isEmpty()) {
                stuName = stu.getName();
            } else if (rawStudentId != null && !rawStudentId.isEmpty()) {
                stuName = "ID: " + rawStudentId; // show raw ID if name not resolved
            } else {
                stuName = "(Unknown)";
            }

            Batch  bat      = bDao.getBatchById(e.getBatchId());
            String batchName = bat != null ? bat.getBatchName() : "Batch #" + e.getBatchId();
            String dateStr   = e.getEnrollmentDate() != null ? sdf.format(e.getEnrollmentDate()) : "—";
            String status    = e.getStatus() != null ? e.getStatus() : "Active";

            tableModel.addRow(new Object[]{idx++, stuName, batchName, dateStr, status});
        }
    }

    // ── Enroll Modal ───────────────────────────────────────────────────────────
    private void openEnrollModal() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Enroll Student", true);
        dialog.setSize(480, 360);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(CARD_BG);
        dialog.setLayout(new BorderLayout());

        // Title bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(NAV_BG);
        titleBar.setBorder(new EmptyBorder(18, 24, 18, 24));
        JLabel titleLbl = new JLabel("Enroll a Student into a Batch");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLbl.setForeground(Color.WHITE);
        titleBar.add(titleLbl, BorderLayout.WEST);

        // Form
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 16));
        form.setBackground(CARD_BG);
        form.setBorder(new EmptyBorder(28, 28, 20, 28));

        // Student combo
        JComboBox<String> studentCombo = new JComboBox<>();
        studentCombo.addItem("Select Student");
        List<Student> students = new StudentDAO().getAllStudents();
        for (Student s : students) {
            studentCombo.addItem(s.getUserId() + " – " + (s.getName() != null ? s.getName() : "(unnamed)"));
        }

        // Batch combo
        JComboBox<String> batchCombo = new JComboBox<>();
        batchCombo.addItem("Select Batch");
        List<Batch> batches = new BatchDAO().getAllBatches();
        for (Batch b : batches) {
            batchCombo.addItem(b.getBatchId() + " – " + b.getBatchName());
        }

        // Status combo
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Active", "Inactive"});

        form.add(makeFormRow("Student", studentCombo));
        form.add(makeFormRow("Batch", batchCombo));
        form.add(makeFormRow("Status", statusCombo));

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 16));
        btnRow.setBackground(new Color(244, 247, 249));
        JButton cancel = makeSecondaryButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());
        JButton save = makeAccentButton("  Save Enrollment  ");
        save.addActionListener(e -> {
            try {
                String selS = studentCombo.getSelectedItem().toString();
                String selB = batchCombo.getSelectedItem().toString();
                if (selS.startsWith("Select") || selB.startsWith("Select")) {
                    JOptionPane.showMessageDialog(dialog, "Please select a student and a batch.");
                    return;
                }
                String studentId = selS.split(" – ")[0].trim();
                int    batchId   = Integer.parseInt(selB.split(" – ")[0].trim());

                Enrollment en = new Enrollment();
                en.setEnrollmentId((int)(System.currentTimeMillis() % 100000));
                en.setStudentUserId(studentId);
                en.setBatchId(batchId);
                en.setStatus(statusCombo.getSelectedItem().toString());
                en.setEnrollmentDate(new Date());
                en.setRemarks("");

                if (new EnrollmentDAO().addEnrollment(en)) {
                    JOptionPane.showMessageDialog(dialog, "Enrollment saved successfully!");
                    refreshTable();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Failed to save enrollment.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error: " + ex.getMessage());
            }
        });
        btnRow.add(cancel);
        btnRow.add(save);

        dialog.add(titleBar, BorderLayout.NORTH);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private JPanel makeFormRow(String label, JComponent comp) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setBackground(CARD_BG);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(TEXT_SEC);
        if (comp instanceof JComboBox) {
            comp.setPreferredSize(new Dimension(comp.getPreferredSize().width, 38));
        }
        row.add(lbl, BorderLayout.NORTH);
        row.add(comp, BorderLayout.CENTER);
        return row;
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(44);
        t.setShowGrid(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(235, 240, 248));
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(CARD_BG);
        t.setSelectionBackground(new Color(74, 144, 226, 30));
        t.setSelectionForeground(TEXT_PRI);
        t.setFocusable(false);

        // Header
        t.getTableHeader().setBackground(new Color(248, 250, 253));
        t.getTableHeader().setForeground(TEXT_SEC);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0, new Color(230,235,245)));

        // Default cell renderer
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                c.setBackground(isSelected ? new Color(74,144,226,30) : CARD_BG);
                c.setForeground(TEXT_PRI);
                setBorder(new EmptyBorder(0, 16, 0, 0));
                return c;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
        // status column gets its own renderer
    }

    private JButton makeAccentButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT_DARK : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(160, 38));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        return btn;
    }

    private JButton makeSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(CARD_BG);
        btn.setForeground(ACCENT);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT, 1, true),
            new EmptyBorder(6, 18, 6, 18)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Status Pill Renderer ───────────────────────────────────────────────────
    private static class StatusPillRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            String status = value != null ? value.toString() : "Active";
            boolean active = "Active".equalsIgnoreCase(status);
            JLabel lbl = new JLabel(status, SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(active ? new Color(34,197,94,30) : new Color(239,68,68,25));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(active ? new Color(22,163,74) : new Color(220,38,38));
            lbl.setOpaque(false);
            lbl.setBorder(new EmptyBorder(4, 12, 4, 12));
            JPanel wrap = new JPanel(new GridBagLayout());
            wrap.setBackground(isSelected ? new Color(74,144,226,30) : Color.WHITE);
            wrap.add(lbl);
            return wrap;
        }
    }
}
