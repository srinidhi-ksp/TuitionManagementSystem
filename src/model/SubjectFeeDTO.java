package model;

/**
 * DTO for Fee Management
 * Represents a subject with its fee and payment status
 */
public class SubjectFeeDTO {
    private String subjectId;
    private String subjectName;
    private double monthlyFee;
    private String paymentStatus;  // PAID, UNPAID, PENDING
    private int batchId;
    private double paidAmount;
    private double pendingAmount;
    private String paymentMethod; // UPI, CARD, CASH_REQUEST
    private String detailedStatus; // SUCCESS, REQUESTED

    // Constructors
    public SubjectFeeDTO() {}

    public SubjectFeeDTO(String subjectId, String subjectName, double monthlyFee, String paymentStatus) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.monthlyFee = monthlyFee;
        this.paymentStatus = paymentStatus;
    }

    public SubjectFeeDTO(String subjectId, String subjectName, double monthlyFee, String paymentStatus, int batchId) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.monthlyFee = monthlyFee;
        this.paymentStatus = paymentStatus;
        this.batchId = batchId;
        this.paidAmount = 0.0;
        this.pendingAmount = monthlyFee;
    }
    
    public SubjectFeeDTO(String subjectId, String subjectName, double monthlyFee, String paymentStatus, int batchId, double paidAmount, double pendingAmount) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.monthlyFee = monthlyFee;
        this.paymentStatus = paymentStatus;
        this.batchId = batchId;
        this.paidAmount = paidAmount;
        this.pendingAmount = pendingAmount;
    }

    public SubjectFeeDTO(String subjectId, String subjectName, double monthlyFee, String paymentStatus, int batchId, double paidAmount, double pendingAmount, String method, String detailedStatus) {
        this(subjectId, subjectName, monthlyFee, paymentStatus, batchId, paidAmount, pendingAmount);
        this.paymentMethod = method;
        this.detailedStatus = detailedStatus;
    }

    // Getters & Setters
    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public double getMonthlyFee() {
        return monthlyFee;
    }

    public void setMonthlyFee(double monthlyFee) {
        this.monthlyFee = monthlyFee;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public double getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(double pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    @Override
    public String toString() {
        return "SubjectFeeDTO{" +
                "subjectId='" + subjectId + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", monthlyFee=" + monthlyFee +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", batchId=" + batchId +
                '}';
    }

    public String getPaymentMethod() { return paymentMethod != null ? paymentMethod : "—"; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getDetailedStatus() { return detailedStatus != null ? detailedStatus : "—"; }
    public void setDetailedStatus(String detailedStatus) { this.detailedStatus = detailedStatus; }
}
