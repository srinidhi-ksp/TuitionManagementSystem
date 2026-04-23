package model;

import java.util.Date;

public class Batch {

    private int batchId;
    private int subjectId;
    private String teacherUserId;
    private String batchName;
    private String timing;  // e.g. "09:00 - 11:00" — stored in & read from DB
    private Date startTime;
    private Date endTime;
    private String meetingLink;
    private String classMode;
    private String category; // e.g. "Class 12", "Class 11"
    private String standard; // e.g. "8", "12" — dedicated field for EXACT matching
    private String status = "ACTIVE"; // "ACTIVE" or "INACTIVE"

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

    public String getTiming() {
        return timing;
    }

    public void setTiming(String timing) {
        this.timing = timing;
    }

    @Override
    public String toString() {
        return "Batch [batchId=" + batchId +
                ", subjectId=" + subjectId +
                ", teacherUserId=" + teacherUserId +
                ", batchName=" + batchName +
                ", timing=" + timing +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", meetingLink=" + meetingLink +
                ", classMode=" + classMode + "]";
    }
}