package model;

import java.util.Date;

/**
 * Payment Model - Represents subject-wise payments for students
 * Schema:
 * - student_id: Student's user ID
 * - subject_id: Subject ID (ensures unique subject fees)
 * - amount_paid: Fee amount paid for the subject
 * - payment_mode: Payment method (Cash/UPI/Card)
 * - payment_date: Date of payment
 * - month: Month for which payment is made
 */
public class Payment {
    private int paymentId;
    private String studentId;         // Student's user_id
    private int subjectId;            // Subject ID
    private double amountPaid;        // Amount paid for subject
    private Date paymentDate;         // Date of payment
    private String paymentMode;       // Cash, UPI, Card
    private String month;             // Month (e.g., "2024-04")
    
    // Legacy fields (for backward compatibility)
    private int feeId;
    private String receiptNo;

    // Default Constructor
    public Payment() {
    }

    // Constructor with new schema
    public Payment(int paymentId, String studentId, int subjectId, 
                   double amountPaid, Date paymentDate, String paymentMode, String month) {
        this.paymentId = paymentId;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.paymentMode = paymentMode;
        this.month = month;
    }

    // Getters and Setters
    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    // Legacy getters/setters for backward compatibility
    public int getFeeId() {
        return feeId;
    }

    public void setFeeId(int feeId) {
        this.feeId = feeId;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    @Override
    public String toString() {
        return "Payment [paymentId=" + paymentId +
               ", studentId=" + studentId +
               ", subjectId=" + subjectId +
               ", amountPaid=" + amountPaid +
               ", paymentDate=" + paymentDate +
               ", paymentMode=" + paymentMode +
               ", month=" + month + "]";
    }
}
