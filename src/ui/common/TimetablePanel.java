package ui.common;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import dao.BatchDAO;
import dao.TimeslotDAO;
import model.Batch;
import model.Schedule;
import model.ScheduleEntry;
import model.Timeslot;
import util.ThemeManager;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import db.DBConnection;
import db.DocumentMapper;

/**
 * Redesigned Timetable Panel — §2.
 * Rows: MON…SUN. Columns: fixed Timeslot documents ordered by start time.
 * Cells show batch count badges; clicking an occupied cell opens BatchDetailsDialog.
 */
public class TimetablePanel extends JPanel {

    private String viewType; // ADMIN, TEACHER, STUDENT
    private String filterId; // teacherId or studentId
    private BatchDAO batchDAO;
    private TimeslotDAO timeslotDAO;
    
    private JPanel gridContainer;
    private final String[] DAYS = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

    // Colours — §2.4
    private static final Color BG_OCCUPIED    = new Color(214, 232, 250);  // #D6E8FA
    private static final Color BG_HOVER       = new Color(191, 219, 254);
    private static final Color BG_EMPTY       = new Color(248, 250, 253);
    private static final Color TEXT_GREY      = new Color(180, 185, 195);

    public TimetablePanel(String viewType, String filterId) {
        this.viewType      = viewType;
        this.filterId      = filterId;
        this.batchDAO      = new BatchDAO();
        this.timeslotDAO   = new TimeslotDAO();

        setLayout(new BorderLayout(10, 10));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // ── Header row ────────────────────────────────────────────────────────
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(ThemeManager.BG);

        JLabel title = new JLabel(viewType.equals("ADMIN") ? "Admin Weekly Timetable" : viewType + " Weekly Timetable");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(ThemeManager.TEXT);

        JButton refreshBtn = new JButton("↻ Refresh");
        refreshBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        refreshBtn.setBackground(new Color(59, 130, 246));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> refreshTimetable());

        headerRow.add(title, BorderLayout.WEST);
        headerRow.add(refreshBtn, BorderLayout.EAST);
        add(headerRow, BorderLayout.NORTH);

        gridContainer = new JPanel();
        gridContainer.setBackground(ThemeManager.BG);
        
        JScrollPane scroll = new JScrollPane(gridContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(ThemeManager.BG);
        add(scroll, BorderLayout.CENTER);

        refreshTimetable();
    }

    public void refreshTimetable() {
        // 1. Load timeslots (fixed, ordered)
        List<Timeslot> timeslots = timeslotDAO.findAllOrderedByStart();

        // 2. Load batches
        List<Batch> allBatches = loadBatches();

        // 3. Build timetableMap: day → timeslotId → List<Batch>
        Map<String, Map<String, List<Batch>>> timetableMap = new HashMap<>();
        for (Batch b : allBatches) {
            // Process new scheduleEntries first
            if (b.getScheduleEntries() != null && !b.getScheduleEntries().isEmpty()) {
                for (ScheduleEntry entry : b.getScheduleEntries()) {
                    timetableMap
                        .computeIfAbsent(normalizeDay(entry.getDay()), k -> new HashMap<>())
                        .computeIfAbsent(entry.getTimeslotId(), k -> new ArrayList<>())
                        .add(b);
                }
            } else if (b.getSchedules() != null) {
                // Fallback: legacy schedule with timeslotId
                for (Schedule s : b.getSchedules()) {
                    if (s.getTimeslotId() != null) {
                        timetableMap
                            .computeIfAbsent(normalizeDay(s.getDay()), k -> new HashMap<>())
                            .computeIfAbsent(s.getTimeslotId(), k -> new ArrayList<>())
                            .add(b);
                    } else if (s.getDay() != null && s.getStart() != null) {
                        // Legacy mapping: find matching Timeslot ID by start/end time
                        String matchedTsId = null;
                        for (Timeslot ts : timeslots) {
                            String tsStart = String.format("%02d:%02d", ts.getStartHour(), ts.getStartMin());
                            String tsEnd = String.format("%02d:%02d", ts.getEndHour(), ts.getEndMin());
                            if (tsStart.equals(normalizeTime24(s.getStart()))
                                    || (tsStart.equals(normalizeTime24(s.getStart())) && tsEnd.equals(normalizeTime24(s.getEnd())))) {
                                matchedTsId = ts.getId();
                                break;
                            }
                        }
                        if (matchedTsId == null) {
                            // Fallback pseudo-slot key
                            matchedTsId = s.getStart() + "-" + (s.getEnd() != null ? s.getEnd() : "");
                        }
                        timetableMap
                            .computeIfAbsent(normalizeDay(s.getDay()), k -> new HashMap<>())
                            .computeIfAbsent(matchedTsId, k -> new ArrayList<>())
                            .add(b);
                    }
                }
            }
        }

        // 4. Build Grid
        gridContainer.removeAll();
        int cols = timeslots.size() + 1; // +1 for DAY column
        gridContainer.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(2, 2, 2, 2);

        // Header row: corner + timeslot labels
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0.08;
        gbc.weighty = 0;
        gbc.ipady = 10;
        gridContainer.add(makeHeaderCell("DAY / TIME"), gbc);

        for (int c = 0; c < timeslots.size(); c++) {
            gbc.gridx = c + 1;
            gbc.weightx = 0.12;
            gridContainer.add(makeHeaderCell(timeslots.get(c).getLabel()), gbc);
        }

        // Day rows
        for (int r = 0; r < DAYS.length; r++) {
            String day = DAYS[r];
            gbc.gridy = r + 1;
            gbc.ipady = 30;

            // Day label column
            gbc.gridx = 0;
            gbc.weightx = 0.08;
            gbc.weighty = 0.13;
            gridContainer.add(makeDayCell(day), gbc);

            // Slot cells
            for (int c = 0; c < timeslots.size(); c++) {
                Timeslot ts = timeslots.get(c);
                gbc.gridx = c + 1;
                gbc.weightx = 0.12;

                Map<String, List<Batch>> dayMap = timetableMap.getOrDefault(day, Collections.emptyMap());
                List<Batch> batchesAtSlot = dayMap.getOrDefault(ts.getId(), Collections.emptyList());

                gridContainer.add(makeSlotCell(day, ts, batchesAtSlot, timeslots, timetableMap), gbc);
            }
        }

        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private String normalizeDay(String day) {
        if (day == null) return "";
        String value = day.trim().toUpperCase();
        if (value.startsWith("MON")) return "MON";
        if (value.startsWith("TUE")) return "TUE";
        if (value.startsWith("WED")) return "WED";
        if (value.startsWith("THU")) return "THU";
        if (value.startsWith("FRI")) return "FRI";
        if (value.startsWith("SAT")) return "SAT";
        if (value.startsWith("SUN")) return "SUN";
        return value;
    }

    private String normalizeTime24(String time) {
        if (time == null) return "";
        String text = time.trim().toUpperCase().replace('.', ':');
        java.util.List<java.text.SimpleDateFormat> formats = java.util.Arrays.asList(
            new java.text.SimpleDateFormat("HH:mm:ss"),
            new java.text.SimpleDateFormat("HH:mm"),
            new java.text.SimpleDateFormat("hh:mm a")
        );
        for (java.text.SimpleDateFormat fmt : formats) {
            try {
                fmt.setLenient(false);
                return new java.text.SimpleDateFormat("HH:mm").format(fmt.parse(text));
            } catch (Exception ignored) {}
        }
        return text.length() >= 5 ? text.substring(0, 5) : text;
    }

    // ── Cell builders ────────────────────────────────────────────────────────

    private JLabel makeHeaderCell(String text) {
        JLabel l = new JLabel("<html><center>" + text + "</center></html>", SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(ThemeManager.SUB_TEXT);
        l.setOpaque(true);
        l.setBackground(ThemeManager.BG);
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.DIVIDER),
            new EmptyBorder(4, 6, 4, 6)
        ));
        return l;
    }

    private JLabel makeDayCell(String day) {
        JLabel l = new JLabel(day, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        l.setForeground(ThemeManager.TEXT);
        l.setOpaque(true);
        l.setBackground(new Color(241, 245, 249));
        l.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        return l;
    }

    private JPanel makeSlotCell(String day, Timeslot ts, List<Batch> batches,
                                List<Timeslot> allTimeslots,
                                Map<String, Map<String, List<Batch>>> timetableMap) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));

        if (batches.isEmpty()) {
            // Empty cell — §2.4
            cell.setBackground(BG_EMPTY);
            JLabel dash = new JLabel("–", SwingConstants.CENTER);
            dash.setFont(new Font("SansSerif", Font.PLAIN, 13));
            dash.setForeground(TEXT_GREY);
            cell.add(dash, BorderLayout.CENTER);
        } else {
            // Occupied cell — §2.4
            cell.setBackground(BG_OCCUPIED);
            cell.setCursor(new Cursor(Cursor.HAND_CURSOR));

            int count = batches.size();
            JLabel badge = new JLabel(String.valueOf(count), SwingConstants.CENTER);
            badge.setFont(new Font("SansSerif", Font.BOLD, 20));
            badge.setForeground(new Color(30, 64, 175)); // dark blue

            JLabel sub = new JLabel(count == 1 ? "batch" : "batches", SwingConstants.CENTER);
            sub.setFont(new Font("SansSerif", Font.PLAIN, 10));
            sub.setForeground(new Color(59, 130, 246));

            JPanel inner = new JPanel(new GridLayout(2, 1, 0, 0));
            inner.setOpaque(false);
            inner.add(badge);
            inner.add(sub);
            cell.add(inner, BorderLayout.CENTER);

            // Add hover effect
            cell.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { cell.setBackground(BG_HOVER); }
                public void mouseExited(java.awt.event.MouseEvent e) { cell.setBackground(BG_OCCUPIED); }
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    openBatchDetailsDialog(day, ts, batches);
                }
            });
        }

        return cell;
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    /**
     * Opens the batch details dialog for the clicked (day, timeslot) cell.
     * See spec §3.
     */
    private void openBatchDetailsDialog(String day, Timeslot ts, List<Batch> batches) {
        // Try to open via the dedicated dialog class
        try {
            Class<?> dialogClass = Class.forName("ui.admin.BatchDetailsDialog");
            java.lang.reflect.Constructor<?> ctor = dialogClass.getConstructor(
                Frame.class, String.class, Timeslot.class, java.util.List.class);
            Object dialog = ctor.newInstance(
                SwingUtilities.getWindowAncestor(this) instanceof Frame
                    ? (Frame) SwingUtilities.getWindowAncestor(this) : null,
                day, ts, batches);
            dialog.getClass().getMethod("setVisible", boolean.class).invoke(dialog, true);
            return;
        } catch (Exception ex) {
            // fall through to inline fallback
        }

        // Inline fallback (used in student/teacher views where admin dialog may not be accessible)
        showInlineBatchDetailsDialog(day, ts, batches);
    }

    private void showInlineBatchDetailsDialog(String day, Timeslot ts, List<Batch> batches) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
            "Batch Details — " + day + " · " + (ts != null ? ts.getLabel() : ""), false);
        dialog.setLayout(new BorderLayout(10, 10));

        // Title bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(10, 27, 63));
        titleBar.setBorder(new EmptyBorder(12, 20, 12, 20));
        JLabel titleLbl = new JLabel("ℹ  Batch Details — " + day + " · " + (ts != null ? ts.getLabel() : ""));
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLbl.setForeground(Color.WHITE);
        titleBar.add(titleLbl, BorderLayout.WEST);

        // Info row
        JLabel infoLbl = new JLabel("  Showing " + batches.size() + " batch(es)");
        infoLbl.setFont(new Font("SansSerif", Font.ITALIC, 12));
        infoLbl.setForeground(new Color(107, 114, 128));
        infoLbl.setBorder(new EmptyBorder(8, 20, 0, 20));

        // Batch cards
        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(Color.WHITE);
        cardsPanel.setBorder(new EmptyBorder(10, 16, 10, 16));

        dao.TeacherDAO tDAO = new dao.TeacherDAO();
        dao.TestsDAO testDAO = new dao.TestsDAO();

        for (Batch b : batches) {
            cardsPanel.add(buildBatchCard(b, tDAO, testDAO));
            cardsPanel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scroll = new JScrollPane(cardsPanel);
        scroll.setBorder(null);
        int height = Math.min(600, 150 + 145 * batches.size());
        dialog.setPreferredSize(new Dimension(520, height));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("  OK  ");
        ok.addActionListener(e -> dialog.dispose());
        btnPanel.add(ok);

        dialog.add(titleBar, BorderLayout.NORTH);
        dialog.add(infoLbl, BorderLayout.CENTER);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JPanel buildBatchCard(Batch b, dao.TeacherDAO tDAO, dao.TestsDAO testDAO) {
        JPanel card = new JPanel(new GridLayout(0, 1, 0, 4));
        card.setBackground(new Color(248, 250, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(214, 228, 255), 1, true),
            new EmptyBorder(12, 16, 12, 16)
        ));

        model.Teacher teacher = tDAO.getTeacherById(b.getTeacherId());
        String tName = teacher != null ? teacher.getName() : b.getTeacherId();
        model.Subject subject = new dao.SubjectDAO().getSubjectById(b.getSubjectId());
        String subjectName = subject != null && subject.getSubjectName() != null
            ? subject.getSubjectName() : "Not Available";

        // Full schedule summary
        String scheduleStr = buildScheduleString(b);

        // Next test
        String testInfo = "No upcoming tests";
        try {
            List<model.Test> tests = testDAO.getTestsByBatchId(b.getBatchId());
            Date now = new Date();
            model.Test next = null;
            for (model.Test t : tests) {
                if (t.getTestDate() != null && t.getTestDate().after(now)) {
                    if (next == null || t.getTestDate().before(next.getTestDate())) next = t;
                }
            }
            if (next != null) {
                testInfo = next.getTestName() + " (" +
                    new java.text.SimpleDateFormat("dd-MM-yyyy hh:mm a").format(next.getTestDate()) + ")";
            }
        } catch (Exception ex) { /* ignore */ }

        card.add(row("Batch Name", b.getBatchName()));
        card.add(row("Teacher", tName));
        card.add(row("Subject", subjectName));
        card.add(row("Schedule", scheduleStr));
        card.add(row("Mode", b.getClassMode() != null ? b.getClassMode() : "—"));
        if ("Online".equalsIgnoreCase(b.getClassMode()) || "ONLINE".equalsIgnoreCase(b.getClassMode())) {
            card.add(row("Link", b.getMeetingLink() != null ? b.getMeetingLink() : "—"));
        }
        card.add(row("Next Test", testInfo));

        return card;
    }

    private JLabel row(String field, String value) {
        JLabel l = new JLabel("<html><b>" + field + ":</b>  " + (value != null ? value : "—") + "</html>");
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(new Color(30, 41, 59));
        return l;
    }

    private String buildScheduleString(Batch b) {
        if (b.getScheduleEntries() != null && !b.getScheduleEntries().isEmpty()) {
            TimeslotDAO tsDAO = new TimeslotDAO();
            StringBuilder sb = new StringBuilder();
            // Group by timeslotId
            String tsId = b.getScheduleEntries().get(0).getTimeslotId();
            Timeslot ts = tsDAO.findById(tsId);
            String tsLabel = ts != null ? ts.getLabel() : tsId;
            List<String> days = new ArrayList<>();
            for (ScheduleEntry e : b.getScheduleEntries()) days.add(e.getDay());
            sb.append(String.join(" & ", days)).append("  ·  ").append(tsLabel);
            return sb.toString();
        }
        if (b.getSchedules() != null && !b.getSchedules().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < b.getSchedules().size(); i++) {
                if (i > 0) sb.append(", ");
                Schedule s = b.getSchedules().get(i);
                sb.append(s.toString());
            }
            return sb.toString();
        }
        return b.getTiming() != null ? b.getTiming() : "—";
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private List<Batch> loadBatches() {
        List<Batch> displayBatches = new ArrayList<>();
        MongoDatabase database = DBConnection.getDatabase();

        try {
            if ("STUDENT".equals(viewType)) {
                displayBatches = loadStudentBatches(database);
            } else if ("TEACHER".equals(viewType)) {
                displayBatches = batchDAO.getBatchesByTeacher(filterId);
            } else {
                displayBatches = batchDAO.findAllActive();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return displayBatches;
    }

    private List<Batch> loadStudentBatches(MongoDatabase database) {
        List<Batch> result = new ArrayList<>();
        if (database == null) return result;

        // Resolve user ID → student ID
        String resolvedStudentId = filterId;
        try {
            dao.StudentDAO studentDAO = new dao.StudentDAO();
            model.Student student = studentDAO.getStudentByUserId(filterId);
            if (student != null && student.getUserId() != null) {
                resolvedStudentId = student.getUserId();
            }
        } catch (Exception ex) { /* ignore */ }

        com.mongodb.client.MongoCollection<org.bson.Document> enrollCol = database.getCollection("enrollments");
        final String sid = resolvedStudentId;
        List<org.bson.Document> enrollments = enrollCol.find(
            Filters.and(
                Filters.or(
                    Filters.eq("student_user_id", sid),
                    Filters.eq("student_id", sid),
                    Filters.eq("user_id", sid)
                ),
                com.mongodb.client.model.Filters.regex("status", "^ACTIVE$", "i")
            )
        ).into(new ArrayList<>());

        List<Integer> batchIds = new ArrayList<>();
        for (org.bson.Document e : enrollments) {
            Object bIdObj = e.get("batch_id");
            if (bIdObj instanceof Number) batchIds.add(((Number) bIdObj).intValue());
        }

        if (!batchIds.isEmpty()) {
            com.mongodb.client.MongoCollection<org.bson.Document> batchColl = database.getCollection("batches");
            com.mongodb.client.MongoCollection<org.bson.Document> teacherColl = database.getCollection("teachers");
            List<org.bson.Document> batchDocs = batchColl.find(
                Filters.in("_id", batchIds)
            ).into(new ArrayList<>());

            for (org.bson.Document bDoc : batchDocs) {
                Object tIdObj = bDoc.get("teacher_id");
                String tId = tIdObj != null ? tIdObj.toString() : null;
                org.bson.Document teacherDoc = teacherColl.find(Filters.eq("user_id", tId)).first();
                if (teacherDoc == null) teacherDoc = teacherColl.find(Filters.eq("_id", tId)).first();

                Batch b = DocumentMapper.documentToBatch(bDoc);
                if (b != null) {
                    Object tNameObj = teacherDoc != null ? teacherDoc.get("full_name") : null;
                    if (tNameObj == null && teacherDoc != null) tNameObj = teacherDoc.get("name");
                    String tName = tNameObj != null ? tNameObj.toString() : "Not Assigned";
                    b.setTeacherUserId(tName);
                    result.add(b);
                }
            }
        }
        return result;
    }
}
