package model;

public class Parent extends User {

    private String preferredLanguage;
    private String occupation;
    private double annualIncome;
    private long emergencyContact;
    private String relationType;

    // Default Constructor
    public Parent() {
        super();
    }

    // Parameterized Constructor
    public Parent(String userId, String name, String email, String password, String role, String address, java.util.Date createdAt,
                  String preferredLanguage, String occupation, double annualIncome,
                  long emergencyContact, String relationType) {

        super(userId, name, email, password, role, address, createdAt);

        this.preferredLanguage = preferredLanguage;
        this.occupation = occupation;
        this.annualIncome = annualIncome;
        this.emergencyContact = emergencyContact;
        this.relationType = relationType;
    }

    // Getters and Setters

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