package service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import db.DBConnection;
import org.bson.Document;
import java.util.Date;

public class NotificationService {

    private MongoCollection<Document> notifCol;

    public NotificationService() {
        MongoDatabase db = DBConnection.getDatabase();
        this.notifCol = db.getCollection("notifications");
    }

    // ── Called after a fee payment is recorded ─────────────────────────────
    public void notifyFeePaid(String parentId, String studentId,
            String studentName, double amount, String batchName, String mode) {
        insertNotification(parentId, studentId, studentName,
            "FEE_PAID",
            "✅ Fee Payment Confirmed",
            "Fee of ₹" + (int) amount + " for batch \"" + batchName 
                + "\" paid successfully via " + mode + ".",
            new Date());
    }

    // ── Called after teacher enters marks in tests collection ───────────────
    public void notifyMarksUpdated(String parentId, String studentId,
            String studentName, String testName, int score, int totalMarks) {
        String grade = calculateGrade(score, totalMarks);
        insertNotification(parentId, studentId, studentName,
            "MARKS_UPDATED",
            "📊 Test Result Published",
            studentName + " scored " + score + "/" + totalMarks 
                + " (Grade: " + grade + ") in \"" + testName + "\".",
            new Date());
    }

    // ── Called after attendance is marked in the attendance collection ──────
    public void notifyLowAttendance(String parentId, String studentId,
            String studentName, double attendancePercent) {
        if (attendancePercent < 75.0) {
            insertNotification(parentId, studentId, studentName,
                "ATTENDANCE_ALERT",
                "⚠️ Low Attendance Warning",
                studentName + "'s current attendance is " 
                    + String.format("%.1f", attendancePercent) 
                    + "%. Minimum required is 75%. Please ensure regular attendance.",
                new Date());
        }
    }

    // ── Called when a payment has status PENDING/OVERDUE in payments ───────
    public void notifyFeeOverdue(String parentId, String studentId,
            String studentName, String batchName, double amount) {
        insertNotification(parentId, studentId, studentName,
            "FEE_OVERDUE",
            "🔴 Fee Payment Overdue",
            "Fee of ₹" + (int) amount + " for batch \"" + batchName 
                + "\" is still PENDING. Please pay immediately to avoid suspension.",
            new Date());
    }

    // ── Called by admin for general announcements ───────────────────────────
    public void notifyAnnouncement(String parentId, String studentId,
            String studentName, String title, String message) {
        insertNotification(parentId, studentId, studentName,
            "ANNOUNCEMENT", "📢 " + title, message, new Date());
    }

    // ── Internal insert helper ──────────────────────────────────────────────
    private void insertNotification(String parentId, String studentId,
            String studentName, String type, String title, 
            String message, Date date) {
        try {
            Document doc = new Document()
                .append("parent_id", parentId)
                .append("student_id", studentId)
                .append("student_name", studentName)
                .append("type", type)
                .append("title", title)
                .append("message", message)
                .append("date", date)
                .append("is_read", false);
            notifCol.insertOne(doc);
            System.out.println("[NotificationService] Inserted: " 
                + type + " for parent " + parentId);
        } catch (Exception e) {
            System.err.println("[NotificationService] Failed to insert: " 
                + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Grade calculator ────────────────────────────────────────────────────
    private String calculateGrade(int score, int total) {
        if (total == 0) return "-";
        double p = (double) score / total * 100;
        if (p >= 90) return "A+";
        if (p >= 80) return "A";
        if (p >= 70) return "B";
        if (p >= 60) return "C";
        return "F";
    }
}
