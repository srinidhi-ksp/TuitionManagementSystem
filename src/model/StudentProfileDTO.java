package model;

import java.util.Map;

public class StudentProfileDTO {
    private Student student;
    private String parentName;
    private String parentPhone;
    private String parentOccupation;
    private String parentRelation;
    private Map<String, Object> feeSummary;
    private TestPerformanceDTO testPerformance;
    private int totalEnrollments;

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }

    public String getParentOccupation() { return parentOccupation; }
    public void setParentOccupation(String parentOccupation) { this.parentOccupation = parentOccupation; }

    public String getParentRelation() { return parentRelation; }
    public void setParentRelation(String parentRelation) { this.parentRelation = parentRelation; }

    public Map<String, Object> getFeeSummary() { return feeSummary; }
    public void setFeeSummary(Map<String, Object> feeSummary) { this.feeSummary = feeSummary; }

    public TestPerformanceDTO getTestPerformance() { return testPerformance; }
    public void setTestPerformance(TestPerformanceDTO testPerformance) { this.testPerformance = testPerformance; }

    public int getTotalEnrollments() { return totalEnrollments; }
    public void setTotalEnrollments(int totalEnrollments) { this.totalEnrollments = totalEnrollments; }
}
