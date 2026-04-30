package dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import db.DBConnection;
import db.DocumentMapper;
import model.Student;

/**
 * Fee Analytics DAO - Handles aggregation for fee-related analytics
 */
public class FeeAnalyticsDAO {

    private MongoCollection<Document> studentCollection;
    private MongoCollection<Document> paymentCollection;
    private MongoCollection<Document> enrollmentCollection;

    public FeeAnalyticsDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            this.studentCollection = database.getCollection("students");
            this.paymentCollection = database.getCollection("payments");
            this.enrollmentCollection = database.getCollection("enrollments");
        }
    }

    /**
     * Get all students with their consolidated fee status.
     * Logic: 
     * - Fetch all students.
     * - For each student, check their enrollments.
     * - Check if payments exist for those enrollments.
     * - If enrollments exist and all are paid -> PAID.
     * - If enrollments exist and some are unpaid -> UNPAID.
     * - If no enrollments -> NO_ENROLLMENT.
     */
    public List<Map<String, Object>> getAllStudentFeeStatus() {
        List<Map<String, Object>> report = new ArrayList<>();
        if (studentCollection == null) return report;

        List<Student> students = new StudentDAO().getAllStudents();
        PaymentDAO paymentDao = new PaymentDAO();
        BatchDAO batchDao = new BatchDAO();

        for (Student s : students) {
            Map<String, Object> data = new HashMap<>();
            data.put("student", s);
            data.put("name", s.getName());
            data.put("id", s.getUserId());
            data.put("standard", s.getCurrentStd());
            
            // Check enrollments
            List<Document> enrollments = new ArrayList<>();
            enrollmentCollection.find(Filters.or(
                Filters.eq("student_user_id", s.getUserId()),
                Filters.eq("student_id", s.getUserId())
            )).into(enrollments);

            if (enrollments.isEmpty()) {
                data.put("status", "NO_ENROLLMENT");
                data.put("batch", "--");
            } else {
                boolean allPaid = true;
                StringBuilder batches = new StringBuilder();
                for (Document enroll : enrollments) {
                    Integer batchId = enroll.getInteger("batch_id");
                    if (batchId != null) {
                        model.Batch b = batchDao.getBatchById(batchId);
                        if (b != null) {
                            if (batches.length() > 0) batches.append(", ");
                            batches.append(b.getBatchName());
                            
                            if (!paymentDao.isSubjectPaid(s.getUserId(), String.valueOf(b.getSubjectId()))) {
                                allPaid = false;
                            }
                        }
                    }
                }
                data.put("status", allPaid ? "PAID" : "UNPAID");
                data.put("batch", batches.toString());
            }
            report.add(data);
        }
        return report;
    }

    /**
     * Get counts for summary cards
     */
    public Map<String, Long> getFeeSummaryStats() {
        Map<String, Long> stats = new HashMap<>();
        List<Map<String, Object>> report = getAllStudentFeeStatus();
        
        long total = report.size();
        long paid = report.stream().filter(r -> "PAID".equals(r.get("status"))).count();
        long unpaid = report.stream().filter(r -> "UNPAID".equals(r.get("status"))).count();
        
        stats.put("total", total);
        stats.put("paid", paid);
        stats.put("unpaid", unpaid);
        
        return stats;
    }
}
