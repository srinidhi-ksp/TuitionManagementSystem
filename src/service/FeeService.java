package service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dao.EnrollmentDAO;
import dao.BatchDAO;
import dao.SubjectDAO;
import dao.PaymentDAO;
import model.Enrollment;
import model.Batch;
import model.Subject;
import model.SubjectFeeDTO;

/**
 * FeeService - Core business logic for fee management
 * Implements the fee calculation and payment tracking logic
 */
public class FeeService {
    
    private EnrollmentDAO enrollmentDAO;
    private BatchDAO batchDAO;
    private SubjectDAO subjectDAO;
    private PaymentDAO paymentDAO;

    public FeeService() {
        enrollmentDAO = new EnrollmentDAO();
        batchDAO = new BatchDAO();
        subjectDAO = new SubjectDAO();
        paymentDAO = new PaymentDAO();
    }

    /**
     * Get all subject-wise fee details for a student
     * 
     * LOGIC:
     * 1. Fetch ACTIVE enrollments for student
     * 2. Extract unique subject_ids from batches
     * 3. For each subject: fetch details, check payment status
     * 4. Return list of SubjectFeeDTO
     */
    public List<SubjectFeeDTO> getStudentFeeDetails(String studentId) {
        List<SubjectFeeDTO> feeDetails = new ArrayList<>();
        
        if (studentId == null || studentId.isEmpty()) {
            return feeDetails;
        }

        try {
            // STEP 1: Fetch ACTIVE enrollments for this student
            List<Enrollment> enrollments = enrollmentDAO.getActiveEnrollmentsByStudentId(studentId);
            
            if (enrollments == null || enrollments.isEmpty()) {
                System.out.println("[FeeService] No active enrollments found for student: " + studentId);
                return feeDetails;
            }

            // STEP 2: Extract UNIQUE subject IDs from batches
            Set<Integer> uniqueSubjectIds = new HashSet<>();
            
            for (Enrollment enrollment : enrollments) {
                Batch batch = batchDAO.getBatchById(enrollment.getBatchId());
                if (batch != null) {
                    uniqueSubjectIds.add(batch.getSubjectId());
                }
            }

            if (uniqueSubjectIds.isEmpty()) {
                System.out.println("[FeeService] No subjects found for student: " + studentId);
                return feeDetails;
            }

            // STEP 3 & 4: For each unique subject, build DTO with payment status
            for (int subjectId : uniqueSubjectIds) {
                Subject subject = subjectDAO.getSubjectById(subjectId);
                
                if (subject != null) {
                    // Check if subject is paid
                    boolean isPaid = paymentDAO.isSubjectPaid(studentId, subjectId);
                    String paymentStatus = isPaid ? "PAID" : "UNPAID";
                    
                    // Build DTO
                    SubjectFeeDTO dto = new SubjectFeeDTO(
                        subject.getSubjectId(),
                        subject.getSubjectName(),
                        subject.getMonthlyFee(),
                        paymentStatus
                    );
                    
                    feeDetails.add(dto);
                    System.out.println("[FeeService] Subject Fee: " + dto);
                }
            }

        } catch (Exception e) {
            System.err.println("[FeeService] Error in getStudentFeeDetails: " + e.getMessage());
            e.printStackTrace();
        }

        return feeDetails;
    }

    /**
     * Calculate total fees for a student
     * Sums up all subject monthly fees
     */
    public double calculateTotalFee(String studentId) {
        List<SubjectFeeDTO> fees = getStudentFeeDetails(studentId);
        double total = 0;
        for (SubjectFeeDTO fee : fees) {
            total += fee.getMonthlyFee();
        }
        return total;
    }

    /**
     * Calculate paid amount for a student
     * Sums up fees of subjects marked as PAID
     */
    public double calculatePaidAmount(String studentId) {
        List<SubjectFeeDTO> fees = getStudentFeeDetails(studentId);
        double paid = 0;
        for (SubjectFeeDTO fee : fees) {
            if ("PAID".equalsIgnoreCase(fee.getPaymentStatus())) {
                paid += fee.getMonthlyFee();
            }
        }
        return paid;
    }

    /**
     * Calculate pending amount for a student
     * total_fee - paid_amount
     */
    public double calculatePendingAmount(String studentId) {
        double total = calculateTotalFee(studentId);
        double paid = calculatePaidAmount(studentId);
        return total - paid;
    }

    /**
     * Get overall payment status for a student
     * PAID: All subjects paid
     * PARTIAL: Some subjects paid
     * UNPAID: No subjects paid
     */
    public String getOverallPaymentStatus(String studentId) {
        List<SubjectFeeDTO> fees = getStudentFeeDetails(studentId);
        
        if (fees.isEmpty()) {
            return "NO_SUBJECTS";
        }

        int paidCount = 0;
        for (SubjectFeeDTO fee : fees) {
            if ("PAID".equalsIgnoreCase(fee.getPaymentStatus())) {
                paidCount++;
            }
        }

        if (paidCount == fees.size()) {
            return "PAID";
        } else if (paidCount > 0) {
            return "PARTIAL";
        } else {
            return "UNPAID";
        }
    }

    /**
     * Mark a subject as paid for a student
     * Creates a payment record
     */
    public boolean markSubjectAsPaid(String studentId, int subjectId, double amount, 
                                     String paymentMode, String month) {
        try {
            // Check if already paid
            if (paymentDAO.isSubjectPaid(studentId, subjectId)) {
                System.out.println("[FeeService] Subject already marked as paid for student: " + studentId);
                return false;
            }

            // Get subject to verify it exists
            Subject subject = subjectDAO.getSubjectById(subjectId);
            if (subject == null) {
                System.out.println("[FeeService] Subject not found: " + subjectId);
                return false;
            }

            // Create payment record
            model.Payment payment = new model.Payment();
            payment.setPaymentId(generatePaymentId()); // Generate ID
            payment.setStudentId(studentId);
            payment.setSubjectId(subjectId);
            payment.setAmountPaid(amount);
            payment.setPaymentMode(paymentMode);
            payment.setPaymentDate(new java.util.Date());
            payment.setMonth(month);

            return paymentDAO.insertPayment(payment);

        } catch (Exception e) {
            System.err.println("[FeeService] Error marking subject as paid: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Generate a unique payment ID
     * (In production, use MongoDB ObjectId or auto-increment)
     */
    private int generatePaymentId() {
        // Use timestamp as a simple ID generator
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }
}
