package model;

public class StudentDashboard {
    private String name;
    private int batchCount;
    private double totalFees;
    private double paidAmount;
    private double pending;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getBatchCount() { return batchCount; }
    public void setBatchCount(int batchCount) { this.batchCount = batchCount; }
    public double getTotalFees() { return totalFees; }
    public void setTotalFees(double totalFees) { this.totalFees = totalFees; }
    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }
    public double getPending() { return pending; }
    public void setPending(double pending) { this.pending = pending; }
}
