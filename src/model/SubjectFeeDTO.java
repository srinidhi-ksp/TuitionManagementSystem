package model;

/**
 * DTO for Fee Management
 * Represents a subject with its fee and payment status
 */
public class SubjectFeeDTO {
    private String subjectId;
    private String subjectName;
    private double monthlyFee;
    private String paymentStatus;  // PAID, UNPAID
    private int batchId;

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
}
