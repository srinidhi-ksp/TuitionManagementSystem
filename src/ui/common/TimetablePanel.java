package ui.common;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import dao.BatchDAO;
import model.Batch;
import model.Schedule;
import util.ThemeManager;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import db.DBConnection;
import db.DocumentMapper;

public class TimetablePanel extends JPanel {

    private String viewType; // ADMIN, TEACHER, STUDENT
    private String filterId; // teacherId or studentId
    private BatchDAO batchDAO;
    
    private JPanel gridContainer;
    private final String[] DAYS = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
    private Set<String> timeSlots = new TreeSet<>();

    public TimetablePanel(String viewType, String filterId) {
        this.viewType = viewType;
        this.filterId = filterId;
        this.batchDAO = new BatchDAO();

        setLayout(new BorderLayout(10, 10));
        setBackground(ThemeManager.BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel(viewType + " Weekly Timetable");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(ThemeManager.TEXT);
        add(title, BorderLayout.NORTH);

        gridContainer = new JPanel();
        gridContainer.setBackground(ThemeManager.BG);
        
        JScrollPane scroll = new JScrollPane(gridContainer);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        refreshTimetable();
    }

    public void refreshTimetable() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database == null) return;

        List<Batch> displayBatches = new ArrayList<>();
        
        try {
            if ("STUDENT".equals(viewType)) {
                // STEP 1 — RESOLVE USER ID → STUDENT ID
                // filterId from StudentDashboard is the User ID (e.g. U21).
                // Enrollments store the Student ID (e.g. S001). Resolve it first.
                String resolvedStudentId = filterId;
                try {
                    dao.StudentDAO studentDAO = new dao.StudentDAO();
                    model.Student student = studentDAO.getStudentByUserId(filterId);
                    if (student != null && student.getUserId() != null) {
                        resolvedStudentId = student.getUserId();
                        System.out.println("[TimetablePanel] Resolved " + filterId + " → " + resolvedStudentId);
                    } else {
                        System.out.println("[TimetablePanel] Could not resolve User ID, using raw: " + filterId);
                    }
                } catch (Exception ex) {
                    System.err.println("[TimetablePanel] Error resolving student ID: " + ex.getMessage());
                }

                // STEP 2 — FETCH ENROLLMENTS (OR across all 3 field name variants)
                com.mongodb.client.MongoCollection<org.bson.Document> enrollmentsCollection = database.getCollection("enrollments");
                final String sid = resolvedStudentId;
                List<org.bson.Document> enrollments = enrollmentsCollection.find(
                    Filters.and(
                        Filters.or(
                            Filters.eq("student_user_id", sid),
                            Filters.eq("student_id", sid),
                            Filters.eq("user_id", sid)
                        ),
                        com.mongodb.client.model.Filters.regex("status", "^ACTIVE$", "i")
                    )
                ).into(new ArrayList<>());
                System.out.println("[TimetablePanel] Found " + enrollments.size() + " enrollments for student: " + sid);

                // STEP 3 — EXTRACT BATCH IDS
                List<Integer> batchIds = new ArrayList<>();
                for (org.bson.Document e : enrollments) {
                    Object bIdObj = e.get("batch_id");
                    if (bIdObj instanceof Number) {
                        batchIds.add(((Number) bIdObj).intValue());
                    }
                }

                if (!batchIds.isEmpty()) {
                    // STEP 3 — FETCH TIMETABLE (using batches as source of schedule)
                    com.mongodb.client.MongoCollection<org.bson.Document> batchColl = database.getCollection("batches");
                    com.mongodb.client.MongoCollection<org.bson.Document> teacherColl = database.getCollection("teachers");

                    List<org.bson.Document> batchesDocs = batchColl.find(
                        Filters.in("_id", batchIds)
                    ).into(new ArrayList<>());

                    for (org.bson.Document bDoc : batchesDocs) {
                        // STEP 4 — JOIN WITH BATCH + TEACHER
                        Object tIdObj = bDoc.get("teacher_id");
                        String tId = tIdObj != null ? tIdObj.toString() : null;
                        
                        org.bson.Document teacherDoc = teacherColl.find(Filters.eq("user_id", tId)).first();
                        if (teacherDoc == null) teacherDoc = teacherColl.find(Filters.eq("_id", tId)).first();

                        Batch b = DocumentMapper.documentToBatch(bDoc);
                        if (b != null) {
                            // Link teacher name for display (Part 6 Step 4)
                            Object tNameObj = teacherDoc != null ? teacherDoc.get("full_name") : null;
                            if (tNameObj == null && teacherDoc != null) tNameObj = teacherDoc.get("name");
                            
                            String tName = tNameObj != null ? tNameObj.toString() : "Not Assigned";
                            
                            // Temporary storage for display logic
                            b.setTeacherUserId(tName); // Using teacherId field as name carrier for the UI labels
                            displayBatches.add(b);
                        }
                    }
                }
            } else if ("TEACHER".equals(viewType)) {
                displayBatches = batchDAO.getBatchesByTeacher(filterId);
            } else {
                displayBatches = batchDAO.getAllBatches();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 1. Extract dynamic time slots
        timeSlots.clear();
        for (Batch b : displayBatches) {
            if (b.getSchedules() != null) {
                for (Schedule s : b.getSchedules()) {
                    timeSlots.add(s.getStart() + " - " + s.getEnd());
                }
            }
        }

        if (timeSlots.isEmpty()) {
            timeSlots.add("09:00 - 10:30");
            timeSlots.add("11:00 - 12:30");
            timeSlots.add("14:00 - 15:30");
        }

        // 2. Build Grid
        gridContainer.removeAll();
        int cols = timeSlots.size() + 1; // +1 for Days column
        gridContainer.setLayout(new GridLayout(8, cols, 10, 10)); // 7 days + 1 header row

        // Header Row
        gridContainer.add(createHeaderLabel("DAY / TIME"));
        for (String slot : timeSlots) {
            gridContainer.add(createHeaderLabel(slot));
        }

        // Day Rows
        for (String day : DAYS) {
            gridContainer.add(createDayLabel(day));
            for (String slot : timeSlots) {
                gridContainer.add(createCell(day, slot, displayBatches));
            }
        }

        gridContainer.revalidate();
        gridContainer.repaint();
    }

    private JLabel createHeaderLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(ThemeManager.SUB_TEXT);
        l.setOpaque(true);
        l.setBackground(ThemeManager.BG);
        l.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        return l;
    }

    private JLabel createDayLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        l.setForeground(ThemeManager.TEXT);
        l.setOpaque(true);
        l.setBackground(new Color(241, 245, 249));
        l.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        return l;
    }

    private JPanel createCell(String day, String slot, List<Batch> batches) {
        JPanel cell = new JPanel(new GridLayout(0, 1, 2, 2));
        cell.setBorder(BorderFactory.createLineBorder(ThemeManager.DIVIDER));
        cell.setBackground(ThemeManager.CARD);

        List<Batch> matches = new ArrayList<>();
        for (Batch b : batches) {
            if (b.getSchedules() != null) {
                for (Schedule s : b.getSchedules()) {
                    String bSlot = s.getStart() + " - " + s.getEnd();
                    if (day.equals(s.getDay()) && slot.equals(bSlot)) {
                        matches.add(b);
                    }
                }
            }
        }

        if (matches.isEmpty()) {
            JLabel dash = new JLabel("-", SwingConstants.CENTER);
            dash.setForeground(Color.LIGHT_GRAY);
            cell.add(dash);
        } else if (matches.size() == 1) {
            Batch b = matches.get(0);
            Color defaultBg = new Color(219, 234, 254);
            Color hoverBg = new Color(191, 219, 254);
            cell.setBackground(defaultBg);
            cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            // Use the name stored in teacherUserId if available (from join in Part 6 Step 4)
            String tName = b.getTeacherUserId() != null ? b.getTeacherUserId() : "Not Assigned";
            if (tName.equals(b.getTeacherId())) { // fallback if join wasn't done
                dao.TeacherDAO tDAO = new dao.TeacherDAO();
                model.Teacher teacher = tDAO.getTeacherById(b.getTeacherId());
                tName = (teacher != null) ? teacher.getName() : "Not Assigned";
            }
            
            cell.setToolTipText("Teacher: " + tName);
            cell.putClientProperty("batch", b);
            
            JLabel name = new JLabel("<html><div style='text-align:center;'><b>" + b.getBatchName() + "</b></div></html>", SwingConstants.CENTER);
            name.setFont(new Font("SansSerif", Font.PLAIN, 11));
            cell.add(name);
            
            final String finalTName = tName;
            cell.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { cell.setBackground(hoverBg); }
                public void mouseExited(java.awt.event.MouseEvent e) { cell.setBackground(defaultBg); }
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    Batch batchObj = (Batch) cell.getClientProperty("batch");
                    if (batchObj != null) {
                        showBatchDetailsPopup(batchObj, finalTName, day, slot);
                    }
                }
            });
        } else {
            Color defaultBg = new Color(219, 234, 254);
            Color hoverBg = new Color(191, 219, 254);
            cell.setBackground(defaultBg);
            cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cell.setToolTipText(matches.size() + " Batches. Click for details.");
            
            JLabel conf = new JLabel("<html><div style='text-align:center;'><b>" + matches.size() + " Batches</b></div></html>", SwingConstants.CENTER);
            conf.setForeground(new Color(59, 130, 246));
            conf.setFont(new Font("SansSerif", Font.BOLD, 10));
            cell.add(conf);
            
            cell.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { cell.setBackground(hoverBg); }
                public void mouseExited(java.awt.event.MouseEvent e) { cell.setBackground(defaultBg); }
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    showConflictPopup(matches, day, slot);
                }
            });
        }

        return cell;
    }

    private void showBatchDetailsPopup(Batch b, String tName, String day, String slot) {
        dao.TestsDAO tDAO = new dao.TestsDAO();
        List<model.Test> tests = tDAO.getTestsByBatchId(b.getBatchId());
        model.Test nextTest = null;
        java.util.Date now = new java.util.Date();
        for (model.Test test : tests) {
            if (test.getTestDate() != null && test.getTestDate().after(now)) {
                if (nextTest == null || test.getTestDate().before(nextTest.getTestDate())) {
                    nextTest = test;
                }
            }
        }
        
        String testInfo = (nextTest != null) 
            ? nextTest.getTestName() + " (" + new java.text.SimpleDateFormat("dd MMM").format(nextTest.getTestDate()) + ")"
            : "No upcoming tests";

        String details = "<html><body style='width: 300px; padding: 15px; font-family: Segoe UI, SansSerif;'>"
            + "<h2 style='color:#1e3a8a; margin-top:0;'>Batch Details</h2>"
            + "<hr style='border: 0; border-top: 1px solid #eee; margin-bottom: 15px;'>"
            + "<b>Batch Name:</b> " + b.getBatchName() + "<br><br>"
            + "<b>Teacher:</b> " + tName + "<br><br>"
            + "<b>Schedule:</b> " + day + " " + slot + "<br><br>"
            + "<b>Mode:</b> " + (b.getMode() != null ? b.getMode() : "Offline") + "<br><br>"
            + ( "Online".equalsIgnoreCase(b.getMode()) ? "<b>Meeting Link:</b> <a href='#'>" + (b.getMeetingLink() != null ? b.getMeetingLink() : "NULL") + "</a><br><br>" : "" )
            + "<b>Upcoming Test:</b> <span style='color:#e67e22;'><b>" + testInfo + "</b></span>"
            + "</body></html>";
            
        JOptionPane.showMessageDialog(this, new JLabel(details), "Batch Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showConflictPopup(List<Batch> matches, String day, String slot) {
        dao.TeacherDAO tDAO = new dao.TeacherDAO();
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='width: 300px; padding: 10px; font-family: SansSerif;'>");
        sb.append("<h2 style='color:#dc2626; margin-top:0;'>⚠ Two Batches Detected</h2>");
        sb.append("<b>Time Slot:</b> ").append(slot).append("<br>");
        sb.append("<b>Day:</b> ").append(day).append("<br><br>");
        sb.append("<b>Conflicting Batches:</b><br><br>");
        
        for (int i = 0; i < matches.size(); i++) {
            Batch b = matches.get(i);
            model.Teacher t = tDAO.getTeacherById(b.getTeacherId());
            String tName = t != null ? t.getName() : "Not Assigned";
            sb.append(i + 1).append(". <b>Batch:</b> ").append(b.getBatchName()).append("<br>");
            sb.append("&nbsp;&nbsp;&nbsp;<b>Teacher:</b> ").append(tName).append("<br><br>");
        }
        sb.append("</body></html>");
        
        JOptionPane.showMessageDialog(this, new JLabel(sb.toString()), "Conflict Details", JOptionPane.WARNING_MESSAGE);
    }
}
