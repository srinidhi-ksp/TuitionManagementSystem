package model;

import java.util.List;

public class Subject {

    private int subjectId;
    private String subjectName;
    private String category;
    private String status;
    private String syllabusVersion;
    private String description;
    private double monthlyFee;
    
    private List<Chapter> chapters;

    public static class Chapter {
        private int chapterId;
        private String name;
        private String difficulty;

        public Chapter() {}

        public Chapter(int chapterId, String name, String difficulty) {
            this.chapterId = chapterId;
            this.name = name;
            this.difficulty = difficulty;
        }

        public int getChapterId() { return chapterId; }
        public void setChapterId(int chapterId) { this.chapterId = chapterId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    }

    // Default Constructor
    public Subject() {
    }

    // Parameterized Constructor
    public Subject(int subjectId, String subjectName, String category,
                   String status, String syllabusVersion,
                   String description, double monthlyFee) {

        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.category = category;
        this.status = status;
        this.syllabusVersion = syllabusVersion;
        this.description = description;
        this.monthlyFee = monthlyFee;
    }

    // Getters and Setters

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSyllabusVersion() { return syllabusVersion; }
    public void setSyllabusVersion(String syllabusVersion) { this.syllabusVersion = syllabusVersion; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(double monthlyFee) { this.monthlyFee = monthlyFee; }

    public List<Chapter> getChapters() { return chapters; }
    public void setChapters(List<Chapter> chapters) { this.chapters = chapters; }

    @Override
    public String toString() {
        return "Subject [subjectId=" + subjectId +
                ", subjectName=" + subjectName +
                ", category=" + category +
                ", status=" + status +
                ", syllabusVersion=" + syllabusVersion +
                ", description=" + description +
                ", monthlyFee=" + monthlyFee + "]";
    }
}