package model;

import java.util.Date;
import java.util.List;

public class User {

    private String userId;
    private String name;
    private String email;
    private String password;
    private String role;  // Primary role for backwards compatibility
    private List<String> roles;  // Array of all roles from MongoDB
    private String address;
    private String phone;
    private Date createdAt;

    // ✅ Default constructor
    public User() {}

    // ✅ SIMPLE constructor (used in login)
    public User(String userId, String email, String password, String role) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // ✅ FULL constructor (used by Student, Teacher, Parent)
    public User(String userId, String name, String email, String password,
                String role, String address, Date createdAt) {

        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.address = address;
        this.createdAt = createdAt;
    }

    // =====================
    // GETTERS & SETTERS
    // =====================

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    // ✅ Roles (Array/List) - NEW METHODS
    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    // Check if user has a specific role (case-insensitive)
    public boolean hasRole(String roleToCheck) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String r : roles) {
            if (r != null && r.equalsIgnoreCase(roleToCheck)) {
                return true;
            }
        }
        return false;
    }
}