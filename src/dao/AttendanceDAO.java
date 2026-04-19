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
                    Filters.eq("batch_id", batchId),
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
            attendanceCollection.replaceOne(
                Filters.and(
                    Filters.eq("user_id",  att.getUserId()),
                    Filters.eq("batch_id", batchId),
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
}
