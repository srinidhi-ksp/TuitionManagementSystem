package model;

import java.util.Date;
import java.util.List;

public class Teacher extends User {

    private String specialization;
    private Date joinDate;
    private String adminId;

    private int doorNo;
    private String street;
    private String city;
    private long pincode;
    
    // New MongoDB fields
    private List<String> qualifications;
    private Salary salary;

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
                   String specialization, Date joinDate, String adminId,
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

    public Date getJoinDate() { return joinDate; }
    public void setJoinDate(Date joinDate) { this.joinDate = joinDate; }

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

    public Salary getSalary() { return salary; }
    public void setSalary(Salary salary) { this.salary = salary; }

    @Override
    public String toString() {
        return getUserId() + " - " + (getName() != null ? getName() : "Teacher");
    }
}