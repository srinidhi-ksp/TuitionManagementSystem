package model;

import java.util.Date;

public class Payment {
    private int paymentId;
    private int feeId;
    private double amountPaid;
    private Date paymentDate;
    private String paymentMode;
    private String receiptNo;
    
    // New fields for subject-wise payment
    private String studentId;
    private String subjectId;
    private int month;
    
    // New fields for batch-wise tracking
    private int batchId;
    private String monthStr; // "YYYY-MM" format
    private String status; // "PAID" or "UNPAID"

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public int getFeeId() { return feeId; }
    public void setFeeId(int feeId) { this.feeId = feeId; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public Date getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
    
    public String getMethod() { return paymentMode; }
    public void setMethod(String method) { this.paymentMode = method; }
    
    // New getters/setters
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }
    
    public String getMonthStr() { return monthStr; }
    public void setMonthStr(String monthStr) { this.monthStr = monthStr; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
