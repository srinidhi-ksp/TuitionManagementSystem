package ui.admin;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import dao.TeacherDAO;
import dao.TestsDAO;
import dao.TimeslotDAO;
import dao.SubjectDAO;
import model.Batch;
import model.ScheduleEntry;
import model.Schedule;
import model.Teacher;
import model.Test;
import model.Timeslot;

/**
 * Non-modal dialog showing details of all batches in a given (day, timeslot) cell.
 * See spec §3.
 */
public class BatchDetailsDialog extends JDialog {

    private static final Color NAV_BG   = new Color(10, 27, 63);
    private static final Color CARD_BG  = Color.WHITE;
    private static final Color CARD_BORDER = new Color(214, 228, 255);
    private static final Color CARD_BG2 = new Color(248, 250, 255);
    private static final Color TEXT_PRI = new Color(26, 35, 64);
    private static final Color TEXT_SEC = new Color(107, 122, 153);
    private static final Color ACCENT   = new Color(59, 130, 246);

    public BatchDetailsDialog(Frame parent, String day, Timeslot timeslot, List<Batch> batches) {
        super(parent, "Batch Details — " + day + " · " + (timeslot != null ? timeslot.getLabel() : ""), false);

        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(CARD_BG);

        // ── Title bar ────────────────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(NAV_BG);
        titleBar.setBorder(new EmptyBorder(14, 20, 14, 20));

        JLabel titleLbl = new JLabel("ℹ  Batch Details — " + day + " · "
                + (timeslot != null ? timeslot.getLabel() : ""));
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setForeground(Color.WHITE);
        titleBar.add(titleLbl, BorderLayout.WEST);
        add(titleBar, BorderLayout.NORTH);

        // ── Sub-header: count ─────────────────────────────────────────────────
        JLabel countLbl = new JLabel("  Showing " + batches.size() + " batch(es)");
        countLbl.setFont(new Font("SansSerif", Font.ITALIC, 12));
        countLbl.setForeground(TEXT_SEC);
        countLbl.setBorder(new EmptyBorder(10, 20, 6, 20));

        // ── Batch cards in a scroll pane ─────────────────────────────────────
        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(CARD_BG);
        cardsPanel.setBorder(new EmptyBorder(10, 16, 10, 16));

        TeacherDAO teacherDAO = new TeacherDAO();
        TestsDAO   testsDAO   = new TestsDAO();
        TimeslotDAO tsDAO     = new TimeslotDAO();
        SubjectDAO subjectDAO = new SubjectDAO();

        for (Batch b : batches) {
            cardsPanel.add(buildBatchCard(b, teacherDAO, testsDAO, tsDAO, subjectDAO));
            cardsPanel.add(Box.createVerticalStrut(12));
        }

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(CARD_BG);

        // ── OK button ─────────────────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 12));
        btnPanel.setBackground(new Color(248, 250, 253));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 235, 245)));
        JButton okBtn = new JButton("  OK  ");
        okBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        okBtn.setBackground(ACCENT);
        okBtn.setForeground(Color.WHITE);
        okBtn.setFocusPainted(false);
        okBtn.setBorderPainted(false);
        okBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        okBtn.addActionListener(e -> dispose());
        btnPanel.add(okBtn);

        // ── Centre panel ─────────────────────────────────────────────────────
        JPanel centre = new JPanel(new BorderLayout());
        centre.setBackground(CARD_BG);
        centre.add(countLbl, BorderLayout.NORTH);
        centre.add(scroll, BorderLayout.CENTER);

        add(centre, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        // Size: 480 wide, capped at 600 tall — §3.3
        int height = Math.min(600, 150 + 145 * batches.size());
        setPreferredSize(new Dimension(520, height));
        pack();
        setLocationRelativeTo(parent);
    }

    // ── Card builder ─────────────────────────────────────────────────────────

    private JPanel buildBatchCard(Batch b, TeacherDAO teacherDAO,
                                  TestsDAO testsDAO, TimeslotDAO tsDAO, SubjectDAO subjectDAO) {
        JPanel card = new JPanel(new GridLayout(0, 1, 0, 5));
        card.setBackground(CARD_BG2);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BORDER, 1, true),
            new EmptyBorder(14, 16, 14, 16)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Resolve teacher name
        Teacher teacher = teacherDAO.getTeacherById(b.getTeacherId());
        String tName = teacher != null ? teacher.getName() : b.getTeacherId();
        model.Subject subject = subjectDAO.getSubjectById(b.getSubjectId());
        String subjectName = subject != null && subject.getSubjectName() != null
            ? subject.getSubjectName() : "Not Available";

        // Build full schedule string showing ALL days — §3.2
        String scheduleStr = buildFullScheduleString(b, tsDAO);

        // Next upcoming test
        String testInfo = "No upcoming tests";
        try {
            List<Test> tests = testsDAO.getTestsByBatchId(b.getBatchId());
            java.util.Date now = new java.util.Date();
            Test next = null;
            for (Test t : tests) {
                if (t.getTestDate() != null && t.getTestDate().after(now)) {
                    if (next == null || t.getTestDate().before(next.getTestDate())) next = t;
                }
            }
            if (next != null) {
                testInfo = next.getTestName() + " ("
                    + new java.text.SimpleDateFormat("dd-MM-yyyy hh:mm a").format(next.getTestDate()) + ")";
            }
        } catch (Exception ex) { /* ignore */ }

        card.add(row("Batch Name", b.getBatchName() != null ? b.getBatchName() : "—"));
        card.add(row("Teacher",    tName));
        card.add(row("Subject",    subjectName));
        card.add(row("Schedule",   scheduleStr));
        card.add(row("Mode",       b.getClassMode() != null ? b.getClassMode() : "—"));
        if ("Online".equalsIgnoreCase(b.getClassMode()) || "ONLINE".equalsIgnoreCase(b.getClassMode())) {
            card.add(row("Link", b.getMeetingLink() != null ? b.getMeetingLink() : "—"));
        } else {
            card.add(row("Link", "—"));
        }
        card.add(row("Next Test",  testInfo));

        return card;
    }

    /**
     * Build the schedule description showing ALL days this batch runs — not just the clicked day.
     * e.g. "TUE & THU  ·  06:00 – 07:30"
     */
    private String buildFullScheduleString(Batch b, TimeslotDAO tsDAO) {
        if (b.getScheduleEntries() != null && !b.getScheduleEntries().isEmpty()) {
            List<String> days = new ArrayList<>();
            String tsId = b.getScheduleEntries().get(0).getTimeslotId();
            for (ScheduleEntry e : b.getScheduleEntries()) days.add(e.getDay());
            Timeslot ts = tsDAO.findById(tsId);
            String tsLabel = ts != null ? ts.getLabel() : tsId;
            return String.join(" & ", days) + "  ·  " + tsLabel;
        }
        if (b.getSchedules() != null && !b.getSchedules().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < b.getSchedules().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(b.getSchedules().get(i).toString());
            }
            return sb.toString();
        }
        return b.getTiming() != null ? b.getTiming() : "—";
    }

    private JLabel row(String field, String value) {
        JLabel l = new JLabel("<html><b>" + escHtml(field) + ":</b>  "
                + escHtml(value != null ? value : "—") + "</html>");
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(TEXT_PRI);
        return l;
    }

    private static String escHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
