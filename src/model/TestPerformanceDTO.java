package model;

import java.util.List;
import org.bson.Document;

public class TestPerformanceDTO {
    private int totalTests;
    private double averageScore;
    private int highestScore;
    private String grade;
    private List<TestHistoryItem> testHistory;

    public int getTotalTests() { return totalTests; }
    public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }

    public int getHighestScore() { return highestScore; }
    public void setHighestScore(int highestScore) { this.highestScore = highestScore; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public List<TestHistoryItem> getTestHistory() { return testHistory; }
    public void setTestHistory(List<TestHistoryItem> testHistory) { this.testHistory = testHistory; }

    public static class TestHistoryItem {
        private String testName;
        private int score;
        private int totalMarks;
        private double percentage;
        private String result; // PASS/FAIL

        public String getTestName() { return testName; }
        public void setTestName(String testName) { this.testName = testName; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public int getTotalMarks() { return totalMarks; }
        public void setTotalMarks(int totalMarks) { this.totalMarks = totalMarks; }

        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
    }
}
