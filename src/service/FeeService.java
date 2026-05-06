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
        System.out.println("\n[FeeService] getStudentFeeDetails() for: " + studentId);

        List<SubjectFeeDTO> feeDetails = new ArrayList<>();

        try {
            List<Enrollment> enrollments = enrollmentDAO.getEnrollmentsByStudentId(studentId);
            System.out.println("[FeeService] Enrollments found: " + enrollments.size());

            if (enrollments.isEmpty()) {
                System.out.println("[FeeService] No enrollments for: " + studentId);
                return feeDetails;
            }

            for (Enrollment enrollment : enrollments) {
                Batch batch = batchDAO.getBatchById(enrollment.getBatchId());
                if (batch == null) continue;

                Subject subject = subjectDAO.getSubjectById(batch.getSubjectId());
                if (subject == null) continue;

                double monthlyFee = subject.getMonthlyFee();

                // ── CORE FIX: Use getBatchPaymentSummary() directly (same as FeeAnalyticsDAO approach) ──
                java.util.Map<String, Object> paySummary =
                    paymentDAO.getBatchPaymentSummary(studentId, batch.getBatchId());

                double paidAmount   = (Double) paySummary.get("totalPaid");
                String rawStatus    = (String) paySummary.get("status");   // "SUCCESS", "REQUESTED", "UNPAID"
                String method       = (String) paySummary.get("method");

                double pendingAmount = monthlyFee - paidAmount;
                if (pendingAmount < 0) pendingAmount = 0;

                // Determine display status: PAID / PARTIAL / PENDING / UNPAID
                String displayStatus;
                String detailedStatus;

                if (paidAmount >= monthlyFee) {
                    displayStatus  = "PAID";
                    detailedStatus = "SUCCESS";
                } else if (paidAmount > 0 && pendingAmount > 0) {
                    displayStatus  = "PARTIAL";
                    detailedStatus = "PARTIAL";
                } else if ("REQUESTED".equalsIgnoreCase(rawStatus)) {
                    displayStatus  = "PENDING";
                    detailedStatus = "REQUESTED";
                } else {
                    displayStatus  = "UNPAID";
                    detailedStatus = "UNPAID";
                }

                String displayName = subject.getSubjectName() + " (" + batch.getBatchName() + ")";

                SubjectFeeDTO dto = new SubjectFeeDTO(
                    String.valueOf(subject.getSubjectId()),
                    displayName,
                    monthlyFee,
                    displayStatus,
                    batch.getBatchId(),
                    paidAmount,
                    pendingAmount,
                    method,
                    detailedStatus
                );
                feeDetails.add(dto);

                System.out.println("[FeeService] -> " + displayName
                    + " | monthlyFee=" + monthlyFee
                    + " | paid=" + paidAmount
                    + " | pending=" + pendingAmount
                    + " | status=" + displayStatus);
            }

        } catch (Exception e) {
            System.err.println("[FeeService] Error in getStudentFeeDetails: " + e.getMessage());
            e.printStackTrace();
        }

        return feeDetails;
    }

    /**
     * Calculate fee summary for a student
     */
    public Map<String, Object> getFeeSummary(String inputId) {
        if (inputId == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("totalFee", 0.0);
            empty.put("paidAmount", 0.0);
            empty.put("pendingAmount", 0.0);
            empty.put("status", "NO_ENROLLMENT");
            return empty;
        }

        String studentId = resolveStudentId(inputId);
        List<SubjectFeeDTO> feeDetails = getStudentFeeDetails(studentId);

        Map<String, Object> summary = new HashMap<>();

        if (feeDetails.isEmpty()) {
            summary.put("totalFee", 0.0);
            summary.put("paidAmount", 0.0);
            summary.put("pendingAmount", 0.0);
            summary.put("status", "NO_ENROLLMENT");
            return summary;
        }

        double totalFee     = 0;
        double paidAmount   = 0;
        int paidSubjects    = 0;
        int partialSubjects = 0;
        int pendingSubjects = 0; // REQUESTED / awaiting admin approval

        for (SubjectFeeDTO fee : feeDetails) {
            totalFee   += fee.getMonthlyFee();
            paidAmount += fee.getPaidAmount();

            String s = fee.getPaymentStatus();
            if ("PAID".equalsIgnoreCase(s))    paidSubjects++;
            if ("PARTIAL".equalsIgnoreCase(s)) partialSubjects++;
            if ("PENDING".equalsIgnoreCase(s)) pendingSubjects++;
        }

        double pendingAmount = totalFee - paidAmount;
        if (pendingAmount < 0) pendingAmount = 0;

        String overallStatus;
        if (pendingAmount == 0 && totalFee > 0) {
            overallStatus = "PAID";
        } else if (partialSubjects > 0) {
            overallStatus = "PARTIAL";
        } else if (pendingSubjects > 0) {
            overallStatus = "PENDING";
        } else {
            overallStatus = "UNPAID";
        }

        summary.put("totalFee",      totalFee);
        summary.put("paidAmount",    paidAmount);
        summary.put("pendingAmount", pendingAmount);
        summary.put("status",        overallStatus);
        summary.put("totalSubjects", feeDetails.size());
        summary.put("paidSubjects",  paidSubjects);

        System.out.println("[FeeService] Summary for " + studentId
            + " -> Total=" + totalFee + " Paid=" + paidAmount
            + " Pending=" + pendingAmount + " Status=" + overallStatus);

        return summary;
    }

    /**
     * Record a payment for a specific batch
     */
    public boolean recordPayment(String inputId, int batchId, String paymentMode) {
        String studentId = resolveStudentId(inputId);
        System.out.println("[FeeService] recordPayment() student=" + studentId + " batch=" + batchId);

        try {
            // PRE-CHECK: Is this batch already fully paid? (same logic as FeeAnalyticsDAO)
            java.util.Map<String, Object> existing = paymentDAO.getBatchPaymentSummary(studentId, batchId);
            String existingRawStatus = (String) existing.get("status");

            Batch batch = batchDAO.getBatchById(batchId);
            if (batch == null) { System.err.println("[FeeService] Batch not found: " + batchId); return false; }

            Subject subject = subjectDAO.getSubjectById(batch.getSubjectId());
            if (subject == null) { System.err.println("[FeeService] Subject not found for batch: " + batchId); return false; }

            double monthlyFee  = subject.getMonthlyFee();
            double alreadyPaid = (Double) existing.get("totalPaid");

            if (alreadyPaid >= monthlyFee) {
                System.out.println("[FeeService] ⚠️ Already fully paid for batch " + batchId + ". Aborting.");
                return false; // Caller must show "Fee already paid for this month"
            }

            if ("REQUESTED".equalsIgnoreCase(existingRawStatus)) {
                System.out.println("[FeeService] ⚠️ Payment already requested (pending admin approval) for batch " + batchId);
                return false; // Caller must show "Payment pending admin approval"
            }

            double pendingToPay = monthlyFee - alreadyPaid;

            // Create payment record
            Payment payment = new Payment();
            payment.setStudentId(studentId);
            payment.setBatchId(batchId);
            payment.setAmountPaid(pendingToPay);
            payment.setPaymentDate(new java.util.Date());

            String currentMonth = new java.text.SimpleDateFormat("yyyy-MM").format(new java.util.Date());
            payment.setMonthStr(currentMonth);

            if ("CASH".equalsIgnoreCase(paymentMode)) {
                payment.setStatus("REQUESTED");
                payment.setPaymentMode("CASH_REQUEST");
            } else {
                payment.setStatus("SUCCESS");
                payment.setPaymentMode(paymentMode);
            }

            boolean success = paymentDAO.insertPayment(payment);

            if (success) {
                System.out.println("[FeeService] ✅ Payment recorded for student=" + studentId + " batch=" + batchId);
            } else {
                System.err.println("[FeeService] ❌ insertPayment returned false for student=" + studentId + " batch=" + batchId);
            }
            return success;

        } catch (Exception e) {
            System.err.println("[FeeService] ❌ recordPayment error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
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
     * Generate a receipt string for a paid subject.
     * Maps Student ID, Batch, Subject, and Payment details.
     */
    public model.Receipt generateReceipt(String inputId, int batchId) {
        String studentId = resolveStudentId(inputId);
        System.out.println("[FeeService] Generating receipt for Student: " + studentId + ", Batch: " + batchId);
        
        try {
            // 1. Get Student Details
            model.Student student = studentDAO.getStudentById(studentId);
            if (student == null) student = studentDAO.getStudentByUserId(studentId);
            
            String studentName = (student != null) ? student.getName() : "-";
            String className = (student != null) ? student.getCurrentStd() : "-";

            // 2. Get Payment Details
            model.Payment payment = paymentDAO.getPayment(studentId, batchId);
            if (payment == null) {
                System.err.println("[FeeService] ❌ No payment found for Student: " + studentId + ", Batch: " + batchId);
                return null;
            }

            // 3. Get Batch and Subject Details
            model.Batch batch = batchDAO.getBatchById(batchId);
            String batchName = (batch != null) ? batch.getBatchName() : "-";
            
            String subjectName = "-";
            double amount = payment.getAmountPaid();
            
            if (batch != null) {
                model.Subject subject = subjectDAO.getSubjectById(batch.getSubjectId());
                if (subject != null) {
                    subjectName = subject.getSubjectName();
                    amount = subject.getMonthlyFee();
                }
            }

            // 4. Format Date
            String paymentDate = "-";
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
                payment.getPaymentMode() != null ? payment.getPaymentMode() : "-"
            );

        } catch (Exception e) {
            System.err.println("[FeeService] ❌ Error generating receipt: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    /**
     * Get all payments for a student (History)
     */
    public List<Map<String, Object>> getPaymentHistory(String inputId) {
        String studentId = resolveStudentId(inputId);
        List<Map<String, Object>> history = new ArrayList<>();
        
        try {
            List<Payment> payments = paymentDAO.getAllPaymentsForStudent(studentId);
            for (Payment p : payments) {
                Map<String, Object> row = new HashMap<>();
                
                Batch b = batchDAO.getBatchById(p.getBatchId());
                String batchName = (b != null) ? b.getBatchName() : "-";
                
                String subjectName = "-";
                if (b != null) {
                    Subject s = subjectDAO.getSubjectById(b.getSubjectId());
                    if (s != null) subjectName = s.getSubjectName();
                }
                
                row.put("subject", subjectName);
                row.put("batch", batchName);
                row.put("amount", p.getAmountPaid());
                row.put("method", p.getPaymentMode() != null ? p.getPaymentMode() : "-");
                row.put("status", p.getStatus() != null ? p.getStatus() : "-");
                
                String dateStr = "-";
                if (p.getPaymentDate() != null) {
                    dateStr = new java.text.SimpleDateFormat("dd-MM-yyyy").format(p.getPaymentDate());
                }
                row.put("date", dateStr);
                
                history.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return history;
    }
}
