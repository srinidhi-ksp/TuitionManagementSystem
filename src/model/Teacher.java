package model;

import java.util.Date;
import java.util.List;

public class Teacher extends User {

    private String specialization;
    private String joinDate;
    private String adminId;

    private int doorNo;
    private String street;
    private String city;
    private long pincode;

    // New MongoDB fields
    private List<String> qualifications;
    private Salary legacySalary; // legacy nested salary object

    private int experience;
    private double salary;
    private String highestDegree;
    private String status;        // ACTIVE / INACTIVE

    public static class Salary {
        private double baseSalary;
        private int workingDays;

        public Salary() {}

        public Salary(double baseSalary, int workingDays) {
            this.baseSalary = baseSalary;
            this.workingDays = workingDays;
        }

        public double getBaseSalary() { return baseSalary; }
        public void setBaseSalary(double baseSalary) { this.baseSalary = baseSalary; }
        public int getWorkingDays() { return workingDays; }
        public void setWorkingDays(int workingDays) { this.workingDays = workingDays; }
    }

    // Default Constructor
    public Teacher() {
        super();
    }

    // Parameterized Constructor
    public Teacher(String userId, String name, String email, String password, String role, String address, Date createdAt,
                   String specialization, String joinDate, String adminId,
                   int doorNo, String street, String city, long pincode) {

        super(userId, name, email, password, role, address, createdAt);

        this.specialization = specialization;
        this.joinDate = joinDate;
        this.adminId = adminId;
        this.doorNo = doorNo;
        this.street = street;
        this.city = city;
        this.pincode = pincode;
    }

    // Getters and Setters

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public int getDoorNo() { return doorNo; }
    public void setDoorNo(int doorNo) { this.doorNo = doorNo; }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public long getPincode() { return pincode; }
    public void setPincode(long pincode) { this.pincode = pincode; }

    public List<String> getQualifications() { return qualifications; }
    public void setQualifications(List<String> qualifications) { this.qualifications = qualifications; }

    public Salary getLegacySalary() { return legacySalary; }
    public void setLegacySalary(Salary legacySalary) { this.legacySalary = legacySalary; }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    public String getHighestDegree() { return highestDegree; }
    public void setHighestDegree(String highestDegree) { this.highestDegree = highestDegree; }

    public String getStatus() { return status != null ? status : "ACTIVE"; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return getUserId() + " - " + (getName() != null ? getName() : "Teacher");
    }
}