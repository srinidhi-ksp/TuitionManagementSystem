package ui.admin;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import dao.AttendanceDAO;
import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.StudentDAO;
import dao.TeacherDAO;
import model.Attendance;
import model.Batch;
import model.Enrollment;
import model.Student;
import model.Teacher;

/**
 * AttendanceManagementFrame
 * - JTabbedPane with "Student Attendance" and "Teacher Attendance" tabs.
 * - JSpinner date picker (no external JARs needed).
 * - Student tab: batch + date → load → radio buttons → save (upsert).
 * - Teacher tab: date → load all teachers → JTable with JComboBox status editor → save.
 */
public class AttendanceManagementFrame extends JPanel {

    // ── Design tokens ──────────────────────────────────────────────────────────
    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color ACCENT_DARK = new Color(0, 102, 204);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);
    private static final SimpleDateFormat DATE_KEY = new SimpleDateFormat("yyyy-MM-dd");

    // ── Student tab state ──────────────────────────────────────────────────────
    private JComboBox<String> batchCombo;
    private DateChooser       studentDateChooser;
    private JPanel            studentListPanel;
    private final Map<String, String[]>    studentInfo  = new HashMap<>();
    private final Map<String, ButtonGroup> statusGroups = new HashMap<>();
    private List<Batch>  batches = new ArrayList<>();

    // ── Teacher tab state ──────────────────────────────────────────────────────
    private DateChooser       teacherDateChooser;
    private DefaultTableModel teacherTableModel;
    private List<Teacher>     allTeachers = new ArrayList<>();
    private static final String[] STATUS_OPTIONS = {"Present", "Absent", "Late"};

    public AttendanceManagementFrame() {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabbedBody(), BorderLayout.CENTER);
    }

    // ── Page header ────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PAGE_BG);
        header.setBorder(new EmptyBorder(28, 36, 12, 36));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(PAGE_BG);
        JLabel titleLbl = new JLabel("Attendance Management");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLbl.setForeground(TEXT_PRI);
        JLabel subLbl = new JLabel("Track and save student & teacher attendance");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subLbl.setForeground(TEXT_SEC);
        titles.add(titleLbl); titles.add(subLbl);
        header.add(titles, BorderLayout.WEST);
        return header;
    }

    // ── JTabbedPane ────────────────────────────────────────────────────────────
    private JTabbedPane buildTabbedBody() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.setBackground(PAGE_BG);
        tabs.setBorder(new EmptyBorder(0, 36, 36, 36));

        tabs.addTab("🎓  Student Attendance", buildStudentTab());
        tabs.addTab("👨‍🏫  Teacher Attendance", buildTeacherTab());

        // Style tab area
        tabs.setOpaque(false);
        return tabs;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STUDENT ATTENDANCE TAB
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildStudentTab() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(PAGE_BG);
        body.setBorder(new EmptyBorder(16, 0, 0, 0));
        body.add(buildStudentSelectorCard(), BorderLayout.NORTH);
        body.add(buildStudentListCard(), BorderLayout.CENTER);
        return body;
    }

    // ── Selector card (batch + date + load) ───────────────────────────────────
    private JPanel buildStudentSelectorCard() {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true),
            new EmptyBorder(4, 4, 4, 4)));

        batchCombo = new JComboBox<>();
        batchCombo.addItem("Select Batch");
        batches = new BatchDAO().getAllBatches();
        for (Batch b : batches) batchCombo.addItem(b.getBatchId() + " – " + b.getBatchName());
        batchCombo.setPreferredSize(new Dimension(240, 38));
        batchCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));

        studentDateChooser = new DateChooser();

        JButton loadBtn = makeAccentButton("Load Students");
        loadBtn.addActionListener(e -> loadStudents());

        card.add(selectorLabel("Select Batch:"));
        card.add(batchCombo);
        card.add(Box.createHorizontalStrut(12));
        card.add(selectorLabel("Select Date:"));
        card.add(studentDateChooser);
        card.add(Box.createHorizontalStrut(12));
        card.add(loadBtn);
        return card;
    }

    // ── Student list card ──────────────────────────────────────────────────────
    private JPanel buildStudentListCard() {
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setBackground(CARD_BG);
        cardHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,new Color(230,235,245)),
            new EmptyBorder(14,20,14,20)));
        JLabel headerLbl = new JLabel("Student Attendance List");
        headerLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        headerLbl.setForeground(TEXT_PRI);
        JButton saveBtn = makeAccentButton("✓  Save Attendance");
        saveBtn.addActionListener(e -> saveStudentAttendance());
        cardHeader.add(headerLbl, BorderLayout.WEST);
        cardHeader.add(saveBtn, BorderLayout.EAST);

        // Column headers strip
        JPanel colHeaders = new JPanel(new GridLayout(1, 4));
        colHeaders.setBackground(new Color(248, 250, 253));
        colHeaders.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,new Color(230,235,245)),
            new EmptyBorder(10,20,10,20)));
        for (String h : new String[]{"#", "Student Name", "Status", ""}) {
            JLabel lbl = new JLabel(h.equals("Status") ? "  Present       Absent       Late" : h);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(TEXT_SEC);
            colHeaders.add(lbl);
        }

        studentListPanel = new JPanel();
        studentListPanel.setLayout(new BoxLayout(studentListPanel, BoxLayout.Y_AXIS));
        studentListPanel.setBackground(CARD_BG);
        studentListPanel.add(placeholderLabel("Select a batch and date, then click Load Students."));

        JScrollPane scroll = new JScrollPane(studentListPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        JPanel innerStack = new JPanel(new BorderLayout());
        innerStack.setBackground(CARD_BG);
        innerStack.add(colHeaders, BorderLayout.NORTH);
        innerStack.add(scroll, BorderLayout.CENTER);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(CARD_BG);
        outer.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true));
        outer.add(cardHeader, BorderLayout.NORTH);
        outer.add(innerStack, BorderLayout.CENTER);
        return outer;
    }

    // ── Load students for selected batch+date ──────────────────────────────────
    private void loadStudents() {
        studentInfo.clear();
        statusGroups.clear();
        studentListPanel.removeAll();

        String selBatch = batchCombo.getSelectedItem() != null ? batchCombo.getSelectedItem().toString() : "";
        if (selBatch.startsWith("Select")) {
            studentListPanel.add(warnLabel("⚠ Please select a batch first."));
            studentListPanel.revalidate(); studentListPanel.repaint(); return;
        }

        int batchId;
        try { batchId = Integer.parseInt(selBatch.split(" – ")[0].trim()); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid batch selection."); return; }

        String dateStr = DATE_KEY.format(studentDateChooser.getDate());

        List<Enrollment> enrollments = new EnrollmentDAO().getAllEnrollments();
        StudentDAO sDao = new StudentDAO();
        List<Student> enrolled = new ArrayList<>();
        for (Enrollment en : enrollments)
            if (en.getBatchId() == batchId) {
                Student s = sDao.getStudentById(en.getStudentUserId());
                if (s != null) enrolled.add(s);
            }

        // Pre-load existing attendance for this batch+date
        List<Attendance> existing = new AttendanceDAO().getAttendanceByBatchAndDate(batchId, dateStr);
        Map<String,String> existingStatus = new HashMap<>();
        for (Attendance a : existing)
            if (a.getUserId() != null && a.getStatus() != null)
                existingStatus.put(a.getUserId(), a.getStatus());

        if (enrolled.isEmpty()) {
            studentListPanel.add(placeholderLabel("No students enrolled in this batch."));
        } else {
            int idx = 1;
            for (Student s : enrolled) {
                studentInfo.put(s.getUserId(), new String[]{s.getName() != null ? s.getName() : s.getUserId()});
                studentListPanel.add(buildStudentRow(idx++, s, existingStatus.getOrDefault(s.getUserId(), "Present")));
                studentListPanel.add(Box.createRigidArea(new Dimension(0, 1)));
            }
        }
        studentListPanel.revalidate(); studentListPanel.repaint();
    }

    private JPanel buildStudentRow(int idx, Student s, String defaultStatus) {
        JPanel row = new JPanel(new GridLayout(1, 4));
        row.setBackground(CARD_BG);
        row.setBorder(new EmptyBorder(12, 20, 12, 20));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel numLbl  = new JLabel(String.valueOf(idx));
        numLbl.setFont(new Font("SansSerif", Font.PLAIN, 13)); numLbl.setForeground(TEXT_SEC);
        JLabel nameLbl = new JLabel(s.getName() != null ? s.getName() : s.getUserId());
        nameLbl.setFont(new Font("SansSerif", Font.PLAIN, 13)); nameLbl.setForeground(TEXT_PRI);

        JRadioButton present = styledRadio("Present", new Color(34, 197, 94));
        JRadioButton absent  = styledRadio("Absent",  new Color(239, 68, 68));
        JRadioButton late    = styledRadio("Late",    new Color(245, 158, 11));
        present.setActionCommand("Present"); absent.setActionCommand("Absent"); late.setActionCommand("Late");
        ButtonGroup bg = new ButtonGroup();
        bg.add(present); bg.add(absent); bg.add(late);
        statusGroups.put(s.getUserId(), bg);
        if ("Absent".equalsIgnoreCase(defaultStatus))    absent.setSelected(true);
        else if ("Late".equalsIgnoreCase(defaultStatus)) late.setSelected(true);
        else                                             present.setSelected(true);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        radioPanel.setBackground(CARD_BG);
        radioPanel.add(present); radioPanel.add(absent); radioPanel.add(late);

        row.add(numLbl); row.add(nameLbl); row.add(radioPanel); row.add(new JLabel(""));
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                row.setBackground(new Color(248,250,253)); radioPanel.setBackground(new Color(248,250,253));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                row.setBackground(CARD_BG); radioPanel.setBackground(CARD_BG);
            }
        });
        return row;
    }

    private void saveStudentAttendance() {
        String selBatch = batchCombo.getSelectedItem() != null ? batchCombo.getSelectedItem().toString() : "";
        if (selBatch.startsWith("Select")) {
            JOptionPane.showMessageDialog(this, "No batch selected. Please load students first."); return;
        }
        int batchId;
        try { batchId = Integer.parseInt(selBatch.split(" – ")[0].trim()); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid batch."); return; }
        if (statusGroups.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students loaded."); return;
        }

        String dateStr = DATE_KEY.format(studentDateChooser.getDate());
        AttendanceDAO dao = new AttendanceDAO();
        int saved = 0;
        for (Map.Entry<String, ButtonGroup> entry : statusGroups.entrySet()) {
            String userId = entry.getKey();
            String status = entry.getValue().getSelection() != null
                ? entry.getValue().getSelection().getActionCommand() : "Present";
            Attendance att = new Attendance();
            att.setAttendanceId((int)(System.currentTimeMillis() % 100000));
            att.setUserId(userId);
            att.setStatus(status);
            att.setMarkedBy("ADMIN");
            att.setAttendanceDate(new Date());
            if (dao.saveOrUpdateAttendance(att, batchId, dateStr)) saved++;
        }
        JOptionPane.showMessageDialog(this, "✓ Attendance saved for " + saved + " student(s) on " + dateStr + ".");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TEACHER ATTENDANCE TAB
    // ══════════════════════════════════════════════════════════════════════════
    private JPanel buildTeacherTab() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(PAGE_BG);
        body.setBorder(new EmptyBorder(16, 0, 0, 0));
        body.add(buildTeacherSelectorCard(), BorderLayout.NORTH);
        body.add(buildTeacherTableCard(), BorderLayout.CENTER);
        return body;
    }

    private JPanel buildTeacherSelectorCard() {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true),
            new EmptyBorder(4, 4, 4, 4)));

        teacherDateChooser = new DateChooser();

        JButton loadBtn = makeAccentButton("Load Teachers");
        loadBtn.addActionListener(e -> loadTeachers());

        card.add(selectorLabel("Select Date:"));
        card.add(teacherDateChooser);
        card.add(Box.createHorizontalStrut(12));
        card.add(loadBtn);
        return card;
    }

    private JPanel buildTeacherTableCard() {
        // Card header with save button
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setBackground(CARD_BG);
        cardHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0,new Color(230,235,245)),
            new EmptyBorder(14,20,14,20)));
        JLabel headerLbl = new JLabel("Teacher Attendance List");
        headerLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        headerLbl.setForeground(TEXT_PRI);
        JButton saveBtn = makeAccentButton("✓  Save Attendance");
        saveBtn.addActionListener(e -> saveTeacherAttendance());
        cardHeader.add(headerLbl, BorderLayout.WEST);
        cardHeader.add(saveBtn, BorderLayout.EAST);

        // Table
        String[] cols = {"#", "Teacher Name", "Specialization", "Status"};
        teacherTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3; }
            @Override public Class<?> getColumnClass(int c) { return c == 3 ? String.class : Object.class; }
        };
        JTable teacherTable = new JTable(teacherTableModel);
        teacherTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        teacherTable.setRowHeight(42);
        teacherTable.setShowGrid(false); teacherTable.setShowHorizontalLines(true);
        teacherTable.setGridColor(new Color(235,240,248));
        teacherTable.setIntercellSpacing(new Dimension(0,0));
        teacherTable.setBackground(CARD_BG);
        teacherTable.setSelectionBackground(new Color(74,144,226,25));
        teacherTable.setFocusable(false);
        teacherTable.getTableHeader().setBackground(new Color(248,250,253));
        teacherTable.getTableHeader().setForeground(TEXT_SEC);
        teacherTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        teacherTable.getTableHeader().setBorder(
            BorderFactory.createMatteBorder(0,0,1,0,new Color(230,235,245)));

        // Cell renderer for regular columns
        DefaultTableCellRenderer rend = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t,Object v,
                    boolean sel,boolean foc,int r,int c) {
                Component comp = super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                comp.setBackground(sel ? new Color(74,144,226,20) : CARD_BG);
                comp.setForeground(TEXT_PRI);
                setBorder(new EmptyBorder(0,16,0,0)); return comp;
            }
        };
        for (int i = 0; i < 3; i++) teacherTable.getColumnModel().getColumn(i).setCellRenderer(rend);
        teacherTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        teacherTable.getColumnModel().getColumn(3).setPreferredWidth(160);

        // JComboBox status editor for column 3
        JComboBox<String> statusCombo = new JComboBox<>(STATUS_OPTIONS);
        statusCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        teacherTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(statusCombo));

        // Colour-coded renderer for the status column
        teacherTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t,Object v,
                    boolean sel,boolean foc,int r,int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t,v,sel,foc,r,c);
                String val = v != null ? v.toString() : "Present";
                if ("Absent".equals(val))      lbl.setForeground(new Color(239, 68, 68));
                else if ("Late".equals(val))   lbl.setForeground(new Color(245, 158, 11));
                else                           lbl.setForeground(new Color(34, 197, 94));
                lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
                lbl.setBorder(new EmptyBorder(0,16,0,0));
                lbl.setBackground(sel ? new Color(74,144,226,20) : CARD_BG);
                return lbl;
            }
        });

        JScrollPane scroll = new JScrollPane(teacherTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(CARD_BG);
        outer.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true));
        outer.add(cardHeader, BorderLayout.NORTH);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    private void loadTeachers() {
        teacherTableModel.setRowCount(0);
        allTeachers = new TeacherDAO().getAllTeachers();

        String dateStr = DATE_KEY.format(teacherDateChooser.getDate());
        AttendanceDAO dao = new AttendanceDAO();

        if (allTeachers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No teachers found in the system.");
            return;
        }

        int idx = 1;
        for (Teacher t : allTeachers) {
            String name = t.getName() != null ? t.getName() + " (#" + t.getUserId() + ")" : t.getUserId();
            String spec = t.getSpecialization() != null ? t.getSpecialization() : "—";
            // Load existing status for this teacher on this date (sentinel batchId = -1)
            String status = dao.getTeacherAttendanceStatus(t.getUserId(), dateStr);
            teacherTableModel.addRow(new Object[]{idx++, name, spec, status});
        }
    }

    private void saveTeacherAttendance() {
        if (teacherTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No teachers loaded. Click 'Load Teachers' first."); return;
        }
        String dateStr = DATE_KEY.format(teacherDateChooser.getDate());
        AttendanceDAO dao = new AttendanceDAO();
        int saved = 0;
        for (int row = 0; row < teacherTableModel.getRowCount(); row++) {
            if (row >= allTeachers.size()) break;
            Teacher t = allTeachers.get(row);
            Object statusObj = teacherTableModel.getValueAt(row, 3);
            String status = statusObj != null ? statusObj.toString() : "Present";
            if (dao.saveTeacherAttendance(t.getUserId(), status, dateStr)) saved++;
        }
        JOptionPane.showMessageDialog(this, "✓ Teacher attendance saved for " + saved + " teacher(s) on " + dateStr + ".");
    }

    // ── Shared widget helpers ──────────────────────────────────────────────────
    private JSpinner buildDateSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        spinner.setEditor(editor);
        spinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
        spinner.setPreferredSize(new Dimension(160, 38));
        spinner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
            new EmptyBorder(2, 8, 2, 8)));
        return spinner;
    }

    private JLabel selectorLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(TEXT_SEC);
        return lbl;
    }

    private JLabel placeholderLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(TEXT_SEC);
        lbl.setBorder(new EmptyBorder(40, 0, 40, 0));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private JLabel warnLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(new Color(220, 120, 40));
        lbl.setBorder(new EmptyBorder(30, 0, 30, 0));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private JRadioButton styledRadio(String text, Color color) {
        JRadioButton rb = new JRadioButton(text);
        rb.setFont(new Font("SansSerif", Font.BOLD, 12));
        rb.setForeground(color);
        rb.setBackground(CARD_BG);
        rb.setFocusPainted(false);
        rb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return rb;
    }

    private JButton makeAccentButton(String text) {
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
        btn.setPreferredSize(new Dimension(190, 38)); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        return btn;
    }
}
