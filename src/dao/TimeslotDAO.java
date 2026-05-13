package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;

import db.DBConnection;
import model.Timeslot;

/**
 * DAO for the 'timeslots' collection.
 * Seeds the 8 fixed slots on first access if the collection is empty.
 */
public class TimeslotDAO {

    private static final List<Timeslot> DEFAULT_SLOTS = new ArrayList<>();

    static {
        DEFAULT_SLOTS.add(new Timeslot("TS1", "06:00 \u2013 07:30",  6,  0,  7, 30, 90));
        DEFAULT_SLOTS.add(new Timeslot("TS2", "06:30 \u2013 08:00",  6, 30,  8,  0, 90));
        DEFAULT_SLOTS.add(new Timeslot("TS3", "08:30 \u2013 10:30",  8, 30, 10, 30, 120));
        DEFAULT_SLOTS.add(new Timeslot("TS4", "10:00 \u2013 12:00", 10,  0, 12,  0, 120));
        DEFAULT_SLOTS.add(new Timeslot("TS5", "13:00 \u2013 15:00", 13,  0, 15,  0, 120));
        DEFAULT_SLOTS.add(new Timeslot("TS6", "16:00 \u2013 17:30", 16,  0, 17, 30, 90));
        DEFAULT_SLOTS.add(new Timeslot("TS7", "18:00 \u2013 19:30", 18,  0, 19, 30, 90));
        DEFAULT_SLOTS.add(new Timeslot("TS8", "19:30 \u2013 21:00", 19, 30, 21,  0, 90));
    }

    private MongoCollection<Document> collection;

    public TimeslotDAO() {
        MongoDatabase db = DBConnection.getDatabase();
        if (db != null) {
            collection = db.getCollection("timeslots");
            ensureSeeded();
        }
    }

    /** Insert the 8 default timeslots if the collection is empty. */
    private void ensureSeeded() {
        try {
            if (collection.countDocuments() == 0) {
                for (Timeslot ts : DEFAULT_SLOTS) {
                    collection.insertOne(timeslotToDocument(ts));
                }
                System.out.println("[TimeslotDAO] Seeded 8 default timeslots.");
            }
        } catch (Exception e) {
            System.err.println("[TimeslotDAO] Seed error: " + e.getMessage());
        }
    }

    /** Return all timeslots ordered by startHour + startMin ascending. */
    public List<Timeslot> findAllOrderedByStart() {
        List<Timeslot> list = new ArrayList<>();
        if (collection == null) {
            return DEFAULT_SLOTS; // fallback to in-memory defaults
        }
        try {
            for (Document doc : collection.find().sort(
                    Sorts.ascending("startHour", "startMin"))) {
                Timeslot ts = documentToTimeslot(doc);
                if (ts != null) list.add(ts);
            }
        } catch (Exception e) {
            System.err.println("[TimeslotDAO] findAllOrderedByStart error: " + e.getMessage());
            return DEFAULT_SLOTS;
        }
        if (list.isEmpty()) return DEFAULT_SLOTS;
        return list;
    }

    /** Find a single timeslot by its ID (e.g. "TS1"). */
    public Timeslot findById(String id) {
        if (collection == null || id == null) {
            return DEFAULT_SLOTS.stream().filter(ts -> ts.getId().equals(id)).findFirst().orElse(null);
        }
        try {
            Document doc = collection.find(new Document("_id", id)).first();
            return documentToTimeslot(doc);
        } catch (Exception e) {
            return null;
        }
    }

    /** Upsert a timeslot (used during seed / admin update). */
    public void upsert(Timeslot ts) {
        if (collection == null || ts == null) return;
        try {
            collection.updateOne(
                new Document("_id", ts.getId()),
                new Document("$set", timeslotToDocument(ts)),
                new UpdateOptions().upsert(true)
            );
        } catch (Exception e) {
            System.err.println("[TimeslotDAO] upsert error: " + e.getMessage());
        }
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    public static Timeslot documentToTimeslot(Document doc) {
        if (doc == null) return null;
        Timeslot ts = new Timeslot();
        ts.setId(doc.getString("_id"));
        ts.setLabel(doc.getString("label"));
        Object sh = doc.get("startHour"); if (sh instanceof Number) ts.setStartHour(((Number)sh).intValue());
        Object sm = doc.get("startMin");  if (sm instanceof Number) ts.setStartMin(((Number)sm).intValue());
        Object eh = doc.get("endHour");   if (eh instanceof Number) ts.setEndHour(((Number)eh).intValue());
        Object em = doc.get("endMin");    if (em instanceof Number) ts.setEndMin(((Number)em).intValue());
        Object dur = doc.get("durationMins"); if (dur instanceof Number) ts.setDurationMins(((Number)dur).intValue());
        return ts;
    }

    public static Document timeslotToDocument(Timeslot ts) {
        Document doc = new Document();
        doc.append("_id",          ts.getId());
        doc.append("label",        ts.getLabel());
        doc.append("startHour",    ts.getStartHour());
        doc.append("startMin",     ts.getStartMin());
        doc.append("endHour",      ts.getEndHour());
        doc.append("endMin",       ts.getEndMin());
        doc.append("durationMins", ts.getDurationMins());
        return doc;
    }
}
