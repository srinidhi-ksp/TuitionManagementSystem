# Complete Corrected Java Code - Login System Works for ALL Users

---

## 1. DBConnection.java - MongoDB Connection

```java
package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class DBConnection {

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "tuitionManagementSystem";
    
    private static MongoClient mongoClient = null;

    // Private constructor to prevent instantiation
    private DBConnection() {}

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            synchronized (DBConnection.class) {
                if (mongoClient == null) {
                    try {
                        System.out.println("[DBConnection] Attempting to connect to MongoDB at: " + CONNECTION_STRING);
                        mongoClient = MongoClients.create(CONNECTION_STRING);
                        
                        // Verify connection with a ping
                        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
                        database.runCommand(new org.bson.Document("ping", 1));
                        
                        System.out.println("[DBConnection] ✅ MongoDB Connected Successfully to database: " + DATABASE_NAME);
                        
                    } catch (Exception e) {
                        System.err.println("[DBConnection] ❌ Database Connection Failed!");
                        e.printStackTrace();
                        mongoClient = null; // Reset on failure
                    }
                }
            }
        }
        if (mongoClient != null) {
            return mongoClient.getDatabase(DATABASE_NAME);
        }
        return null;
    }
    
    // Optional utility to close the connection on shutdown
    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("[DBConnection] MongoDB Connection Closed.");
        }
    }
}
```

**What this does:**
- ✅ Connects to MongoDB at localhost:27017
- ✅ Uses singleton pattern (one connection per JVM)
- ✅ Verifies connection with ping test
- ✅ Returns null if connection fails (safe)
- ✅ Works for ANY database with proper name

---

## 2. UserDAO.java - Login Query (CRITICAL)

```java
package dao;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import db.DBConnection;
import db.DocumentMapper;
import model.User;

public class UserDAO {
    private MongoCollection<Document> userCollection;

    public UserDAO() {
        try {
            MongoDatabase database = DBConnection.getDatabase();
            if (database != null) {
                userCollection = database.getCollection("users");
                System.out.println("[UserDAO] ✅ Connected to 'users' collection");
            } else {
                System.err.println("[UserDAO] ❌ Database connection returned null!");
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] ❌ Error initializing UserDAO: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================
    // LOGIN - Works for ANY user in database
    // =========================================
    public User login(String email, String password) {
        System.out.println("\n========== LOGIN ATTEMPT ==========");
        System.out.println("[UserDAO.login] Input Email: '" + email + "'");
        System.out.println("[UserDAO.login] Input Password length: " + (password != null ? password.length() : "null"));
        
        // Validation: Check collection
        if (userCollection == null) {
            System.err.println("[UserDAO.login] ❌ ERROR: userCollection is NULL!");
            System.err.println("[UserDAO.login]    Database connection failed!");
            System.out.println("====================================\n");
            return null;
        }
        
        // Validation: Check email input
        if (email == null || email.trim().isEmpty()) {
            System.err.println("[UserDAO.login] ❌ ERROR: Email input is empty!");
            System.out.println("====================================\n");
            return null;
        }
        
        // Validation: Check password input
        if (password == null || password.isEmpty()) {
            System.err.println("[UserDAO.login] ❌ ERROR: Password input is empty!");
            System.out.println("====================================\n");
            return null;
        }
        
        try {
            // ============================================
            // STEP 1: Query MongoDB by email (DYNAMIC)
            // ============================================
            String trimmedEmail = email.trim();
            System.out.println("[UserDAO.login] 🔍 Searching database for email: '" + trimmedEmail + "'");
            
            // Query MongoDB: { email: "user@example.com" }
            Document doc = userCollection.find(Filters.eq("email", trimmedEmail)).first();
            
            if (doc == null) {
                System.err.println("[UserDAO.login] ❌ User NOT found with email: '" + trimmedEmail + "'");
                System.out.println("[UserDAO.login] 📋 Listing all emails in database:");
                
                // List all emails for debugging
                userCollection.find().forEach((Document d) -> {
                    String dbEmail = d.getString("email");
                    System.out.println("    - " + dbEmail);
                });
                System.out.println("====================================\n");
                return null;
            }
            
            // ============================================
            // STEP 2: Validate password
            // ============================================
            System.out.println("[UserDAO.login] ✅ User document FOUND!");
            System.out.println("[UserDAO.login] 📄 User Details:");
            System.out.println("    User ID (_id): " + doc.getString("_id"));
            System.out.println("    Email: " + doc.getString("email"));
            System.out.println("    Status: " + doc.getString("status"));
            System.out.println("    Roles: " + doc.getList("roles", String.class));
            
            String storedPassword = doc.getString("password");
            
            if (storedPassword == null) {
                System.err.println("[UserDAO.login] ❌ ERROR: Password field is NULL in database!");
                System.out.println("====================================\n");
                return null;
            }
            
            // ============================================
            // STEP 3: Compare passwords (trim both)
            // ============================================
            String trimmedStoredPassword = storedPassword.trim();
            String trimmedInputPassword = password.trim();
            
            System.out.println("[UserDAO.login] 🔐 Password Comparison:");
            System.out.println("    Stored password (trimmed): '" + trimmedStoredPassword + "'");
            System.out.println("    Input password (trimmed):  '" + trimmedInputPassword + "'");
            System.out.println("    Stored length: " + trimmedStoredPassword.length());
            System.out.println("    Input length:  " + trimmedInputPassword.length());
            
            if (trimmedStoredPassword.equals(trimmedInputPassword)) {
                System.out.println("[UserDAO.login] ✅ Password MATCHES!");
                System.out.println("[UserDAO.login] 🎉 Login SUCCESSFUL!");
                System.out.println("====================================\n");
                return DocumentMapper.documentToUser(doc);
            } else {
                System.err.println("[UserDAO.login] ❌ Password MISMATCH!");
                System.err.println("    Stored: " + java.util.Arrays.toString(trimmedStoredPassword.toCharArray()));
                System.err.println("    Input:  " + java.util.Arrays.toString(trimmedInputPassword.toCharArray()));
                System.out.println("====================================\n");
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("[UserDAO.login] ❌ EXCEPTION during login:");
            e.printStackTrace();
            System.out.println("====================================\n");
            return null;
        }
    }

    // Other DAO methods...
}
```

**What this does (CRITICAL for ALL users):**
- ✅ Queries by email dynamically: `Filters.eq("email", trimmedEmail)`
- ✅ NO hardcoded emails - works for ANY user
- ✅ Trims whitespace on input email
- ✅ Lists all emails if user not found (debugging)
- ✅ Validates password character-by-character
- ✅ Returns null if password mismatch
- ✅ Complete debug logging

---

## 3. User.java - Model with Roles Array Support

```java
package model;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class User {

    private String userId;
    private String name;
    private String email;
    private String password;
    private String role;              // Primary role (backward compat)
    private List<String> roles;       // ✅ Roles array (NEW)
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

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // ✅ NEW: Roles Array (for MongoDB support)
    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    // ✅ NEW: Check if user has specific role (case-insensitive)
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
```

**What this does:**
- ✅ Stores roles as array: `List<String> roles`
- ✅ Supports multiple roles per user
- ✅ Method `hasRole()` checks if role in array (case-insensitive)
- ✅ Backward compatible with single role

---

## 4. DocumentMapper.java - Map MongoDB to Java

```java
package db;

import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import model.User;

public class DocumentMapper {

    // ====================================
    // USER MAPPER - From MongoDB to Java
    // ====================================
    public static User documentToUser(Document doc) {
        if (doc == null) {
            System.err.println("[DocumentMapper] ERROR: Document is null!");
            return null;
        }
        
        User user = new User();
        String userId = doc.getString("_id");
        user.setUserId(userId);
        System.out.println("[DocumentMapper] Mapping user: " + userId);
        
        user.setEmail(doc.getString("email"));
        user.setPassword(doc.getString("password"));
        
        // ✅ CRITICAL: Get roles as ARRAY (not single value)
        List<String> roles = doc.getList("roles", String.class);
        System.out.println("[DocumentMapper] Raw roles from MongoDB: " + roles);
        
        if (roles != null && !roles.isEmpty()) {
            // Store complete roles array
            user.setRoles(roles);
            
            // Set primary role: prefer ADMIN, else first role
            String primaryRole = roles.get(0);
            for (String r : roles) {
                if (r != null && r.equalsIgnoreCase("admin")) {
                    primaryRole = r;
                    break;
                }
            }
            user.setRole(primaryRole);
            System.out.println("[DocumentMapper] All roles: " + roles);
            System.out.println("[DocumentMapper] Primary role: '" + primaryRole + "'");
        } else {
            System.err.println("[DocumentMapper] WARNING: No roles in document!");
            user.setRole("Unknown");
            user.setRoles(new ArrayList<>());
        }
        
        user.setCreatedAt(doc.getDate("created_at"));
        System.out.println("[DocumentMapper] ✅ User mapped successfully!");
        return user;
    }

    // ... rest of mapper code ...
}
```

**What this does:**
- ✅ Gets roles as list: `doc.getList("roles", String.class)`
- ✅ Stores complete array in user object
- ✅ Sets primary role intelligently
- ✅ Works for any role configuration

---

## 5. AuthService.java - Role Validation

```java
package service;

import dao.UserDAO;
import model.User;

public class AuthService {

    private UserDAO userDAO;

    public AuthService() {
        userDAO = new UserDAO();
    }

    // ✅ LOGIN - Dynamic role checking
    public User login(String email, String password, String selectedRole) {
        System.out.println("\n[AuthService.login] ========================================");
        System.out.println("[AuthService.login] Starting authentication...");
        System.out.println("[AuthService.login] Email: " + email);
        System.out.println("[AuthService.login] Selected Role: " + selectedRole);

        // Step 1: Authenticate with email & password
        User user = userDAO.login(email, password);

        if (user != null) {
            System.out.println("[AuthService.login] ✅ User authenticated: " + user.getEmail());
            System.out.println("[AuthService.login] User ID: " + user.getUserId());
            System.out.println("[AuthService.login] Available roles: " + user.getRoles());
            
            // ✅ CRITICAL: Check role in array (case-insensitive)
            if (user.hasRole(selectedRole)) {
                System.out.println("[AuthService.login] ✅ Role check PASSED!");
                System.out.println("[AuthService.login]    User has role: '" + selectedRole + "'");
                
                user.setRole(selectedRole);
                System.out.println("[AuthService.login] 🎉 Authentication SUCCESS!");
                System.out.println("[AuthService.login] ========================================\n");
                return user;
            } else {
                System.err.println("[AuthService.login] ❌ Role check FAILED!");
                System.err.println("[AuthService.login]    User does NOT have role: '" + selectedRole + "'");
                System.err.println("[AuthService.login]    Available: " + user.getRoles());
                System.err.println("[AuthService.login] ========================================\n");
                return null;
            }
        } else {
            System.err.println("[AuthService.login] ❌ Authentication failed!");
            System.err.println("[AuthService.login] ========================================\n");
        }

        return null;
    }
}
```

**What this does:**
- ✅ Uses `user.hasRole()` for array checking
- ✅ Case-insensitive role matching
- ✅ Works for multiple roles
- ✅ Complete debug output

---

## Summary: What Makes This Work for ALL Users

| Component | How It's Dynamic |
|-----------|-----------------|
| **Email Query** | `Filters.eq("email", inputEmail)` - queries by user input |
| **Password** | `storedPassword.equals(inputPassword)` - any password works |
| **Roles Array** | `user.hasRole(selectedRole)` - checks array, not single value |
| **Role Match** | Case-insensitive `equalsIgnoreCase()` - "TEACHER" = "Teacher" |
| **Logging** | Shows exactly what's in DB vs what was input |

---

## Common Issues Fixed

✅ **Issue: Roles stored as array but code expects string**
- Fixed: Now handles `List<String> roles` properly

✅ **Issue: Case sensitivity in roles**
- Fixed: Uses `equalsIgnoreCase()` for matching

✅ **Issue: Extra whitespace on input**
- Fixed: Trims both email and password

✅ **Issue: NULL pointer on missing password**
- Fixed: Checks `if (storedPassword == null)` before using

✅ **Issue: Only works for one example user**
- Fixed: Completely dynamic - no hardcoding

