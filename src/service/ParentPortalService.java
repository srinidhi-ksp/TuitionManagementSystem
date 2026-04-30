package service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.AttendanceDAO;
import dao.EnrollmentDAO;
import dao.ParentDAO;
import dao.PaymentDAO;
import dao.StudentDAO;
import dao.TestsDAO;
import model.Parent;
import model.Student;
import model.TestMark;
import model.Attendance;

/**
 * Parent Portal Service - Aggregates data for the Parent Dashboard
 */
public class ParentPortalService {

    private ParentDAO parentDAO;
    private StudentDAO studentDAO;
    private EnrollmentDAO enrollmentDAO;
    private TestsDAO testsDAO;
    private AttendanceDAO attendanceDAO;
    private FeeService feeService;

    public ParentPortalService() {
        this.parentDAO = new ParentDAO();
        this.studentDAO = new StudentDAO();
        this.enrollmentDAO = new EnrollmentDAO();
        this.testsDAO = new TestsDAO();
        this.attendanceDAO = new AttendanceDAO();
        this.feeService = new FeeService();
    }

    /**
     * Get linked students for a parent
     */
    public List<Student> getLinkedStudents(String parentUserId) {
        Parent p = parentDAO.getByUserId(parentUserId);
        List<Student> students = new ArrayList<>();
        if (p != null && p.getLinkedStudentIds() != null) {
            for (String sId : p.getLinkedStudentIds()) {
                Student s = studentDAO.getStudentByUserId(sId);
                if (s == null) s = studentDAO.getStudentById(sId);
                if (s != null) students.add(s);
            }
        }
        return students;
    }

    /**
     * Get consolidated student performance for parent overview
     */
    public Map<String, Object> getStudentOverview(String studentUserId) {
        Map<String, Object> overview = new HashMap<>();
        
        // 1. Fee Stats
        Map<String, Object> feeSummary = feeService.getFeeSummary(studentUserId);
        overview.put("fees", feeSummary);
        
        // 2. Attendance Stats
        double attendancePercent = attendanceDAO.getAttendancePercentage(studentUserId);
        overview.put("attendancePercent", attendancePercent);
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy");
        List<Attendance> list = attendanceDAO.getAttendanceByStudentId(studentUserId);
        int presentCount = 0;
        for (Attendance a : list) {
            if ("PRESENT".equalsIgnoreCase(a.getStatus())) presentCount++;
        }

        // 3. Performance Stats
        List<TestMark> marks = testsDAO.getStudentMarks(studentUserId);
        double avgMarks = 0;
        if (!marks.isEmpty()) {
            double total = 0;
            for (TestMark m : marks) total += (double) m.getMarksObtained() / m.getMaxMarks() * 100;
            avgMarks = total / marks.size();
        }
        overview.put("avgMarks", avgMarks);
        overview.put("totalExams", marks.size());
        
        // 4. Enrollment Stats
        int subjectsCount = enrollmentDAO.getEnrollmentsByStudentId(studentUserId).size();
        overview.put("subjectsCount", subjectsCount);
        
        return overview;
    }

    /**
     * Get marks for report card generation
     */
    public List<TestMark> getDetailedMarks(String studentUserId) {
        return testsDAO.getStudentMarks(studentUserId);
    }

    /**
     * Get notifications for student
     */
    public List<String> getNotifications(String studentUserId) {
        List<String> notes = new ArrayList<>();
        
        // 1. Fee Due
        Map<String, Object> fees = feeService.getFeeSummary(studentUserId);
        double pending = (double) fees.get("pendingAmount");
        if (pending > 0) {
            notes.add("⚠️ Fee Due: ₹" + pending + " pending. Please pay soon.");
        }
        
        // 2. Attendance Warning
        double att = attendanceDAO.getAttendancePercentage(studentUserId);
        if (att < 75 && att > 0) {
            notes.add("📉 Attendance Warning: " + String.format("%.1f%%", att) + " is below 75%.");
        }
        
        // 3. Recent Test Results
        List<TestMark> marks = testsDAO.getStudentMarks(studentUserId);
        if (!marks.isEmpty()) {
            TestMark last = marks.get(marks.size() - 1);
            notes.add("📝 New Mark: " + last.getTestName() + " - " + last.getMarksObtained() + "/" + last.getMaxMarks());
        }
        
        return notes;
    }

    /**
     * Get recent activity
     */
    public List<Map<String, String>> getRecentActivity(String studentUserId) {
        List<Map<String, String>> activity = new ArrayList<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM");

        // 1. Recent Payments
        PaymentDAO pDAO = new PaymentDAO();
        for (model.Payment p : pDAO.getRecentPayments(studentUserId, 3)) {
            Map<String, String> item = new HashMap<>();
            item.put("type", "PAYMENT");
            item.put("title", "Fee Paid: ₹" + p.getAmountPaid());
            item.put("desc", p.getSubjectId() + " via " + p.getPaymentMode());
            item.put("date", sdf.format(p.getPaymentDate()));
            activity.add(item);
        }

        // 2. Recent Attendance
        List<model.Attendance> attList = attendanceDAO.getAttendanceByStudentId(studentUserId);
        if (!attList.isEmpty()) {
            model.Attendance last = attList.get(attList.size() - 1);
            Map<String, String> item = new HashMap<>();
            item.put("type", "ATTENDANCE");
            item.put("title", "Attendance Marked");
            item.put("desc", "Status: " + last.getStatus());
            item.put("date", last.getAttendanceDate() != null ? sdf.format(last.getAttendanceDate()) : "Recent");
            activity.add(item);
        }

        return activity;
    }
}
