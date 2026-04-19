package model;

import java.util.Date;

public class Student extends User {

    private Date joinDate;
    private String currentStd;
    private String board;
    private Date dob;

    private int doorNo;
    private String street;
    private String city;
    private long pincode;
    
    private Parent parent; // Nested embedded object (backward compat)

    // ── Flat parent convenience fields (populated from student.parent in MongoDB) ──
    private String parentId;
    private String parentName;
    private String parentPhone;
    private String parentOccupation;
    private String parentRelation = "Father"; // default

    // Default Constructor
    public Student() {
        super();   // calls User()
    }

    // Parameterized Constructor (Constructor Chaining)
    public Student(String userId, String name, String email, String password, String role, String address, Date createdAt,
                   Date joinDate, String currentStd, String board, Date dob,
                   int doorNo, String street, String city, long pincode) {

        super(userId, name, email, password, role, address, createdAt);

        this.joinDate = joinDate;
        this.currentStd = currentStd;
        this.board = board;
        this.dob = dob;
        this.doorNo = doorNo;
        this.street = street;
        this.city = city;
        this.pincode = pincode;
    }

    // Getters and Setters

    public Date getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }

    public String getCurrentStd() {
        return currentStd;
    }

    public void setCurrentStd(String currentStd) {
        this.currentStd = currentStd;
    }

    public String getBoard() {
        return board;
    }

    public void setBoard(String board) {
        this.board = board;
    }

    public Date getDob() {
        return dob;
    }

    public void setDob(Date dob) {
        this.dob = dob;
    }

    public int getDoorNo() {
        return doorNo;
    }

    public void setDoorNo(int doorNo) {
        this.doorNo = doorNo;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public long getPincode() {
        return pincode;
    }

    public void setPincode(long pincode) {
        this.pincode = pincode;
    }

    public Parent getParent() {
        return parent;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
    }

    // ── Flat parent getters/setters ──

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getParentName() { return parentName != null ? parentName : "N/A"; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getParentPhone() { return parentPhone != null ? parentPhone : "N/A"; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }

    public String getParentOccupation() { return parentOccupation != null ? parentOccupation : "N/A"; }
    public void setParentOccupation(String parentOccupation) { this.parentOccupation = parentOccupation; }

    public String getParentRelation() { return parentRelation != null ? parentRelation : "Father"; }
    public void setParentRelation(String parentRelation) { this.parentRelation = parentRelation; }
}