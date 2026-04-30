package model;

public class Parent extends User {

    private String preferredLanguage;
    private String occupation;
    private double annualIncome;
    private long emergencyContact;
    private String relationType;
    private java.util.List<String> linkedStudentIds;

    // Default Constructor
    public Parent() {
        super();
        this.linkedStudentIds = new java.util.ArrayList<>();
    }

    // Parameterized Constructor
    public Parent(String userId, String name, String email, String password, String role, String address, java.util.Date createdAt,
                  String preferredLanguage, String occupation, double annualIncome,
                  long emergencyContact, String relationType, java.util.List<String> linkedStudentIds) {

        super(userId, name, email, password, role, address, createdAt);

        this.preferredLanguage = preferredLanguage;
        this.occupation = occupation;
        this.annualIncome = annualIncome;
        this.emergencyContact = emergencyContact;
        this.relationType = relationType;
        this.linkedStudentIds = linkedStudentIds != null ? linkedStudentIds : new java.util.ArrayList<>();
    }

    // Getters and Setters

    public java.util.List<String> getLinkedStudentIds() {
        return linkedStudentIds;
    }

    public void setLinkedStudentIds(java.util.List<String> linkedStudentIds) {
        this.linkedStudentIds = linkedStudentIds;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public double getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(double annualIncome) {
        this.annualIncome = annualIncome;
    }

    public long getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(long emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }
}