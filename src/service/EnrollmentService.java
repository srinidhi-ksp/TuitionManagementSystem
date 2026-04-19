package service;

import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.StudentDAO;
import model.Enrollment;

import java.util.List;

public class EnrollmentService {

    private EnrollmentDAO enrollmentDAO;
    private StudentDAO studentDAO;
    private BatchDAO batchDAO;

    public EnrollmentService() {
        enrollmentDAO = new EnrollmentDAO();
        studentDAO = new StudentDAO();
        batchDAO = new BatchDAO();
    }

    public boolean enrollStudent(Enrollment enrollment) {

        if (studentDAO.getStudentById(enrollment.getStudentUserId()) == null) {
            return false;
        }

        if (batchDAO.getBatchById(enrollment.getBatchId()) == null) {
            return false;
        }

        return enrollmentDAO.addEnrollment(enrollment);
    }

    public Enrollment getEnrollment(int enrollmentId) {
        return enrollmentDAO.getEnrollmentById(enrollmentId);
    }

    public List<Enrollment> getAllEnrollments() {
        return enrollmentDAO.getAllEnrollments();
    }

    public boolean deleteEnrollment(int enrollmentId) {
        return enrollmentDAO.deleteEnrollment(enrollmentId);
    }
}