package dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mongodb.client.MongoDatabase;
import db.DBConnection;

/**
 * DashboardDAO — centralised analytics layer.
 * All counts go through here; individual management DAOs remain focused on CRUD.
 */
public class DashboardDAO {

    private MongoDatabase db;

    public DashboardDAO() {
        db = DBConnection.getDatabase();
    }

    // ── Collection counts ──────────────────────────────────────────────────────

    public long getTotalStudents() {
        try {
            if (db != null) return db.getCollection("students").countDocuments();
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public long getTotalTeachers() {
        try {
            if (db != null) return db.getCollection("teachers").countDocuments();
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public long getTotalSubjects() {
        try {
            if (db != null) return db.getCollection("subjects").countDocuments();
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public long getTotalBatches() {
        try {
            if (db != null) return db.getCollection("batches").countDocuments();
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    // ── Attendance Summary ─────────────────────────────────────────────────────

    public Map<String, Integer> getAttendanceSummary() {
        return new AttendanceDAO().getAttendanceSummary();
    }

    // ── Dynamic System Alerts ──────────────────────────────────────────────────

    public List<String> getSystemAlerts() {
        List<String> alerts = new ArrayList<>();
        try {
            if (db == null) return alerts;

            // Alert 1: Batches with no teacher assigned
            long batchesWithoutTeacher = db.getCollection("batches")
                    .countDocuments(
                        com.mongodb.client.model.Filters.or(
                            com.mongodb.client.model.Filters.exists("teacher_id", false),
                            com.mongodb.client.model.Filters.eq("teacher_id", null),
                            com.mongodb.client.model.Filters.eq("teacher_id", "")
                        )
                    );
            if (batchesWithoutTeacher > 0) {
                alerts.add("⚠  " + batchesWithoutTeacher + " batch(es) have no teacher assigned.");
            }

            // Alert 2: Pending / overdue fee payments
            long pendingFees = db.getCollection("fees")
                    .countDocuments(
                        com.mongodb.client.model.Filters.in("status", "PENDING", "OVERDUE")
                    );
            if (pendingFees > 0) {
                alerts.add("💰  " + pendingFees + " fee record(s) are PENDING or OVERDUE.");
            }

            // Alert 3: Low attendance (absent count)
            Map<String, Integer> att = getAttendanceSummary();
            int absentCount = att.getOrDefault("ABSENT", 0);
            if (absentCount > 5) {
                alerts.add("📉  " + absentCount + " attendance records marked ABSENT this period.");
            }

            // Alert 4: Total student count sanity
            long students = getTotalStudents();
            if (students == 0) {
                alerts.add("ℹ  No student records found in the database.");
            }

            if (alerts.isEmpty()) {
                alerts.add("✅  All systems operational. No alerts.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            alerts.add("❌  Error fetching system alerts: " + e.getMessage());
        }
        return alerts;
    }
}
