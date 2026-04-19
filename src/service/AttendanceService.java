package service;

import dao.AttendanceDAO;
import model.Attendance;

import java.util.List;

public class AttendanceService {

    private AttendanceDAO attendanceDAO;

    public AttendanceService() {
        attendanceDAO = new AttendanceDAO();
    }

    public boolean markAttendance(Attendance attendance) {
        return attendanceDAO.addAttendance(attendance);
    }

    public Attendance getAttendance(int attendanceId) {
        return attendanceDAO.getAttendanceById(attendanceId);
    }

    public List<Attendance> getAllAttendance() {
        return attendanceDAO.getAllAttendance();
    }

    public boolean deleteAttendance(int attendanceId) {
        return attendanceDAO.deleteAttendance(attendanceId);
    }
}