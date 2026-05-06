package model;

import java.util.Date;

/**
 * Payment Model - supports both legacy subject-wise payments and newer
 * batch-wise payment tracking.
 */
public class Payment {
    private int paymentId;
    private int feeId;
    private String studentId;
    private String subjectId;
    private int batchId;
    private double amountPaid;
    private Date paymentDate;
    private String paymentMode;
    private String month;
    private String status;
    private String receiptNo;

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public int getFeeId() { return feeId; }
    public void setFeeId(int feeId) { this.feeId = feeId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public Date getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getMethod() { return paymentMode; }
    public void setMethod(String method) { this.paymentMode = method; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public void setMonth(int month) { this.month = String.valueOf(month); }

    public String getMonthStr() { return month; }
    public void setMonthStr(String monthStr) { this.month = monthStr; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }

    @Override
    public String toString() {
        return "Payment [paymentId=" + paymentId +
               ", studentId=" + studentId +
               ", subjectId=" + subjectId +
               ", batchId=" + batchId +
               ", amountPaid=" + amountPaid +
               ", paymentDate=" + paymentDate +
               ", paymentMode=" + paymentMode +
               ", month=" + month +
               ", status=" + status + "]";
    }
}
