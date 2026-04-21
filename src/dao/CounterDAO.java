package dao;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import db.DBConnection;

/**
 * CounterDAO — Manages sequential ID generation via a MongoDB 'counters' collection.
 * Each entity type (student, teacher, batch) has its own counter document:
 *   { _id: "student_seq", seq: 5 }
 *
 * Uses findOneAndUpdate with $inc for atomic, thread-safe increments.
 * On first use, the counter is seeded from the highest existing ID in that collection.
 */
public class CounterDAO {

    private static final String COLLECTION   = "counters";
    private static final String STUDENT_KEY  = "student_seq";
    private static final String TEACHER_KEY  = "teacher_seq";
    private static final String BATCH_KEY    = "batch_seq";

    private MongoCollection<Document> counters;
    private MongoDatabase database;

    public CounterDAO() {
        database = DBConnection.getDatabase();
        if (database != null) {
            counters = database.getCollection(COLLECTION);
        }
    }

    // ── Public ID generators ────────────────────────────────────────────────

    /** Returns the next sequential student ID, e.g. "S001", "S002". */
    public String getNextStudentId() {
        int seq = nextSeq(STUDENT_KEY, "students", "S");
        return String.format("S%03d", seq);
    }

    /** Returns the next sequential teacher ID, e.g. "T001", "T002". */
    public String getNextTeacherId() {
        int seq = nextSeq(TEACHER_KEY, "teachers", "T");
        return String.format("T%03d", seq);
    }

    /** Returns the next sequential batch ID as an integer. */
    public int getNextBatchId() {
        return nextSeq(BATCH_KEY, "batches", "B");
    }

    // ── Core logic ──────────────────────────────────────────────────────────

    /**
     * Atomically increments and returns the next sequence number for the given key.
     * If the counter document does not exist yet, it is seeded from the max existing
     * ID found in the corresponding MongoDB collection.
     */
    private int nextSeq(String counterKey, String collectionName, String prefix) {
        if (counters == null) return (int)(System.currentTimeMillis() % 10000);

        try {
            // Ensure counter document exists — seed from current max if absent
            ensureCounter(counterKey, collectionName, prefix);

            // Atomically increment and return the new value
            FindOneAndUpdateOptions opts = new FindOneAndUpdateOptions()
                .returnDocument(ReturnDocument.AFTER)
                .upsert(true);

            Document result = counters.findOneAndUpdate(
                Filters.eq("_id", counterKey),
                Updates.inc("seq", 1),
                opts
            );

            if (result != null) {
                Object seqObj = result.get("seq");
                if (seqObj instanceof Number) return ((Number) seqObj).intValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Fallback to timestamp-based ID to avoid crash
        return (int)(System.currentTimeMillis() % 10000);
    }

    /**
     * If no counter document exists for this key, scan the source collection to find
     * the current maximum sequence number and seed the counter from there.
     */
    private void ensureCounter(String counterKey, String collectionName, String prefix) {
        try {
            Document existing = counters.find(Filters.eq("_id", counterKey)).first();
            if (existing != null) return; // already seeded

            // Scan all _id values in the source collection that match the prefix
            int maxSeq = 0;
            MongoCollection<Document> src = database.getCollection(collectionName);
            try (com.mongodb.client.MongoCursor<Document> cur = src.find().iterator()) {
                while (cur.hasNext()) {
                    Document doc = cur.next();
                    Object idObj = doc.get("_id");
                    if (idObj == null) continue;
                    String idStr = idObj.toString().trim().toUpperCase();
                    if (idStr.startsWith(prefix) && idStr.length() > 1) {
                        try {
                            int num = Integer.parseInt(idStr.substring(1));
                            if (num > maxSeq) maxSeq = num;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            // Insert seed document (seq = maxSeq so next call returns maxSeq+1)
            counters.insertOne(new Document("_id", counterKey).append("seq", maxSeq));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
