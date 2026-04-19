package model;

import java.util.Date;

public class Enrollment {

    private int enrollmentId;
    private String studentUserId;
    private int batchId;
    private String status;
    private String remarks;
    private Date enrollmentDate;

    // Default Constructor
    public Enrollment() {
    }

    // Parameterized Constructor
    public Enrollment(int enrollmentId, String studentUserId, int batchId,
                      String status, String remarks, Date enrollmentDate) {

        this.enrollmentId = enrollmentId;
        this.studentUserId = studentUserId;
        this.batchId = batchId;
        this.status = status;
        this.remarks = remarks;
        this.enrollmentDate = enrollmentDate;
    }

    // Getters and Setters

    public int getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public String getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(String studentUserId) {
        this.studentUserId = studentUserId;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Date getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(Date enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    @Override
    public String toString() {
        return "Enrollment [enrollmentId=" + enrollmentId +
                ", studentUserId=" + studentUserId +
                ", batchId=" + batchId +
                ", status=" + status +
                ", remarks=" + remarks +
                ", enrollmentDate=" + enrollmentDate + "]";
    }
}