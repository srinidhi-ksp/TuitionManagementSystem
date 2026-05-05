package service;

import java.util.ArrayList;
import java.util.List;

import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.SubjectDAO;
import model.Batch;
import model.Enrollment;
import model.StudentDashboard;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;
import org.bson.Document;
import java.util.ArrayList;

public class StudentService {

    private EnrollmentDAO enrollmentDAO;
    private BatchDAO batchDAO;
    private SubjectDAO subjectDAO;
    private MongoCollection<Document> studentCol;
    private MongoCollection<Document> enrollCol;
    private MongoCollection<Document> batchCol;
    private MongoCollection<Document> paymentCol;

    public StudentService() {
        this.enrollmentDAO = new EnrollmentDAO();
        this.batchDAO = new BatchDAO();
        this.subjectDAO = new SubjectDAO();
        
        MongoDatabase database = db.DBConnection.getDatabase();
        this.studentCol = database.getCollection("students");
        this.enrollCol = database.getCollection("enrollments");
        this.batchCol = database.getCollection("batches");
        this.paymentCol = database.getCollection("payments");
    }

    private dao.StudentDAO studentDAO = new dao.StudentDAO();

    /**
     * CRITICAL: Resolves student ID from User ID if necessary
     * user_id (e.g., U21) → student_id (e.g., S001)
     */
    private String resolveStudentId(String id) {
        if (id == null) {
            System.err.println("[StudentService] ❌ resolveStudentId: Input ID is NULL");
            return null;
        }
        
        if (id.startsWith("S")) {
            System.out.println("[StudentService] ID already student_id: " + id);
            return id; // Already a student ID
        }
        
        System.out.println("[StudentService] 🔄 Resolving user_id -> student_id for: " + id);
        
        // Try to find student by user_id
        model.Student s = studentDAO.getStudentByUserId(id);
        if (s == null) {
            System.err.println("[StudentService] ❌ Failed to map user_id " + id + " to student");
            return id; // Return as-is, let DAO handle it
        }
        
        String studentId = s.getUserId(); // This is student._id (e.g., S001)
        System.out.println("[StudentService] ✅ Mapped " + id + " → " + studentId);
        
        return studentId;
    }

    /**
     * Common method to get all active enrollments for a student
     */
    public List<Enrollment> getActiveEnrollments(String inputId) {
        String studentId = resolveStudentId(inputId);
        System.out.println("[StudentService] Fetching enrollments for: " + studentId);
        List<Enrollment> enrollments = enrollmentDAO.getEnrollmentsByStudentId(studentId);
        System.out.println("[StudentService] Enrollments found: " + (enrollments != null ? enrollments.size() : 0));
        return enrollments;
    }

    /**
     * Get batches for active enrollments
     */
    public List<Batch> getActiveBatches(String inputId) {
        String studentId = resolveStudentId(inputId);
        List<Enrollment> enrollments = getActiveEnrollments(studentId);
        List<Batch> batches = new ArrayList<>();
        
        if (enrollments != null) {
            for (Enrollment e : enrollments) {
                Batch b = batchDAO.getBatchById(e.getBatchId());
                if (b != null) {
                    batches.add(b);
                }
            }
        }
        return batches;
    }
    /**
     * EXACT Step 4: Get student dashboard data
     */
    public StudentDashboard getDashboard(String inputId) {
        String studentId = resolveStudentId(inputId);
        StudentDashboard dash = new StudentDashboard();

        Document student = studentCol.find(eq("_id", studentId)).first();
        if (student == null) return dash;

        dash.setName(student.getString("name"));
        if (dash.getName() == null) dash.setName(student.getString("full_name"));

        List<Document> enrollments = enrollCol.find(eq("student_id", studentId)).into(new ArrayList<>());
        dash.setBatchCount(enrollments.size());

        double totalFees = 0;
        double paid = 0;

        for (Document e : enrollments) {
            Object bIdObj = e.get("batch_id");
            String batchId = bIdObj != null ? bIdObj.toString() : null;
            if (batchId == null) continue;

            Document batch = batchCol.find(or(eq("_id", batchId), eq("_id", Integer.parseInt(batchId.matches("\\d+") ? batchId : "0")))).first();

            if (batch != null) {
                Object fee = batch.get("monthly_fee");
                if (fee instanceof Number) totalFees += ((Number) fee).doubleValue();
            }

            Document payment = paymentCol.find(and(
                eq("student_id", studentId),
                eq("batch_id", batchId.matches("\\d+") ? Integer.parseInt(batchId) : batchId)
            )).first();

            if (payment != null) {
                Object amt = payment.get("amount");
                if (amt == null) amt = payment.get("amount_paid");
                if (amt instanceof Number) paid += ((Number) amt).doubleValue();
            }
        }

        dash.setTotalFees(totalFees);
        dash.setPaidAmount(paid);
        dash.setPending(totalFees - paid);

        return dash;
    }
}