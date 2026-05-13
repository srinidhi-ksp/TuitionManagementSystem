package dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import db.DBConnection;
import model.TeacherExtraSlot;

/**
 * DAO for the 'teacher_extra_slots' collection.
 * Tracks every extra slot (substitution / additional batch) a teacher teaches.
 */
public class TeacherExtraSlotDAO {

    private MongoCollection<Document> collection;

    public TeacherExtraSlotDAO() {
        MongoDatabase db = DBConnection.getDatabase();
        if (db != null) {
            collection = db.getCollection("teacher_extra_slots");
        }
    }

    /** Insert a new extra slot record. */
    public boolean insert(TeacherExtraSlot slot) {
        if (collection == null || slot == null) return false;
        try {
            if (slot.getId() == null) {
                // Auto-generate a unique ID
                long count = collection.countDocuments(
                    Filters.and(
                        Filters.eq("teacher_id", slot.getTeacherId()),
                        Filters.eq("month", slot.getMonth()),
                        Filters.eq("year",  slot.getYear())
                    )
                );
                slot.setId("EXT_" + slot.getTeacherId() + "_" + slot.getYear()
                         + "_" + slot.getMonth() + "_" + String.format("%03d", count + 1));
            }
            if (slot.getRecordedAt() == null) slot.setRecordedAt(new Date());
            collection.insertOne(slotToDocument(slot));
            return true;
        } catch (Exception e) {
            System.err.println("[TeacherExtraSlotDAO] insert error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Count extra slots for a teacher in a given month/year.
     * Used by the salary engine to compute the bonus.
     */
    public long countByTeacherMonthYear(String teacherId, String month, String year) {
        if (collection == null) return 0;
        try {
            return collection.countDocuments(
                Filters.and(
                    Filters.eq("teacher_id", teacherId),
                    Filters.eq("month", month),
                    Filters.eq("year",  year)
                )
            );
        } catch (Exception e) {
            System.err.println("[TeacherExtraSlotDAO] countByTeacherMonthYear error: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Find all extra slots for a teacher in a given month/year.
     * Used by the LogExtraSlotDialog and for display purposes.
     */
    public List<TeacherExtraSlot> findByTeacherMonthYear(String teacherId, String month, String year) {
        List<TeacherExtraSlot> list = new ArrayList<>();
        if (collection == null) return list;
        try {
            for (Document doc : collection.find(
                    Filters.and(
                        Filters.eq("teacher_id", teacherId),
                        Filters.eq("month", month),
                        Filters.eq("year",  year)
                    )
            )) {
                TeacherExtraSlot s = documentToSlot(doc);
                if (s != null) list.add(s);
            }
        } catch (Exception e) {
            System.err.println("[TeacherExtraSlotDAO] findByTeacherMonthYear error: " + e.getMessage());
        }
        return list;
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private static TeacherExtraSlot documentToSlot(Document doc) {
        if (doc == null) return null;
        TeacherExtraSlot s = new TeacherExtraSlot();
        s.setId(doc.getString("_id"));
        s.setTeacherId(doc.getString("teacher_id"));
        s.setBatchId(doc.getString("batch_id"));
        s.setBatchName(doc.getString("batch_name"));
        s.setDay(doc.getString("day"));
        s.setTimeslotId(doc.getString("timeslot_id"));
        s.setDate(doc.getString("date"));
        s.setMonth(doc.getString("month"));
        s.setYear(doc.getString("year"));
        Object bonus = doc.get("bonus_amount");
        if (bonus instanceof Number) s.setBonusAmount(((Number)bonus).doubleValue());
        s.setRecordedAt(doc.getDate("recorded_at"));
        return s;
    }

    private static Document slotToDocument(TeacherExtraSlot s) {
        Document doc = new Document();
        doc.append("_id",          s.getId());
        doc.append("teacher_id",   s.getTeacherId());
        doc.append("batch_id",     s.getBatchId());
        doc.append("batch_name",   s.getBatchName());
        doc.append("day",          s.getDay());
        doc.append("timeslot_id",  s.getTimeslotId());
        doc.append("date",         s.getDate());
        doc.append("month",        s.getMonth());
        doc.append("year",         s.getYear());
        doc.append("bonus_amount", s.getBonusAmount());
        doc.append("recorded_at",  s.getRecordedAt() != null ? s.getRecordedAt() : new Date());
        return doc;
    }
}
