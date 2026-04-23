package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import db.DBConnection;
import db.DocumentMapper;
import model.Enrollment;

public class EnrollmentDAO {
    private MongoCollection<Document> enrollmentCollection;

    public EnrollmentDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            enrollmentCollection = database.getCollection("enrollments");
        }
    }

    public boolean addEnrollment(Enrollment enrollment) {
        if (enrollmentCollection == null) return false;
        try {
            Document doc = DocumentMapper.enrollmentToDocument(enrollment);
            enrollmentCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Enrollment getEnrollmentById(int enrollmentId) {
        if (enrollmentCollection == null) return null;
        try {
            Document doc = enrollmentCollection.find(Filters.eq("_id", enrollmentId)).first();
            return DocumentMapper.documentToEnrollment(doc);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public List<Enrollment> getAllEnrollments() {
        List<Enrollment> list = new ArrayList<>();
        if (enrollmentCollection == null) return list;

        try (MongoCursor<Document> cursor = enrollmentCollection.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Enrollment e = DocumentMapper.documentToEnrollment(doc);
                if (e != null) {
                    list.add(e);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return list;
    }

    // ✅ NEW METHOD: Get active enrollments by student ID (for fee calculation)
    public List<Enrollment> getEnrollmentsByStudentId(String studentId) {
        List<Enrollment> enrollments = new ArrayList<>();
        if (enrollmentCollection == null) return enrollments;

        try {
            String tid = studentId != null ? studentId.trim() : "";
            System.out.println("[EnrollmentDAO] Querying enrollments for ID: '" + tid + "'");
            
            // Query: (student_user_id OR student_id) AND status = ACTIVE
            MongoCursor<Document> cursor = enrollmentCollection.find(
                Filters.and(
                    Filters.or(
                        Filters.eq("student_user_id", tid),
                        Filters.eq("student_id", tid)
                    ),
                    Filters.eq("status", "ACTIVE")
                )
            ).iterator();

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Enrollment enrollment = DocumentMapper.documentToEnrollment(doc);
                if (enrollment != null) {
                    enrollments.add(enrollment);
                    System.out.println("[EnrollmentDAO]   ✔ Match Found: Enrollment #" + enrollment.getEnrollmentId());
                }
            }
            cursor.close();
            
            if (enrollments.isEmpty()) {
                System.out.println("[EnrollmentDAO] ⚠️  No ACTIVE enrollments found for student ID: " + tid);
            } else {
                System.out.println("[EnrollmentDAO] ✅ Total Enrollments Found: " + enrollments.size());
            }

        } catch (Exception e) {
            System.err.println("[EnrollmentDAO] ❌ Error in getEnrollmentsByStudentId: " + e.getMessage());
            e.printStackTrace();
        }
        return enrollments;
    }

    public List<model.Batch> getBatchesByStudentId(String studentId) {
        List<model.Batch> batches = new ArrayList<>();
        if (enrollmentCollection == null) return batches;
        
        BatchDAO batchDao = new BatchDAO();
        try (MongoCursor<Document> cursor = enrollmentCollection.find(
                Filters.or(
                    Filters.eq("student_user_id", studentId),
                    Filters.eq("student_id", studentId),
                    Filters.eq("user_id", studentId)
                )
            ).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Enrollment e = DocumentMapper.documentToEnrollment(doc);
                if (e != null) {
                    model.Batch b = batchDao.getBatchById(e.getBatchId());
                    if (b != null) {
                        batches.add(b);
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return batches;
    }

    public List<String> getStudentIdsByBatchId(int batchId) {
        List<String> ids = new ArrayList<>();
        if (enrollmentCollection == null) return ids;
        try (MongoCursor<Document> cursor = enrollmentCollection.find(Filters.eq("batch_id", batchId)).iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String sId = doc.getString("student_user_id");
                if (sId != null) ids.add(sId);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return ids;
    }

    public boolean deleteEnrollment(int enrollmentId) {
        if (enrollmentCollection == null) return false;
        try {
            long deletedCount = enrollmentCollection.deleteOne(Filters.eq("_id", enrollmentId)).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean updateEnrollment(Enrollment enrollment) {
        if (enrollmentCollection == null) return false;
        try {
            Document doc = DocumentMapper.enrollmentToDocument(enrollment);
            long matched = enrollmentCollection.replaceOne(Filters.eq("_id", enrollment.getEnrollmentId()), doc).getMatchedCount();
            return matched > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
