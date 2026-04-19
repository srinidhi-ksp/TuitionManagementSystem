package ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import dao.AttendanceDAO;
import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.StudentDAO;
import model.Attendance;
import model.Batch;
import model.Enrollment;
import model.Student;

public class AttendanceManagementFrame extends JPanel {

    // ── Design tokens ──────────────────────────────────────────────────────────
    private static final Color NAV_BG      = new Color(10, 27, 63);
    private static final Color ACCENT      = new Color(74, 144, 226);
    private static final Color ACCENT_DARK = new Color(0, 102, 204);
    private static final Color PAGE_BG     = new Color(244, 247, 249);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color TEXT_PRI    = new Color(26, 35, 64);
    private static final Color TEXT_SEC    = new Color(107, 122, 153);

    private JComboBox<String> batchCombo;
    private JTextField dateField;
    private JPanel studentListPanel;
    private JPanel controlBar;

    // Maps student userId -> selected status ("Present" / "Absent" / "Late")
    private final Map<String, String[]>      studentInfo   = new HashMap<>(); // userId -> [name]
    private final Map<String, ButtonGroup>   statusGroups  = new HashMap<>();

    private List<Batch> batches = new ArrayList<>();

    public AttendanceManagementFrame() {
        setLayout(new BorderLayout());
        setBackground(PAGE_BG);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    // ── Header ─────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PAGE_BG);
        header.setBorder(new EmptyBorder(28, 36, 12, 36));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 4));
        titles.setBackground(PAGE_BG);
        JLabel titleLbl = new JLabel("Attendance Management");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLbl.setForeground(TEXT_PRI);
        JLabel subLbl = new JLabel("Mark and track student attendance by batch and date");
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subLbl.setForeground(TEXT_SEC);
        titles.add(titleLbl);
        titles.add(subLbl);
        header.add(titles, BorderLayout.WEST);
        return header;
    }

    // ── Body ───────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(PAGE_BG);
        body.setBorder(new EmptyBorder(0, 36, 36, 36));

        body.add(buildSelectorCard(), BorderLayout.NORTH);
        body.add(buildStudentListCard(), BorderLayout.CENTER);
        return body;
    }

    // ── Selector card (batch + date + load) ───────────────────────────────────
    private JPanel buildSelectorCard() {
        JPanel card = new JPanel();
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 16));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true),
            new EmptyBorder(4, 4, 4, 4)
        ));

        // Batch dropdown
        batchCombo = new JComboBox<>();
        batchCombo.addItem("Select Batch");
        batches = new BatchDAO().getAllBatches();
        for (Batch b : batches) {
            batchCombo.addItem(b.getBatchId() + " – " + b.getBatchName());
        }
        batchCombo.setPreferredSize(new Dimension(240, 38));
        batchCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));

        // Date field
        dateField = new JTextField("YYYY-MM-DD");
        dateField.setPreferredSize(new Dimension(150, 38));
        dateField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        dateField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
            new EmptyBorder(6, 12, 6, 12)
        ));

        JButton loadBtn = makeAccentButton("Load Students");
        loadBtn.addActionListener(e -> loadStudents());

        JLabel batchLbl = makeSelectorLabel("Select Batch:");
        JLabel dateLbl  = makeSelectorLabel("Select Date:");

        card.add(batchLbl);
        card.add(batchCombo);
        card.add(Box.createHorizontalStrut(12));
        card.add(dateLbl);
        card.add(dateField);
        card.add(Box.createHorizontalStrut(12));
        card.add(loadBtn);
        return card;
    }

    private JLabel makeSelectorLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(TEXT_SEC);
        return lbl;
    }

    // ── Student list card ──────────────────────────────────────────────────────
    private JPanel buildStudentListCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1, true));

        // Card header
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setBackground(CARD_BG);
        cardHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 245)),
            new EmptyBorder(14, 20, 14, 20)
        ));
        JLabel headerLbl = new JLabel("Student Attendance");
        headerLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        headerLbl.setForeground(TEXT_PRI);

        JButton saveBtn = makeAccentButton("✓  Save Attendance");
        saveBtn.addActionListener(e -> saveAttendance());

        cardHeader.add(headerLbl, BorderLayout.WEST);
        cardHeader.add(saveBtn, BorderLayout.EAST);

        // Column headers
        JPanel colHeaders = new JPanel(new GridLayout(1, 4));
        colHeaders.setBackground(new Color(248, 250, 253));
        colHeaders.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 235, 245)),
            new EmptyBorder(10, 20, 10, 20)
        ));
        for (String h : new String[]{"#", "Student Name", "Status", ""}) {
            JLabel lbl = new JLabel(h.equals("Status") ? "  Present       Absent       Late" : h);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            lbl.setForeground(TEXT_SEC);
            colHeaders.add(lbl);
        }

        // Student rows (scroll)
        studentListPanel = new JPanel();
        studentListPanel.setLayout(new BoxLayout(studentListPanel, BoxLayout.Y_AXIS));
        studentListPanel.setBackground(CARD_BG);

        JLabel placeholder = new JLabel("Select a batch and date, then click Load Students.", SwingConstants.CENTER);
        placeholder.setFont(new Font("SansSerif", Font.PLAIN, 13));
        placeholder.setForeground(TEXT_SEC);
        placeholder.setBorder(new EmptyBorder(40, 0, 40, 0));
        studentListPanel.add(placeholder);

        JScrollPane scroll = new JScrollPane(studentListPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CARD_BG);

        card.add(cardHeader,   BorderLayout.NORTH);
        card.add(colHeaders,   BorderLayout.CENTER);
        card.add(scroll,       BorderLayout.SOUTH);

        // Rearrange: header + colHeaders + scroll in a vertical stack
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

    // ── Load students for selected batch ──────────────────────────────────────
    private void loadStudents() {
        studentInfo.clear();
        statusGroups.clear();
        studentListPanel.removeAll();

        String selBatch = batchCombo.getSelectedItem() != null ? batchCombo.getSelectedItem().toString() : "";
        String dateStr  = dateField.getText().trim();

        if (selBatch.startsWith("Select") || dateStr.isEmpty() || dateStr.equals("YYYY-MM-DD")) {
            JLabel warn = new JLabel("⚠ Please select a batch and enter a date (YYYY-MM-DD).", SwingConstants.CENTER);
            warn.setForeground(new Color(220, 120, 40));
            warn.setFont(new Font("SansSerif", Font.PLAIN, 13));
            warn.setBorder(new EmptyBorder(30, 0, 30, 0));
            studentListPanel.add(warn);
            studentListPanel.revalidate();
            studentListPanel.repaint();
            return;
        }

        int batchId;
        try { batchId = Integer.parseInt(selBatch.split(" – ")[0].trim()); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid batch selection."); return; }

        // Fetch enrolled students
        List<Enrollment> enrollments = new EnrollmentDAO().getAllEnrollments();
        StudentDAO sDao = new StudentDAO();
        List<Student> enrolled = new ArrayList<>();
        for (Enrollment en : enrollments) {
            if (en.getBatchId() == batchId) {
                Student s = sDao.getStudentById(en.getStudentUserId());
                if (s != null) enrolled.add(s);
            }
        }

        // Fetch existing attendance for this batch+date
        List<Attendance> existingAttendance = new AttendanceDAO().getAttendanceByBatchAndDate(batchId, dateStr);
        Map<String, String> existingStatus = new HashMap<>();
        for (Attendance a : existingAttendance) {
            if (a.getUserId() != null && a.getStatus() != null) {
                existingStatus.put(a.getUserId(), a.getStatus());
            }
        }

        if (enrolled.isEmpty()) {
            JLabel noStudents = new JLabel("No students enrolled in this batch.", SwingConstants.CENTER);
            noStudents.setForeground(TEXT_SEC);
            noStudents.setFont(new Font("SansSerif", Font.PLAIN, 13));
            noStudents.setBorder(new EmptyBorder(30, 0, 30, 0));
            studentListPanel.add(noStudents);
        } else {
            int idx = 1;
            for (Student s : enrolled) {
                studentInfo.put(s.getUserId(), new String[]{s.getName() != null ? s.getName() : s.getUserId()});
                JPanel row = buildStudentRow(idx++, s, existingStatus.getOrDefault(s.getUserId(), "Present"));
                studentListPanel.add(row);
                studentListPanel.add(Box.createRigidArea(new Dimension(0, 1)));
            }
        }

        studentListPanel.revalidate();
        studentListPanel.repaint();
    }

    private JPanel buildStudentRow(int idx, Student s, String defaultStatus) {
        JPanel row = new JPanel(new GridLayout(1, 4));
        row.setBackground(CARD_BG);
        row.setBorder(new EmptyBorder(12, 20, 12, 20));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel numLbl  = new JLabel(String.valueOf(idx));
        numLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        numLbl.setForeground(TEXT_SEC);

        JLabel nameLbl = new JLabel(s.getName() != null ? s.getName() : s.getUserId());
        nameLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        nameLbl.setForeground(TEXT_PRI);

        // Radio buttons
        JRadioButton present = styledRadio("Present", new Color(34, 197, 94));
        JRadioButton absent  = styledRadio("Absent",  new Color(239, 68, 68));
        JRadioButton late    = styledRadio("Late",    new Color(245, 158, 11));

        ButtonGroup bg = new ButtonGroup();
        bg.add(present); bg.add(absent); bg.add(late);
        statusGroups.put(s.getUserId(), bg);

        // Set default
        if ("Absent".equalsIgnoreCase(defaultStatus))  absent.setSelected(true);
        else if ("Late".equalsIgnoreCase(defaultStatus)) late.setSelected(true);
        else present.setSelected(true);

        present.setActionCommand("Present");
        absent.setActionCommand("Absent");
        late.setActionCommand("Late");

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        radioPanel.setBackground(CARD_BG);
        radioPanel.add(present);
        radioPanel.add(absent);
        radioPanel.add(late);

        row.add(numLbl);
        row.add(nameLbl);
        row.add(radioPanel);
        row.add(new JLabel(""));

        // Hover effect
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { row.setBackground(new Color(248,250,253)); radioPanel.setBackground(new Color(248,250,253)); }
            public void mouseExited(java.awt.event.MouseEvent e)  { row.setBackground(CARD_BG); radioPanel.setBackground(CARD_BG); }
        });

        return row;
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

    // ── Save attendance ────────────────────────────────────────────────────────
    private void saveAttendance() {
        String selBatch = batchCombo.getSelectedItem() != null ? batchCombo.getSelectedItem().toString() : "";
        String dateStr  = dateField.getText().trim();

        if (selBatch.startsWith("Select") || dateStr.equals("YYYY-MM-DD")) {
            JOptionPane.showMessageDialog(this, "No batch/date selected. Please load students first.");
            return;
        }

        int batchId;
        try { batchId = Integer.parseInt(selBatch.split(" – ")[0].trim()); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid batch."); return; }

        if (statusGroups.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students loaded.");
            return;
        }

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
            att.setAttendanceDate(new java.util.Date());

            if (dao.saveOrUpdateAttendance(att, batchId, dateStr)) saved++;
        }

        JOptionPane.showMessageDialog(this,
            "Attendance saved for " + saved + " student(s) on " + dateStr + ".");
    }

    // ── Accent button ──────────────────────────────────────────────────────────
    private JButton makeAccentButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
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
        btn.setPreferredSize(new Dimension(180, 38));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        return btn;
    }
}
