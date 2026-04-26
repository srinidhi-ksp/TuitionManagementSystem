package model;

public class Receipt {
    private String studentName;
    private String studentId;
    private String className;
    private String batchName;
    private String subjectName;
    private double amount;
    private String paymentDate;
    private String paymentMode;
    private String status = "PAID";

    // Constructors
    public Receipt() {}

    public Receipt(String studentName, String studentId, String className, String batchName, 
                   String subjectName, double amount, String paymentDate, String paymentMode) {
        this.studentName = studentName;
        this.studentId = studentId;
        this.className = className;
        this.batchName = batchName;
        this.subjectName = subjectName;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMode = paymentMode;
    }

    // Getters and Setters
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
