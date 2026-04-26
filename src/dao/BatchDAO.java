package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import model.Batch;
import model.Student;
import db.DBConnection;
import db.DocumentMapper;

public class BatchDAO {

    private MongoCollection<Document> batchCollection;

    public BatchDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            batchCollection = database.getCollection("batches");
        }
    }

    public boolean addBatch(Batch batch) {
        if (batchCollection == null) return false;
        try {
            Document doc = DocumentMapper.batchToDocument(batch);
            batchCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Batch getBatchById(int batchId) {
        if (batchCollection == null) return null;
        try {
            Document doc = batchCollection.find(Filters.eq("_id", batchId)).first();
            return DocumentMapper.documentToBatch(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Batch> getAllBatches() {
        List<Batch> list = new ArrayList<>();
        if (batchCollection == null) return list;

        try (MongoCursor<Document> cursor = batchCollection.find().sort(Sorts.ascending("standard", "batch_name")).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Batch b = DocumentMapper.documentToBatch(doc);
                if (b != null) {
                    list.add(b);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Batch> getBatchesByTeacherId(String teacherId) {
        List<Batch> list = new ArrayList<>();
        if (batchCollection == null) return list;

        try (MongoCursor<Document> cursor = batchCollection.find(Filters.eq("teacher_id", teacherId)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Batch b = DocumentMapper.documentToBatch(doc);
                if (b != null) {
                    list.add(b);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteBatch(int batchId) {
        if (batchCollection == null) return false;
        try {
            long deletedCount = batchCollection.deleteOne(Filters.eq("_id", batchId)).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateBatch(Batch batch) {
        if (batchCollection == null) return false;
        try {
            Document doc = DocumentMapper.batchToDocument(batch);
            long matched = batchCollection.replaceOne(Filters.eq("_id", batch.getBatchId()), doc).getMatchedCount();
            return matched > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Find batches by EXACT standard match.
     * standard = "8", "12", etc. — NOT "Class 8".
     * Uses Filters.eq (no regex, no contains).
     */
    public List<Batch> findByStandard(String standard) {
        List<Batch> list = new ArrayList<>();
        if (batchCollection == null || standard == null) return list;

        System.out.println("[BatchDAO] findByStandard: EXACT match on standard='" + standard + "'");

        try (MongoCursor<Document> cursor = batchCollection.find(
            Filters.and(
                Filters.eq("standard", standard),
                Filters.eq("status", "ACTIVE")
            )
        ).iterator()) {
            while (cursor.hasNext()) {
                Batch b = DocumentMapper.documentToBatch(cursor.next());
                if (b != null) list.add(b);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Fallback: if DB doesn't have 'standard' field yet, try matching derived from category
        if (list.isEmpty()) {
            System.out.println("[BatchDAO] No results on 'standard' field. Trying category fallback...");
            String catExact = "Class " + standard;
            try (MongoCursor<Document> cursor = batchCollection.find(
                Filters.and(
                    Filters.eq("category", catExact),
                    Filters.eq("status", "ACTIVE")
                )
            ).iterator()) {
                while (cursor.hasNext()) {
                    Batch b = DocumentMapper.documentToBatch(cursor.next());
                    if (b != null) list.add(b);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("[BatchDAO] Batches Found: " + list.size());
        return list;
    }

    /**
     * @deprecated Use findByStandard() instead for exact matching.
     */
    public List<Batch> getBatchesByCategory(String category) {
        // Delegate to findByStandard with extracted number
        String std = category;
        if (category != null && category.toLowerCase().startsWith("class ")) {
            std = category.substring(6).trim();
        }
        return findByStandard(std);
    }

    /**
     * Professional fix for Enrollment Module.
     * Maps User ID -> Student ID -> Class -> Filtered Batches
     */
    public List<Batch> getBatchesForStudent(String userId) {
        List<Batch> list = new ArrayList<>();
        if (userId == null) return list;

        try {
            // 1. Resolve Student
            StudentDAO studentDAO = new StudentDAO();
            Student student = studentDAO.getStudentById(userId);
            if (student == null) student = studentDAO.getStudentByUserId(userId);
            
            if (student == null) {
                System.err.println("[BatchDAO] No student found for ID: " + userId);
                return list;
            }

            // 2. Extract Class Number (e.g. "Class 10" -> "10")
            String studentClass = student.getCurrentStd();
            if (studentClass == null) return list;
            
            String classNumber = studentClass;
            if (studentClass.toLowerCase().startsWith("class ")) {
                classNumber = studentClass.substring(6).trim();
            }

            System.out.println("[BatchDAO] Filtering batches for Class: " + classNumber);

            // 3. Query using REGEX for flexibility (handles "Class 10-12")
            // We search in both 'category' and 'standard' fields
            try (MongoCursor<Document> cursor = batchCollection.find(
                Filters.and(
                    Filters.eq("status", "ACTIVE"),
                    Filters.or(
                        Filters.eq("standard", classNumber),
                        Filters.regex("category", ".*" + classNumber + ".*")
                    )
                )
            ).iterator()) {
                while (cursor.hasNext()) {
                    Batch b = DocumentMapper.documentToBatch(cursor.next());
                    if (b != null) list.add(b);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("[BatchDAO] Filtered Batches Found: " + list.size());
        return list;
    }
}
