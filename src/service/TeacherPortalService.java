package service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dao.AttendanceDAO;
import dao.BatchDAO;
import dao.EnrollmentDAO;
import dao.StudentDAO;
import dao.SubjectDAO;
import dao.SyllabusProgressDAO;
import dao.TestsDAO;
import dao.TeacherDAO;
import model.Attendance;
import model.Batch;
import model.ChapterProgress;
import model.Enrollment;
import model.Student;
import model.Subject;
import model.Test;

public class TeacherPortalService {

    private final BatchDAO batchDAO = new BatchDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final SubjectDAO subjectDAO = new SubjectDAO();
    private final TestsDAO testsDAO = new TestsDAO();
    private final TeacherDAO teacherDAO = new TeacherDAO();
    private final AttendanceDAO attendanceDAO = new AttendanceDAO();
    private final SyllabusProgressDAO syllabusDAO = new SyllabusProgressDAO();

    public DashboardData getDashboardData(String teacherId) {
        List<Batch> batches = getTeacherBatches(teacherId);
        DashboardData data = new DashboardData();
        data.batches = batches;
        data.totalBatches = batches.size();
        data.totalStudents = countStudents(batches);
        data.pendingEvaluations = testsDAO.countPendingEvaluationsByTeacher(teacherId);
        data.todayClasses = getTodayClasses(batches);
        data.pendingTests = testsDAO.getPendingTestsByTeacher(teacherId);
        return data;
    }

    public List<Batch> getTeacherBatches(String teacherId) {
        if (teacherId == null || teacherId.trim().isEmpty()) return new ArrayList<>();
        return batchDAO.getBatchesByTeacherId(teacherId);
    }

    public model.Teacher getTeacherProfile(String teacherId) {
        if (teacherId == null || teacherId.trim().isEmpty()) return null;
        return teacherDAO.getTeacherById(teacherId);
    }

    public List<BatchRow> getTeacherBatchRows(String teacherId) {
        List<BatchRow> rows = new ArrayList<>();
        for (Batch batch : getTeacherBatches(teacherId)) {
            Subject subject = subjectDAO.getSubjectById(batch.getSubjectId());
            BatchRow row = new BatchRow();
            row.batch = batch;
            row.subjectName = subject != null && subject.getSubjectName() != null ? subject.getSubjectName() : "Unknown";
            row.studentCount = enrollmentDAO.getEnrollmentCountByBatch(batch.getBatchId());
            rows.add(row);
        }
        return rows;
    }

    public List<StudentRow> getTeacherStudents(String teacherId) {
        return getTeacherStudentsByBatch(teacherId, -1); // -1 means All Batches
    }

    public List<StudentRow> getTeacherStudentsByBatch(String teacherId, int filterBatchId) {
        List<StudentRow> rows = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        
        List<Batch> batchesToProcess;
        if (filterBatchId == -1) {
            batchesToProcess = getTeacherBatches(teacherId);
        } else {
            batchesToProcess = new ArrayList<>();
            Batch b = batchDAO.getBatchById(filterBatchId);
            if (b != null && teacherId.equals(b.getTeacherUserId())) {
                batchesToProcess.add(b);
            }
        }

        for (Batch batch : batchesToProcess) {
            for (Enrollment enrollment : enrollmentDAO.getActiveEnrollmentsByBatchId(batch.getBatchId())) {
                String studentId = enrollment.getStudentUserId();
                if (studentId == null || !seen.add(studentId)) continue;
                Student student = studentDAO.getStudentById(studentId);
                if (student == null) continue;
                StudentRow row = new StudentRow();
                row.student = student;
                row.batch = batch;
                row.enrollmentDate = enrollment.getEnrollmentDate();
                row.status = enrollment.getStatus() != null ? enrollment.getStatus() : "ACTIVE";
                rows.add(row);
            }
        }
        return rows;
    }

    public List<Student> getStudentsForTeacherBatch(String teacherId, int batchId) {
        if (!isTeacherBatch(teacherId, batchId)) return new ArrayList<>();
        List<Student> students = new ArrayList<>();
        for (String studentId : enrollmentDAO.getStudentIdsByBatchId(batchId)) {
            Student student = studentDAO.getStudentById(studentId);
            if (student != null) students.add(student);
        }
        return students;
    }

    public Map<String, String> getAttendanceStatusMap(String teacherId, int batchId, String dateStr) {
        Map<String, String> statuses = new LinkedHashMap<>();
        if (!isTeacherBatch(teacherId, batchId)) return statuses;
        for (Attendance attendance : attendanceDAO.getAttendanceByBatchAndDate(batchId, dateStr)) {
            if (attendance.getUserId() != null && attendance.getStatus() != null) {
                statuses.put(attendance.getUserId(), toDisplayStatus(attendance.getStatus()));
            }
        }
        return statuses;
    }

    public boolean hasAttendance(String teacherId, int batchId, String dateStr) {
        return isTeacherBatch(teacherId, batchId) && attendanceDAO.hasAttendanceForBatchAndDate(batchId, dateStr);
    }

    public boolean saveAttendance(String teacherId, int batchId, String dateStr, Map<String, String> statuses) {
        if (!isTeacherBatch(teacherId, batchId)) return false;
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : statuses.entrySet()) {
            normalized.put(entry.getKey(), toDbStatus(entry.getValue()));
        }
        return attendanceDAO.saveBatchAttendance(teacherId, batchId, dateStr, normalized);
    }

    public boolean createTest(String teacherId, int batchId, String name, Date date, int totalMarks) {
        if (!isTeacherBatch(teacherId, batchId)) return false;
        Test test = new Test();
        test.setTestId((int)(System.currentTimeMillis() % 1000000));
        test.setBatchId(batchId);
        test.setTestName(name);
        test.setTestDate(date);
        test.setMaxMarks(totalMarks);
        test.setAttempts(new ArrayList<org.bson.Document>());
        return testsDAO.addTeacherTest(test, teacherId);
    }

    public List<Test> getTestsForBatch(String teacherId, int batchId) {
        if (!isTeacherBatch(teacherId, batchId)) return new ArrayList<>();
        return testsDAO.getTestsByTeacherAndBatch(teacherId, batchId);
    }

    public List<MarkRow> getMarkRows(String teacherId, Test test) {
        List<MarkRow> rows = new ArrayList<>();
        if (test == null || !isTeacherBatch(teacherId, test.getBatchId())) return rows;
        List<Student> students = getStudentsForTeacherBatch(teacherId, test.getBatchId());
        for (Student student : students) {
            MarkRow row = new MarkRow();
            row.student = student;
            row.existingScore = testsDAO.getExistingScore(test.getTestId(), student.getUserId());
            rows.add(row);
        }
        return rows;
    }

    public boolean saveMark(String teacherId, Test test, String studentId, int score) {
        if (test == null || !isTeacherBatch(teacherId, test.getBatchId())) return false;
        if (score < 0 || score > test.getMaxMarks()) return false;
        return testsDAO.saveAttempt(test.getTestId(), studentId, score);
    }

    public Integer getExistingScore(int testId, String studentId) {
        return testsDAO.getExistingScore(testId, studentId);
    }

    public List<ChapterProgress> getSyllabusProgress(String teacherId, int batchId) {
        return syllabusDAO.getProgressByTeacherBatch(batchId, teacherId);
    }

    public boolean updateSyllabusProgress(String teacherId, int batchId, int chapterId, int completion, String remarks) {
        if (!isTeacherBatch(teacherId, batchId) || completion < 0 || completion > 100) return false;
        return syllabusDAO.updateBatchSyllabusProgress(batchId, chapterId, completion, remarks, teacherId);
    }

    public boolean isTeacherBatch(String teacherId, int batchId) {
        Batch batch = batchDAO.getBatchById(batchId);
        return batch != null && teacherId != null && teacherId.equals(batch.getTeacherUserId());
    }

    private int countStudents(List<Batch> batches) {
        Set<String> studentIds = new LinkedHashSet<>();
        for (Batch batch : batches) {
            for (Enrollment enrollment : enrollmentDAO.getActiveEnrollmentsByBatchId(batch.getBatchId())) {
                if (enrollment.getStudentUserId() != null) {
                    studentIds.add(enrollment.getStudentUserId());
                }
            }
        }
        return studentIds.size();
    }

    private List<ScheduleItem> getTodayClasses(List<Batch> batches) {
        List<ScheduleItem> items = new ArrayList<>();
        String today = normalizeDay(new SimpleDateFormat("EEEE").format(new Date()));
        for (Batch batch : batches) {
            if (batch.getSchedules() == null) continue;
            for (model.Schedule schedule : batch.getSchedules()) {
                if (schedule.getDay() != null && today.equals(normalizeDay(schedule.getDay()))) {
                    ScheduleItem item = new ScheduleItem();
                    item.batch = batch;
                    item.schedule = schedule;
                    items.add(item);
                }
            }
        }
        return items;
    }

    private String normalizeDay(String day) {
        if (day == null) return "";
        String value = day.trim().toUpperCase();
        if (value.length() >= 3) value = value.substring(0, 3);
        return value;
    }

    private String toDbStatus(String status) {
        if (status == null) return "PRESENT";
        return status.trim().toUpperCase();
    }

    private String toDisplayStatus(String status) {
        if (status == null) return "Present";
        if ("PRESENT".equalsIgnoreCase(status)) return "Present";
        if ("ABSENT".equalsIgnoreCase(status)) return "Absent";
        if ("LATE".equalsIgnoreCase(status)) return "Late";
        if ("CANCELLED".equalsIgnoreCase(status)) return "Cancelled";
        return status;
    }

    public static class DashboardData {
        public int totalBatches;
        public int totalStudents;
        public int pendingEvaluations;
        public List<ScheduleItem> todayClasses = new ArrayList<>();
        public List<Test> pendingTests = new ArrayList<>();
        public List<Batch> batches = new ArrayList<>();
    }

    public static class BatchRow {
        public Batch batch;
        public String subjectName;
        public int studentCount;
    }

    public static class StudentRow {
        public Student student;
        public Batch batch;
        public Date enrollmentDate;
        public String status;
    }

    public static class MarkRow {
        public Student student;
        public Integer existingScore;
    }

    public static class ScheduleItem {
        public Batch batch;
        public model.Schedule schedule;
    }
}
