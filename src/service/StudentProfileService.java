package service;

import dao.*;
import model.*;
import java.util.*;
import org.bson.Document;

public class StudentProfileService {
    private StudentDAO studentDAO = new StudentDAO();
    private TestsDAO testsDAO = new TestsDAO();
    private EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private FeeService feeService = new FeeService();

    public StudentProfileDTO getStudentProfile(String userId) {
        StudentProfileDTO profile = new StudentProfileDTO();
        
        // 1. Resolve studentId and fetch Student
        Student student = studentDAO.getStudentByUserId(userId);
        if (student == null) {
            student = studentDAO.getStudentById(userId);
        }
        
        if (student == null) return null;
        profile.setStudent(student);
        String studentId = student.getUserId();

        // 2. Parent Details
        profile.setParentName(student.getParentName());
        profile.setParentPhone(student.getParentPhone());
        profile.setParentOccupation(student.getParentOccupation());
        profile.setParentRelation(student.getParentRelation());

        // 3. Fee Summary
        profile.setFeeSummary(feeService.getFeeSummary(studentId));

        // 4. Enrollment Summary
        try {
            List<Enrollment> en = enrollmentDAO.getEnrollmentsByStudentId(studentId);
            profile.setTotalEnrollments(en != null ? en.size() : 0);
        } catch (Exception e) {
            profile.setTotalEnrollments(0);
        }

        // 5. Test Performance
        profile.setTestPerformance(getTestPerformance(studentId));

        return profile;
    }

    public TestPerformanceDTO getTestPerformance(String studentId) {
        TestPerformanceDTO performance = new TestPerformanceDTO();
        List<Test> tests = testsDAO.getTestsByStudentId(studentId);
        
        if (tests.isEmpty()) {
            performance.setTotalTests(0);
            performance.setAverageScore(0);
            performance.setGrade("N/A");
            performance.setTestHistory(new ArrayList<>());
            return performance;
        }

        List<TestPerformanceDTO.TestHistoryItem> history = new ArrayList<>();
        int totalScore = 0;
        int totalMaxMarks = 0;
        int highestScore = 0;
        int evaluatedCount = 0;

        for (Test t : tests) {
            List<Document> attempts = t.getAttempts();
            if (attempts == null) continue;

            for (Document attempt : attempts) {
                if (studentId.equals(attempt.getString("student_id"))) {
                    String status = attempt.getString("status");
                    Object scoreObj = attempt.get("score");
                    
                    if ("EVALUATED".equals(status) && scoreObj instanceof Number) {
                        int score = ((Number) scoreObj).intValue();
                        int maxMarks = t.getMaxMarks();
                        
                        TestPerformanceDTO.TestHistoryItem item = new TestPerformanceDTO.TestHistoryItem();
                        item.setTestName(t.getTestName());
                        item.setScore(score);
                        item.setTotalMarks(maxMarks);
                        double pct = maxMarks > 0 ? (score * 100.0 / maxMarks) : 0;
                        item.setPercentage(pct);
                        item.setResult(pct >= 40 ? "PASS" : "FAIL");
                        
                        history.add(item);
                        
                        totalScore += score;
                        totalMaxMarks += maxMarks;
                        if (score > highestScore) highestScore = score;
                        evaluatedCount++;
                    }
                }
            }
        }

        performance.setTotalTests(evaluatedCount);
        performance.setHighestScore(highestScore);
        
        double avg = totalMaxMarks > 0 ? (totalScore * 100.0 / totalMaxMarks) : 0;
        performance.setAverageScore(avg);
        
        // Grade Logic
        if (evaluatedCount == 0) performance.setGrade("N/A");
        else if (avg >= 90) performance.setGrade("A");
        else if (avg >= 75) performance.setGrade("B");
        else if (avg >= 60) performance.setGrade("C");
        else performance.setGrade("D");

        performance.setTestHistory(history);
        return performance;
    }
}
