package service;

import java.util.ArrayList;
import java.util.List;
import dao.EnrollmentDAO;
import dao.BatchDAO;
import dao.SubjectDAO;
import model.Enrollment;
import model.Batch;
import model.Subject;

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
     * Resolves student ID from User ID if necessary
     */
    private String resolveStudentId(String id) {
        if (id == null) return null;
        if (id.startsWith("S")) return id; // Already a student ID
        
        System.out.println("User ID: " + id);
        // Try to find student by user_id
        model.Student s = studentDAO.getStudentByUserId(id);
        String studentId = (s != null) ? s.getUserId() : id;
        System.out.println("Mapped Student ID: " + studentId);
        
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