package model;

import java.util.Date;

public class Test {
    private int testId;
    private int batchId;
    private String testName;
    private java.util.Date testDate;
    private int maxMarks;
    private java.util.List<org.bson.Document> attempts;

    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public java.util.Date getTestDate() { return testDate; }
    public void setTestDate(java.util.Date testDate) { this.testDate = testDate; }

    public int getMaxMarks() { return maxMarks; }
    public void setMaxMarks(int maxMarks) { this.maxMarks = maxMarks; }

    public java.util.List<org.bson.Document> getAttempts() { return attempts; }
    public void setAttempts(java.util.List<org.bson.Document> attempts) { this.attempts = attempts; }
}
