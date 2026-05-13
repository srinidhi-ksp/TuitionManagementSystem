package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import model.Attendance;
import db.DBConnection;
import db.DocumentMapper;

public class AttendanceDAO {

    private MongoCollection<Document> attendanceCollection;

    public AttendanceDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            attendanceCollection = database.getCollection("attendance"); // Assuming the collection is named 'attendance'
        }
    }

    public boolean addAttendance(Attendance attendance) {
        if (attendanceCollection == null) return false;
        try {
            Document doc = DocumentMapper.attendanceToDocument(attendance);
            attendanceCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Attendance getAttendanceById(int attendanceId) {
        if (attendanceCollection == null) return null;
        try {
            Document doc = attendanceCollection.find(Filters.eq("_id", attendanceId)).first();
            return DocumentMapper.documentToAttendance(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Attendance> getAllAttendance() {
        List<Attendance> list = new ArrayList<>();
        if (attendanceCollection == null) return list;

        try (MongoCursor<Document> cursor = attendanceCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Attendance a = DocumentMapper.documentToAttendance(doc);
                if (a != null) {
                    list.add(a);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Attendance> getAttendanceByStudentId(String studentId) {
        List<Attendance> list = new ArrayList<>();
        if (attendanceCollection == null) return list;

        try (MongoCursor<Document> cursor = attendanceCollection.find(Filters.eq("user_id", studentId)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Attendance a = DocumentMapper.documentToAttendance(doc);
                if (a != null) list.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public double getAttendancePercentage(String studentId) {
        if (attendanceCollection == null) return 0.0;
        try {
            List<Attendance> records = getAttendanceByStudentId(studentId);
            if (records.isEmpty()) return 0.0;
            long relevantCount = records.stream()
                    .filter(a -> !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                    .count();
            if (relevantCount == 0) return 0.0;
            long presentCount = records.stream()
                    .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                    .count();
            return (double) presentCount / relevantCount * 100;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public boolean deleteAttendance(int attendanceId) {
        if (attendanceCollection == null) return false;
        try {
            long deletedCount = attendanceCollection.deleteOne(Filters.eq("_id", attendanceId)).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ── Attendance Summary grouped by status ──
    public Map<String, Integer> getAttendanceSummary() {
        Map<String, Integer> summary = new HashMap<>();
        summary.put("PRESENT", 0);
        summary.put("ABSENT", 0);
        summary.put("LEAVE", 0);
        summary.put("CANCELLED", 0);

        if (attendanceCollection == null) return summary;
        try (MongoCursor<Document> cursor = attendanceCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String status = doc.getString("status");
                if (status != null) {
                    String key = status.toUpperCase();
                    summary.merge(key, 1, Integer::sum);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return summary;
    }

    // ── Fetch attendance records for a given batch on a given date string (yyyy-MM-dd) ──
    public List<Attendance> getAttendanceByBatchAndDate(int batchId, String dateStr) {
        List<Attendance> list = new ArrayList<>();
        if (attendanceCollection == null) return list;
        try (MongoCursor<Document> cursor = attendanceCollection.find(
                Filters.and(
                    batchIdFilter(batchId),
                    Filters.eq("date_str", dateStr)
                )).iterator()) {
            while (cursor.hasNext()) {
                Attendance a = DocumentMapper.documentToAttendance(cursor.next());
                if (a != null) list.add(a);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── Upsert: insert or replace attendance record keyed on user_id + batch_id + date_str ──
    public boolean saveOrUpdateAttendance(Attendance att, int batchId, String dateStr) {
        if (attendanceCollection == null) return false;
        try {
            Document doc = DocumentMapper.attendanceToDocument(att);
            doc.append("batch_id", batchId);
            doc.append("date_str", dateStr);
            doc.append("type", "STUDENT");
            doc.append("student_id", att.getUserId());
            if (att.getMarkedBy() != null) doc.append("teacher_id", att.getMarkedBy());
            attendanceCollection.replaceOne(
                Filters.and(
                    Filters.eq("user_id",  att.getUserId()),
                    batchIdFilter(batchId),
                    Filters.eq("date_str", dateStr)
                ),
                doc,
                new ReplaceOptions().upsert(true)
            );
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasAttendanceForBatchAndDate(int batchId, String dateStr) {
        if (attendanceCollection == null) return false;
        try {
            return attendanceCollection.countDocuments(
                Filters.and(
                    batchIdFilter(batchId),
                    Filters.eq("date_str", dateStr),
                    Filters.eq("type", "STUDENT")
                )
            ) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveBatchAttendance(String teacherId, int batchId, String dateStr, Map<String, String> studentStatuses) {
        if (attendanceCollection == null || studentStatuses == null) return false;
        try {
            java.util.Date attendanceDate = parseDateOrToday(dateStr);
            for (Map.Entry<String, String> entry : studentStatuses.entrySet()) {
                String studentId = entry.getKey();
                String status = normalizeStatus(entry.getValue());
                Document doc = new Document()
                    .append("_id", Math.abs((studentId + "_" + batchId + "_" + dateStr).hashCode()))
                    .append("teacher_id", teacherId)
                    .append("batch_id", batchId)
                    .append("student_id", studentId)
                    .append("user_id", studentId)
                    .append("date", attendanceDate)
                    .append("attendance_date", attendanceDate)
                    .append("date_str", dateStr)
                    .append("status", status)
                    .append("type", "STUDENT")
                    .append("marked_by", teacherId);
                attendanceCollection.replaceOne(
                    Filters.and(
                        Filters.eq("student_id", studentId),
                        batchIdFilter(batchId),
                        Filters.eq("date_str", dateStr),
                        Filters.eq("type", "STUDENT")
                    ),
                    doc,
                    new ReplaceOptions().upsert(true)
                );
            }
            // TRIGGER ABSENCE + LOW-ATTENDANCE NOTIFICATIONS
            final String capturedTeacherId = teacherId;
            final int capturedBatchId = batchId;
            final String capturedDateStr = dateStr;
            final java.util.Map<String,String> capturedStatuses = new java.util.HashMap<>(studentStatuses);
            new Thread(() -> {
                try {
                    MongoDatabase db = DBConnection.getDatabase();
                    if (db == null) return;

                    // Resolve batch name for messages
                    Document batchDoc = db.getCollection("batches").find(
                        batchDocIdFilter(capturedBatchId)).first();
                    String batchName = batchDoc != null ? batchDoc.getString("batch_name") : "Batch " + capturedBatchId;
                    String subjectName = "";
                    if (batchDoc != null) {
                        Object subId = batchDoc.get("subject_id");
                        if (subId != null) {
                            Document subDoc = db.getCollection("subjects").find(
                                Filters.eq("_id", subId)).first();
                            if (subDoc != null) subjectName = subDoc.getString("name") != null
                                ? subDoc.getString("name") : String.valueOf(subDoc.get("name"));
                        }
                    }

                    String dateLabel = "";
                    try {
                        java.util.Date pd = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(capturedDateStr);
                        dateLabel = new java.text.SimpleDateFormat("EEEE, dd MMM yyyy").format(pd);
                    } catch (Exception ignored) { dateLabel = capturedDateStr; }

                    service.NotificationService ns = service.NotificationService.getInstance();

                    for (java.util.Map.Entry<String,String> e : capturedStatuses.entrySet()) {
                        String studentId = e.getKey();
                        String status    = e.getValue();

                        Document studentDoc = db.getCollection("students").find(
                            Filters.or(Filters.eq("_id", studentId),
                                       Filters.eq("user_id", studentId))).first();
                        if (studentDoc == null) continue;

                        String studentName = studentDoc.getString("full_name");
                        String parentId    = studentDoc.getString("parent_user_id");
                        if (parentId == null) {
                            Document pe = (Document) studentDoc.get("parent");
                            parentId = pe != null ? pe.getString("parent_id") : null;
                        }

                        if ("ABSENT".equalsIgnoreCase(status) || "LEAVE".equalsIgnoreCase(status)) {
                            String label = "LEAVE".equalsIgnoreCase(status) ? "on leave" : "absent";

                            ns.push(new service.NotificationDocument(
                                service.NotificationService.ROLE_STUDENT, studentId,
                                service.NotificationService.ATTENDANCE_ABSENT,
                                "Attendance Marked — " + ("LEAVE".equalsIgnoreCase(status) ? "On Leave" : "Absent"),
                                String.format("You were marked %s for '%s' (%s) on %s. " +
                                    "If this is incorrect, please contact administration.",
                                    label, batchName, subjectName, dateLabel))
                                .studentId(studentId).studentName(studentName)
                                .batchId(String.valueOf(capturedBatchId)).subject(subjectName));

                            if (parentId != null) {
                                ns.push(new service.NotificationDocument(
                                    service.NotificationService.ROLE_PARENT, parentId,
                                    service.NotificationService.ATTENDANCE_ABSENT,
                                    "Attendance Alert — " + ("LEAVE".equalsIgnoreCase(status) ? "On Leave" : "Absent"),
                                    String.format("Your ward %s was marked %s for '%s' (%s) on %s. " +
                                        "If you have any concern, please contact MRK Tuition.",
                                        studentName, label, batchName, subjectName, dateLabel))
                                    .studentId(studentId).studentName(studentName)
                                    .batchId(String.valueOf(capturedBatchId)).subject(subjectName));
                            }
                        }

                        // Low attendance check (fires separately)
                        double percent = getAttendancePercentage(studentId);
                        if (percent < 75.0) {
                            ns.notifyLowAttendance(
                                parentId != null ? parentId : "",
                                studentId, studentName, percent);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("[AttendanceDAO] Notification error: " + ex.getMessage());
                }
            }).start();
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Teacher attendance ──────────────────────────────────────────────────────

    /**
     * Returns the attendance status for a teacher on a given date.
     * Falls back to "Present" if no record exists.
     */
    public String getTeacherAttendanceStatus(String teacherId, String dateStr) {
        if (attendanceCollection == null) return "Present";
        try {
            Document doc = attendanceCollection.find(
                Filters.and(
                    Filters.eq("user_id",  teacherId),
                    Filters.eq("date_str", dateStr),
                    Filters.eq("type",     "TEACHER")
                )
            ).first();
            if (doc != null && doc.getString("status") != null) return doc.getString("status");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Present";
    }

    /**
     * Upserts a teacher attendance record identified by user_id + date_str + type=TEACHER.
     */
    public boolean saveTeacherAttendance(String teacherId, String status, String dateStr) {
        if (attendanceCollection == null) return false;
        try {
            Document doc = new Document()
                .append("user_id",         teacherId)
                .append("teacher_id",      teacherId)
                .append("status",          status)
                .append("date_str",        dateStr)
                .append("type",            "TEACHER")
                .append("marked_by",       "ADMIN")
                .append("attendance_date", parseDateOrToday(dateStr))
                .append("date",            parseDateOrToday(dateStr));
            attendanceCollection.replaceOne(
                Filters.and(
                    Filters.eq("user_id",  teacherId),
                    Filters.eq("date_str", dateStr),
                    Filters.eq("type",     "TEACHER")
                ),
                doc,
                new ReplaceOptions().upsert(true)
            );

            // 🔥 TRIGGER SALARY UPDATE
            try {
                String[] parts = dateStr.split("-");
                if (parts.length == 3) {
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    new service.SalaryService().calculateSalary(teacherId, month, year);
                }
            } catch (Exception ex) {
                System.err.println("[AttendanceDAO] Trigger Error: " + ex.getMessage());
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String normalizeStatus(String status) {
        if (status == null) return "PRESENT";
        return status.trim().toUpperCase();
    }

    private java.util.Date parseDateOrToday(String dateStr) {
        try {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
            fmt.setLenient(false);
            return fmt.parse(dateStr);
        } catch (Exception ignored) {
            return new java.util.Date();
        }
    }

    private org.bson.conversions.Bson batchIdFilter(int batchId) {
        return Filters.or(
            Filters.eq("batch_id", batchId),
            Filters.eq("batch_id", String.valueOf(batchId)),
            Filters.eq("batch_id", String.format("B%03d", batchId))
        );
    }

    private org.bson.conversions.Bson batchDocIdFilter(int batchId) {
        return Filters.or(
            Filters.eq("_id", batchId),
            Filters.eq("_id", String.valueOf(batchId)),
            Filters.eq("_id", String.format("B%03d", batchId))
        );
    }
}
