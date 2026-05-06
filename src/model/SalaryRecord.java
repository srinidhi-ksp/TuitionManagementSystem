package model;

import java.util.Date;

/**
 * Salary Record model for tracking teacher payments
 */
public class SalaryRecord {
    private String id; // SAL_T001_2026_05
    private String teacherId;
    private String month;
    private String year;
    private int totalDays;
    private int presentDays;
    private int absentDays;
    private double perDaySalary;
    private double deduction;
    private double finalSalary;
    private Date lastUpdated;

    public SalaryRecord() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public int getPresentDays() { return presentDays; }
    public void setPresentDays(int presentDays) { this.presentDays = presentDays; }

    public int getAbsentDays() { return absentDays; }
    public void setAbsentDays(int absentDays) { this.absentDays = absentDays; }

    public double getPerDaySalary() { return perDaySalary; }
    public void setPerDaySalary(double perDaySalary) { this.perDaySalary = perDaySalary; }

    public double getDeduction() { return deduction; }
    public void setDeduction(double deduction) { this.deduction = deduction; }

    public double getFinalSalary() { return finalSalary; }
    public void setFinalSalary(double finalSalary) { this.finalSalary = finalSalary; }

    public Date getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Date lastUpdated) { this.lastUpdated = lastUpdated; }
}
