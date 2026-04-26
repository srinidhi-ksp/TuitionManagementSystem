package model;

/**
 * DTO for Subject Fee Details
 * Contains subject information, fee amount, and payment status
 */
public class SubjectFeeDTO {
    private int subjectId;
    private String subjectName;
    private double monthlyFee;
    private String paymentStatus; // PAID, UNPAID
    
    // Constructor
    public SubjectFeeDTO(int subjectId, String subjectName, double monthlyFee, String paymentStatus) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.monthlyFee = monthlyFee;
        this.paymentStatus = paymentStatus;
    }

    // Default Constructor
    public SubjectFeeDTO() {
    }

    // Getters and Setters
    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
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

    @Override
    public String toString() {
        return "SubjectFeeDTO [subjectId=" + subjectId + 
               ", subjectName=" + subjectName + 
               ", monthlyFee=" + monthlyFee + 
               ", paymentStatus=" + paymentStatus + "]";
    }
}
