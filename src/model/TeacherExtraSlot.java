package model;

import java.util.Date;

/**
 * Represents one extra slot taught by a teacher beyond their assigned batches.
 * Stored in the 'teacher_extra_slots' collection.
 */
public class TeacherExtraSlot {
    private String id;          // e.g. "EXT_T001_2026_05_001"
    private String teacherId;   // e.g. "T001"
    private String batchId;     // e.g. "B007" (the batch they covered)
    private String batchName;   // display name
    private String day;         // "MON"..."SUN"
    private String timeslotId;  // "TS1"..."TS8"
    private String date;        // "2026-05-07" ISO date string
    private String month;       // "05"
    private String year;        // "2026"
    private double bonusAmount; // bonusPerExtraSlot value at time of recording
    private Date recordedAt;

    public TeacherExtraSlot() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getTimeslotId() { return timeslotId; }
    public void setTimeslotId(String timeslotId) { this.timeslotId = timeslotId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public double getBonusAmount() { return bonusAmount; }
    public void setBonusAmount(double bonusAmount) { this.bonusAmount = bonusAmount; }

    public Date getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Date recordedAt) { this.recordedAt = recordedAt; }
}
