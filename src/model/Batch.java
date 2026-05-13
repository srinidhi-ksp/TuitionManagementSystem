package model;

import java.util.Date;
import java.util.List;

public class Batch {

    private int batchId;
    private int subjectId;
    private String teacherUserId;
    private String batchName;
    private String timing;  // e.g. "09:00 - 11:00" — stored in & read from DB (legacy display)
    private Date startTime;
    private Date endTime;
    private String meetingLink;
    private String classMode;
    private String category; // e.g. "Class 12", "Class 11"
    private String standard; // e.g. "8", "12" — dedicated field for EXACT matching
    private String status = "ACTIVE"; // "ACTIVE" or "INACTIVE"

    // Legacy schedule list (day + start/end strings)
    private java.util.List<Schedule> schedules;

    // NEW: timeslot-based schedule (day + timeslotId pairs) — per requirements §1.2 / §4
    private String timeslotId;                       // shared timeslotId for this batch
    private java.util.List<ScheduleEntry> scheduleEntries; // [{day, timeslotId}, ...]

    // Default Constructor
    public Batch() {
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStandard() { return standard; }
    public void setStandard(String standard) { this.standard = standard; }

    // Parameterized Constructor
    public Batch(int batchId, int subjectId, String teacherUserId,
                 String batchName, Date startTime, Date endTime,
                 String meetingLink, String classMode) {

        this.batchId = batchId;
        this.subjectId = subjectId;
        this.teacherUserId = teacherUserId;
        this.batchName = batchName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.meetingLink = meetingLink;
        this.classMode = classMode;
    }

    // Getters and Setters

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getTeacherUserId() {
        return teacherUserId;
    }

    public void setTeacherUserId(String teacherUserId) {
        this.teacherUserId = teacherUserId;
    }

    public String getTeacherId() {
        return teacherUserId;
    }

    public String getBatchName() {
        return batchName;
    }

    public void setBatchName(String batchName) {
        this.batchName = batchName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getClassMode() {
        return classMode;
    }

    public void setClassMode(String classMode) {
        this.classMode = classMode;
    }

    public String getMode() {
        return classMode;
    }

    public String getTiming() {
        return timing;
    }

    public void setTiming(String timing) {
        this.timing = timing;
    }

    public java.util.List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(java.util.List<Schedule> schedules) {
        this.schedules = schedules;
    }

    // ── NEW: timeslot-based schedule fields ──────────────────────────────────

    public String getTimeslotId() { return timeslotId; }
    public void setTimeslotId(String timeslotId) { this.timeslotId = timeslotId; }

    public java.util.List<ScheduleEntry> getScheduleEntries() { return scheduleEntries; }
    public void setScheduleEntries(java.util.List<ScheduleEntry> scheduleEntries) {
        this.scheduleEntries = scheduleEntries;
    }

    /**
     * Returns all days this batch runs, derived from scheduleEntries.
     * Falls back to legacy schedule list, then to timing string parsing.
     */
    public java.util.List<String> getDays() {
        java.util.List<String> days = new java.util.ArrayList<>();
        if (scheduleEntries != null) {
            for (ScheduleEntry e : scheduleEntries) {
                if (e.getDay() != null) days.add(e.getDay());
            }
        }
        if (days.isEmpty() && schedules != null) {
            for (Schedule s : schedules) {
                if (s.getDay() != null) days.add(s.getDay());
            }
        }
        return days;
    }

    @Override
    public String toString() {
        return (batchName != null ? batchName : "") + " (" + batchId + ")";
    }
}
