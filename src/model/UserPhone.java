package model;

public class UserPhone {

    private String userId;
    private long phone;

    // Default Constructor
    public UserPhone() {
    }

    // Parameterized Constructor
    public UserPhone(String userId, long phone) {
        this.userId = userId;
        this.phone = phone;
    }

    // Getter for userId
    public String getUserId() {
        return userId;
    }

    // Setter for userId
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Getter for phone
    public long getPhone() {
        return phone;
    }

    // Setter for phone
    public void setPhone(long phone) {
        this.phone = phone;
    }

    // toString method

    @Override
    public String toString() {
        return "UserPhone [userId=" + userId + ", phone=" + phone + "]";
    }
}