package service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;

import db.DBConnection;

/**
 * Singleton notification service — the ONLY class that writes to the
 * 'notifications' collection in tuitionManagementDB.
 *
 * Called from DAO methods after every data-changing operation.
 * Thread-safe. Never throws — swallows all exceptions so a failed
 * notification never crashes the main operation.
 */
public class NotificationService {

    // ── Singleton ──────────────────────────────────────────────────────────────
    private static volatile NotificationService instance;

    private MongoCollection<Document> notifCol;

    private NotificationService() {
        try {
            MongoDatabase db = DBConnection.getDatabase();
            if (db != null) {
                notifCol = db.getCollection("notifications");
            }
        } catch (Exception e) {
            System.err.println("[NotificationService] Init error: " + e.getMessage());
        }
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            synchronized (NotificationService.class) {
                if (instance == null) instance = new NotificationService();
            }
        }
        return instance;
    }

    // ── Notification Type constants ─────────────────────────────────────────────
    public static final String ENROLLMENT_CONFIRMED = "ENROLLMENT_CONFIRMED";
    public static final String FEE_PAID             = "FEE_PAID";
    public static final String FEE_PARTIAL          = "FEE_PARTIAL";
    public static final String FEE_OVERDUE          = "FEE_OVERDUE";
    public static final String ATTENDANCE_ABSENT    = "ATTENDANCE_ABSENT";
    public static final String ATTENDANCE_ALERT     = "ATTENDANCE_ALERT";
    public static final String TEST_SCHEDULED       = "TEST_SCHEDULED";
    public static final String TEST_RESULT          = "TEST_RESULT";
    public static final String TEST_RESULT_PENDING  = "TEST_RESULT_PENDING";
    public static final String MARKS_UPDATED        = "MARKS_UPDATED";
    public static final String SALARY_PROCESSED     = "SALARY_PROCESSED";
    public static final String SALARY_DEDUCTION     = "SALARY_DEDUCTION";
    public static final String SALARY_BONUS         = "SALARY_BONUS";
    public static final String BATCH_ASSIGNED       = "BATCH_ASSIGNED";
    public static final String ANNOUNCEMENT         = "ANNOUNCEMENT";

    // ── Recipient Role constants ────────────────────────────────────────────────
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_PARENT  = "PARENT";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_ADMIN   = "ADMIN";

    // ── Core push ──────────────────────────────────────────────────────────────
    /**
     * Insert one or more notification documents atomically.
     * Never throws — errors are logged to stderr only.
     */
    public void push(NotificationDocument... docs) {
        if (notifCol == null || docs == null || docs.length == 0) return;
        try {
            List<Document> bsonList = new ArrayList<>();
            for (NotificationDocument nd : docs) {
                if (nd != null) bsonList.add(nd.toBson());
            }
            if (bsonList.size() == 1) {
                notifCol.insertOne(bsonList.get(0));
            } else if (bsonList.size() > 1) {
                notifCol.insertMany(bsonList);
            }
        } catch (Exception e) {
            System.err.println("[NotificationService] push error: " + e.getMessage());
        }
    }

    // ── Unread count (used by bell poller) ─────────────────────────────────────
    public long getUnreadCount(String recipientType, String recipientId) {
        if (notifCol == null) return 0;
        try {
            return notifCol.countDocuments(
                Filters.and(
                    Filters.eq("recipient_type", recipientType),
                    recipientIdFilter(recipientType, recipientId),
                    Filters.or(Filters.eq("is_read", false), Filters.eq("read", false))
                )
            );
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Mark single notification read ──────────────────────────────────────────
    public void markAsRead(String notificationId) {
        if (notifCol == null || notificationId == null) return;
        try {
            notifCol.updateOne(
                new Document("_id", notificationId),
                new Document("$set", new Document("is_read", true))
            );
        } catch (Exception e) {
            System.err.println("[NotificationService] markAsRead error: " + e.getMessage());
        }
    }

    // ── Mark ALL notifications read ─────────────────────────────────────────────
    public void markAllAsRead(String recipientType, String recipientId) {
        if (notifCol == null) return;
        try {
            notifCol.updateMany(
                Filters.and(
                    Filters.eq("recipient_type", recipientType),
                    recipientIdFilter(recipientType, recipientId),
                    Filters.or(Filters.eq("is_read", false), Filters.eq("read", false))
                ),
                new Document("$set", new Document("is_read", true).append("read", true))
            );
        } catch (Exception e) {
            System.err.println("[NotificationService] markAllAsRead error: " + e.getMessage());
        }
    }

    // ── Fetch recent notifications for inbox ───────────────────────────────────
    public List<Document> getRecentNotifications(String recipientType,
                                                  String recipientId,
                                                  int limit) {
        List<Document> list = new ArrayList<>();
        if (notifCol == null) return list;
        try {
            notifCol.find(
                Filters.and(
                    Filters.eq("recipient_type", recipientType),
                    recipientIdFilter(recipientType, recipientId)
                )
            )
            .sort(Sorts.descending("date"))
            .limit(limit)
            .into(list);
        } catch (Exception e) {
            System.err.println("[NotificationService] getRecentNotifications error: " + e.getMessage());
        }
        return list;
    }

    private Bson recipientIdFilter(String recipientType, String recipientId) {
        List<String> ids = new ArrayList<>();
        if (recipientId != null && !recipientId.isBlank()) ids.add(recipientId.trim());

        if (ROLE_TEACHER.equalsIgnoreCase(recipientType) && recipientId != null) {
            try {
                MongoDatabase db = DBConnection.getDatabase();
                if (db != null) {
                    Document teacherDoc = db.getCollection("teachers").find(Filters.or(
                        Filters.eq("_id", recipientId),
                        Filters.eq("user_id", recipientId)
                    )).first();
                    if (teacherDoc != null) {
                        Object id = teacherDoc.get("_id");
                        String userId = teacherDoc.getString("user_id");
                        if (id != null) ids.add(id.toString());
                        if (userId != null) ids.add(userId);
                    }
                }
            } catch (Exception ignored) {}
        }

        return Filters.in("recipient_id", ids);
    }

    // ── ID generator ───────────────────────────────────────────────────────────
    public static String generateId(String prefix) {
        return prefix + "_" + System.currentTimeMillis()
             + "_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    // =========================================================================
    // Legacy convenience methods (called by existing DAO code)
    // These are kept for backward compatibility with existing callers.
    // =========================================================================

    /** Called after a fee payment is confirmed (legacy PaymentDAO call). */
    public void notifyFeePaid(String parentId, String studentId,
                              String studentName, double amount,
                              String batchName, String mode) {
        String monthStr = new java.text.SimpleDateFormat("MMM yyyy").format(new Date());
        String msgStu = String.format(
            "Your fee of ₹%,.0f for '%s' has been received for %s via %s. Thank you!",
            amount, batchName, monthStr, mode);
        String msgPar = String.format(
            "Fee of ₹%,.0f for %s ('%s') has been received for %s via %s.",
            amount, studentName, batchName, monthStr, mode);

        push(
            new NotificationDocument(ROLE_STUDENT, studentId,
                FEE_PAID, "Fee Payment Confirmed — " + monthStr, msgStu)
                .studentId(studentId).studentName(studentName).batchId(batchName),
            new NotificationDocument(ROLE_PARENT, parentId,
                FEE_PAID, "Fee Payment Confirmed — " + monthStr, msgPar)
                .studentId(studentId).studentName(studentName).batchId(batchName)
        );
    }

    /** Called when marks are updated (legacy TestsDAO call). */
    public void notifyMarksUpdated(String parentId, String studentId,
                                   String studentName, String testName,
                                   int score, int totalMarks) {
        double pct  = totalMarks > 0 ? (score * 100.0 / totalMarks) : 0;
        String grade = calculateGrade(pct);
        String perf  = pct >= 90 ? "Outstanding!" : pct >= 75 ? "Well done!" :
                       pct >= 60 ? "Good effort." : "Seek help from your teacher.";

        String msgStu = String.format(
            "Your result for '%s' has been published. Score: %d/%d (%s — Grade %s). %s",
            testName, score, totalMarks, String.format("%.0f%%", pct), grade, perf);
        String msgPar = String.format(
            "%s scored %d/%d (%.0f%% — Grade %s) in '%s'.",
            studentName, score, totalMarks, pct, grade, testName);

        push(
            new NotificationDocument(ROLE_STUDENT, studentId,
                TEST_RESULT, "Test Result Published — " + testName, msgStu)
                .studentId(studentId).studentName(studentName),
            new NotificationDocument(ROLE_PARENT, parentId,
                TEST_RESULT, "Test Result — " + studentName, msgPar)
                .studentId(studentId).studentName(studentName)
        );
    }

    /** Called when attendance falls below 75% (legacy AttendanceDAO call). */
    public void notifyLowAttendance(String parentId, String studentId,
                                    String studentName, double attendancePercent) {
        if (attendancePercent >= 75.0) return; // Only notify if actually low
        String msg = String.format(
            "%s's current attendance is %.1f%%. Minimum required is 75%%. " +
            "Please ensure regular attendance to avoid any issues.",
            studentName, attendancePercent);

        push(
            new NotificationDocument(ROLE_STUDENT, studentId,
                ATTENDANCE_ALERT, "⚠ Low Attendance Warning", msg)
                .studentId(studentId).studentName(studentName),
            new NotificationDocument(ROLE_PARENT, parentId,
                ATTENDANCE_ALERT, "⚠ Low Attendance Warning — " + studentName, msg)
                .studentId(studentId).studentName(studentName)
        );
    }

    /** Called when payment is overdue. */
    public void notifyFeeOverdue(String parentId, String studentId,
                                  String studentName, String batchName, double amount) {
        String msg = String.format(
            "Fee of ₹%,.0f for batch '%s' is still PENDING for %s. " +
            "Please pay immediately to avoid suspension.", amount, batchName, studentName);

        push(
            new NotificationDocument(ROLE_STUDENT, studentId,
                FEE_OVERDUE, "🔴 Fee Payment Overdue", msg)
                .studentId(studentId).studentName(studentName),
            new NotificationDocument(ROLE_PARENT, parentId,
                FEE_OVERDUE, "🔴 Fee Payment Overdue — " + studentName, msg)
                .studentId(studentId).studentName(studentName),
            new NotificationDocument(ROLE_ADMIN, "ADMIN",
                FEE_OVERDUE, "Fee Overdue Alert — " + studentName,
                String.format("Student %s has an unpaid fee of ₹%,.0f for '%s'. Please follow up.",
                    studentName, amount, batchName))
                .studentId(studentId).studentName(studentName)
        );
    }

    /** General announcement push (legacy). */
    public void notifyAnnouncement(String parentId, String studentId,
                                   String studentName, String title, String message) {
        push(
            new NotificationDocument(ROLE_PARENT, parentId,
                ANNOUNCEMENT, "📢 " + title, message)
                .studentId(studentId).studentName(studentName)
        );
    }

    // ── Grade helper ───────────────────────────────────────────────────────────
    private static String calculateGrade(double pct) {
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B";
        if (pct >= 60) return "C";
        return "F";
    }
}
