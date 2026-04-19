package model;

import java.util.Date;

public class Test {
    private int testId;
    private int batchId;
    private String testName;
    private Date testDate;
    private int maxMarks;

    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }

    public int getBatchId() { return batchId; }
    public void setBatchId(int batchId) { this.batchId = batchId; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public Date getTestDate() { return testDate; }
    public void setTestDate(Date testDate) { this.testDate = testDate; }

    public int getMaxMarks() { return maxMarks; }
    public void setMaxMarks(int maxMarks) { this.maxMarks = maxMarks; }
}
