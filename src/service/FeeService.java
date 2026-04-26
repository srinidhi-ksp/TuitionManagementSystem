package service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.PaymentDAO;
import dao.SubjectDAO;
import model.Batch;
import model.Enrollment;
import model.Payment;
import model.Subject;
import model.SubjectFeeDTO;

/**
 * Fee Service - Core business logic for fee management
 * Handles fee calculations, payment status, and subject enrollment logic
 */
public class FeeService {

    private EnrollmentDAO enrollmentDAO;
    private BatchDAO batchDAO;
    private SubjectDAO subjectDAO;
    private PaymentDAO paymentDAO;

    public FeeService() {
        this.enrollmentDAO = new EnrollmentDAO();
        this.batchDAO = new BatchDAO();
        this.subjectDAO = new SubjectDAO();
        this.paymentDAO = new PaymentDAO();
    }

    private dao.StudentDAO studentDAO = new dao.StudentDAO();

    /**
     * CRITICAL: Resolves student ID from User ID if necessary
     * user_id (e.g., U21) → student_id (e.g., S001)
     */
    private String resolveStudentId(String id) {
        if (id == null) {
            System.err.println("[FeeService] ❌ resolveStudentId: Input ID is NULL");
            return null;
        }
        
        if (id.startsWith("S")) {
            System.out.println("[FeeService] ID already student_id: " + id);
            return id;
        }
        
        System.out.println("[FeeService] 🔄 Resolving user_id -> student_id for: " + id);
        
        model.Student s = studentDAO.getStudentByUserId(id);
        if (s == null) {
            System.err.println("[FeeService] ❌ Failed to map user_id " + id + " to student");
            return id;
        }
        
        String studentId = s.getUserId(); // This is student._id
        System.out.println("[FeeService] ✅ Mapped " + id + " → " + studentId);
        
        return studentId;
    }

    /**
     * Get detailed fee information for a student
     */
    public List<SubjectFeeDTO> getStudentFeeDetails(String inputId) {
        String studentId = resolveStudentId(inputId);
        System.out.println("\n[FeeService] Fetching fee details for student: " + studentId);
        
        List<SubjectFeeDTO> feeDetails = new ArrayList<>();
        
        try {
            // STEP 1: Fetch ALL active enrollments
            List<Enrollment> enrollments = enrollmentDAO.getEnrollmentsByStudentId(studentId);
            System.out.println("[FeeService] DEBUG - Enrollments found: " + enrollments.size());

            if (enrollments.isEmpty()) {
                System.out.println("[FeeService] ⚠️  No enrollments for student: " + studentId);
                return feeDetails;
            }

            // STEP 2: Collect Batch IDs
            List<Integer> batchIds = new ArrayList<>();
            for (Enrollment e : enrollments) {
                batchIds.add(e.getBatchId());
            }
            System.out.println("[FeeService] DEBUG - Batch IDs: " + batchIds);

            // STEP 3 & 4: Process each enrollment (allowing duplicates of same subject in different batches)
            for (Enrollment enrollment : enrollments) {
                Batch batch = batchDAO.getBatchById(enrollment.getBatchId());
                
                if (batch != null) {
                    Subject subject = subjectDAO.getSubjectById(batch.getSubjectId());
                    
                    if (subject != null) {
                        // Check payment status in payments collection
                        boolean isPaid = paymentDAO.isSubjectPaid(studentId, String.valueOf(subject.getSubjectId()));
                        String status = isPaid ? "PAID" : "UNPAID";
                        
                        // Use "Subject Name (Batch Name)" for clear identification
                        String displayName = subject.getSubjectName() + " (" + batch.getBatchName() + ")";
                        
                        SubjectFeeDTO dto = new SubjectFeeDTO(
                            String.valueOf(subject.getSubjectId()), 
                            displayName, 
                            subject.getMonthlyFee(), 
                            status,
                            batch.getBatchId()
                        );
                        feeDetails.add(dto);
                        
                        System.out.println("[FeeService]   -> Added: " + displayName + " | Status: " + status);
                    }
                }
            }

            System.out.println("[FeeService] ✅ Final fee details size: " + feeDetails.size());
            
        } catch (Exception e) {
            System.err.println("[FeeService] ❌ Error fetching fee details: " + e.getMessage());
            e.printStackTrace();
        }

        return feeDetails;
    }

    /**
     * Calculate fee summary for a student
     */
    public Map<String, Object> getFeeSummary(String inputId) {
        String studentId = resolveStudentId(inputId);
        Map<String, Object> summary = new HashMap<>();
        
        List<SubjectFeeDTO> feeDetails = getStudentFeeDetails(studentId);
        
        if (feeDetails.isEmpty()) {
            summary.put("totalFee", 0.0);
            summary.put("paidAmount", 0.0);
            summary.put("pendingAmount", 0.0);
            summary.put("status", "NO_ENROLLMENT");
            return summary;
        }

        // STEP 7: Calculate totals
        double totalFee = 0;
        double paidAmount = 0;
        int paidSubjects = 0;

        for (SubjectFeeDTO fee : feeDetails) {
            totalFee += fee.getMonthlyFee();
            if ("PAID".equals(fee.getPaymentStatus())) {
                paidAmount += fee.getMonthlyFee();
                paidSubjects++;
            }
        }

        double pendingAmount = totalFee - paidAmount;

        // STEP 8: Determine overall status
        String overallStatus;
        if (paidSubjects == feeDetails.size()) {
            overallStatus = "PAID";
        } else if (paidSubjects > 0) {
            overallStatus = "PARTIAL";
        } else {
            overallStatus = "UNPAID";
        }

        summary.put("totalFee", totalFee);
        summary.put("paidAmount", paidAmount);
        summary.put("pendingAmount", pendingAmount);
        summary.put("status", overallStatus);
        summary.put("totalSubjects", feeDetails.size());
        summary.put("paidSubjects", paidSubjects);

        System.out.println("[FeeService] Summary - Total: Rs. " + totalFee + 
                         " | Paid: Rs. " + paidAmount + " | Pending: Rs. " + pendingAmount + 
                         " | Status: " + overallStatus);

        return summary;
    }

    /**
     * Record a payment for a subject
     */
    public boolean recordPayment(String inputId, String subjectId, String paymentMode) {
        String studentId = resolveStudentId(inputId);
        System.out.println("\n[FeeService] Recording payment - Student: " + studentId + 
                         " | Subject: " + subjectId + " | Mode: " + paymentMode);
        
        try {
            // Get subject fee
            int subjectIdInt = Integer.parseInt(subjectId);
            Subject subject = subjectDAO.getSubjectById(subjectIdInt);
            if (subject == null) {
                System.err.println("[FeeService] Subject not found: " + subjectId);
                return false;
            }

            // Create payment record
            Payment payment = new Payment();
            payment.setStudentId(studentId);
            payment.setSubjectId(subjectId);
            payment.setAmountPaid(subject.getMonthlyFee());
            payment.setPaymentMode(paymentMode);
            payment.setPaymentDate(new Date());
            payment.setMonth(Calendar.getInstance().get(Calendar.MONTH) + 1);

            // Insert into database
            boolean success = paymentDAO.insertPayment(payment);
            
            if (success) {
                System.out.println("[FeeService] ✅ Payment recorded successfully!");
                System.out.println("[FeeService]   Amount: Rs. " + subject.getMonthlyFee());
            } else {
                System.err.println("[FeeService] ❌ Failed to record payment");
            }

            return success;

        } catch (Exception e) {
            System.err.println("[FeeService] ❌ Error recording payment: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get all students with their fee status (for admin view)
     */
    public List<Map<String, Object>> getAllStudentsFeeStatus(List<String> studentIds) {
        List<Map<String, Object>> allStudentsFees = new ArrayList<>();

        for (String studentId : studentIds) {
            Map<String, Object> studentFeeStatus = new HashMap<>();
            studentFeeStatus.put("studentId", studentId);
            studentFeeStatus.put("summary", getFeeSummary(studentId));
            studentFeeStatus.put("details", getStudentFeeDetails(studentId));
            
            allStudentsFees.add(studentFeeStatus);
        }

        return allStudentsFees;
    }
    /**
     * Generate a receipt object for a paid subject.
     * Maps Student ID, Batch, Subject, and Payment details.
     */
    public model.Receipt generateReceipt(String inputId, String subjectId) {
        String studentId = resolveStudentId(inputId);
        System.out.println("[FeeService] Generating receipt for Student: " + studentId + ", Subject: " + subjectId);
        
        try {
            // 1. Get Student Details
            model.Student student = studentDAO.getStudentById(studentId);
            if (student == null) student = studentDAO.getStudentByUserId(studentId);
            
            String studentName = (student != null) ? student.getName() : "N/A";
            String className = (student != null) ? student.getCurrentStd() : "N/A";

            // 2. Get Payment Details
            model.Payment payment = paymentDAO.getPayment(studentId, subjectId);
            if (payment == null) {
                System.err.println("[FeeService] ❌ No payment found for Student: " + studentId + ", Subject: " + subjectId);
                return null;
            }

            // 3. Get Subject Details
            model.Subject subject = subjectDAO.getSubjectById(Integer.parseInt(subjectId));
            String subjectName = (subject != null) ? subject.getSubjectName() : "N/A";
            double amount = (subject != null) ? subject.getMonthlyFee() : payment.getAmountPaid();

            // 4. Get Batch Details
            String batchName = "N/A";
            List<model.Enrollment> enrollments = enrollmentDAO.getEnrollmentsByStudentId(studentId);
            if (enrollments != null) {
                for (model.Enrollment e : enrollments) {
                    model.Batch b = batchDAO.getBatchById(e.getBatchId());
                    if (b != null && String.valueOf(b.getSubjectId()).equals(subjectId)) {
                        batchName = b.getBatchName();
                        break;
                    }
                }
            }

            // 5. Format Date
            String paymentDate = "N/A";
            if (payment.getPaymentDate() != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yyyy");
                paymentDate = sdf.format(payment.getPaymentDate());
            }

            return new model.Receipt(
                studentName,
                studentId,
                className,
                batchName,
                subjectName,
                amount,
                paymentDate,
                payment.getPaymentMode() != null ? payment.getPaymentMode() : "N/A"
            );

        } catch (Exception e) {
            System.err.println("[FeeService] ❌ Error generating receipt: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
