package model;

import java.util.Date;

public class Attendance {

    private int attendanceId;
    private String userId;
    private String status;
    private String reason;
    private String markedBy;
    private Date attendanceDate;

    // Default Constructor
    public Attendance() {
    }

    // Parameterized Constructor
    public Attendance(int attendanceId, String userId, String status,
                      String reason, String markedBy, Date attendanceDate) {

        this.attendanceId = attendanceId;
        this.userId = userId;
        this.status = status;
        this.reason = reason;
        this.markedBy = markedBy;
        this.attendanceDate = attendanceDate;
    }

    // Getters and Setters

    public int getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(int attendanceId) {
        this.attendanceId = attendanceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }

    public Date getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(Date attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    @Override
    public String toString() {
        return "Attendance [attendanceId=" + attendanceId +
                ", userId=" + userId +
                ", status=" + status +
                ", reason=" + reason +
                ", markedBy=" + markedBy +
                ", attendanceDate=" + attendanceDate + "]";
    }
}