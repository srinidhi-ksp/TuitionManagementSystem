package model;

import java.util.Date;

/**
 * Salary rules document stored in the 'salary_rules' collection.
 * Single document with _id = "DEFAULT".
 */
public class SalaryRules {
    private String id;                      // "DEFAULT"
    private int freeDaysAllowed;            // grace absent days before deductions start (default 1)
    private int baseDeductionPerAbsentDay;  // deduction for first chargeable absent day (default 300)
    private int deductionIncrementPerDay;   // increment added per subsequent absent day (default 100)
    private int bonusPerExtraSlot;          // bonus credited per extra slot taught (default 200)
    private String lastUpdatedBy;
    private Date lastUpdatedAt;

    public SalaryRules() {
        // Default values
        this.id = "DEFAULT";
        this.freeDaysAllowed = 1;
        this.baseDeductionPerAbsentDay = 300;
        this.deductionIncrementPerDay = 100;
        this.bonusPerExtraSlot = 200;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getFreeDaysAllowed() { return freeDaysAllowed; }
    public void setFreeDaysAllowed(int freeDaysAllowed) { this.freeDaysAllowed = freeDaysAllowed; }

    public int getBaseDeductionPerAbsentDay() { return baseDeductionPerAbsentDay; }
    public void setBaseDeductionPerAbsentDay(int baseDeductionPerAbsentDay) {
        this.baseDeductionPerAbsentDay = baseDeductionPerAbsentDay;
    }

    public int getDeductionIncrementPerDay() { return deductionIncrementPerDay; }
    public void setDeductionIncrementPerDay(int deductionIncrementPerDay) {
        this.deductionIncrementPerDay = deductionIncrementPerDay;
    }

    public int getBonusPerExtraSlot() { return bonusPerExtraSlot; }
    public void setBonusPerExtraSlot(int bonusPerExtraSlot) { this.bonusPerExtraSlot = bonusPerExtraSlot; }

    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }

    public Date getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Date lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    /**
     * Calculate total deduction for a given number of absent days using current rules.
     * Formula: sum of (baseDeduction + increment * i) for i from 0 to (chargeable-1)
     */
    public double calculateDeduction(int absentDays) {
        double total = 0;
        int chargeable = Math.max(0, absentDays - freeDaysAllowed);
        for (int i = 0; i < chargeable; i++) {
            total += baseDeductionPerAbsentDay + deductionIncrementPerDay * i;
        }
        return total;
    }

    /**
     * Calculate extra bonus for a given count of extra slots taught.
     */
    public double calculateBonus(int extraSlotCount) {
        return (double) extraSlotCount * bonusPerExtraSlot;
    }
}
