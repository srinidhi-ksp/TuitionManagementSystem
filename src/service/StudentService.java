package service;

import java.util.ArrayList;
import java.util.List;

import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.SubjectDAO;
import model.Batch;
import model.Enrollment;

public class StudentService {

    private EnrollmentDAO enrollmentDAO;
    private BatchDAO batchDAO;
    private SubjectDAO subjectDAO;

    public StudentService() {
        this.enrollmentDAO = new EnrollmentDAO();
        this.batchDAO = new BatchDAO();
        this.subjectDAO = new SubjectDAO();
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
}