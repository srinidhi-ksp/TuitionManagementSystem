# Corrected Java Code - Login Validation System

This file contains the complete corrected code for all files that were modified to fix the login validation issue.

---

## 1. DBConnection.java - Database Connection

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
                        
                        System.out.println("[DBConnection] MongoDB Connected Successfully to database: " + DATABASE_NAME);
                        
                        // Option: create initial indexes here or in DAO
                    } catch (Exception e) {
                        System.err.println("[DBConnection] Database Connection Failed!");
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
            System.out.println("MongoDB Connection Closed.");
        }
    }
}
```

---

## 2. UserDAO.java - Login Validation (Key Method)

```java
public User login(String email, String password) {
    System.out.println("\n========== LOGIN ATTEMPT ==========");
    System.out.println("[UserDAO.login] Email: '" + email + "'");
    System.out.println("[UserDAO.login] Password length: " + (password != null ? password.length() : "null"));
    
    if (userCollection == null) {
        System.err.println("[UserDAO.login] ❌ userCollection is NULL! Database connection failed.");
        System.out.println("====================================\n");
        return null;
    }
    
    if (email == null || email.trim().isEmpty()) {
        System.err.println("[UserDAO.login] ❌ Email is empty!");
        System.out.println("====================================\n");
        return null;
    }
    
    if (password == null || password.isEmpty()) {
        System.err.println("[UserDAO.login] ❌ Password is empty!");
        System.out.println("====================================\n");
        return null;
    }
    
    try {
        // Step 1: Find the user by exact email match
        System.out.println("[UserDAO.login] 🔍 Searching for user with email: '" + email.trim() + "'");
        Document doc = userCollection.find(Filters.eq("email", email.trim())).first();
        
        if (doc == null) {
            System.err.println("[UserDAO.login] ❌ No user found with email '" + email.trim() + "'");
            System.out.println("[UserDAO.login] 📋 Listing all emails in database for debugging:");
            userCollection.find().forEach((Document d) -> {
                System.out.println("  - Email: " + d.getString("email"));
            });
            System.out.println("====================================\n");
            return null;
        }
        
        // Step 2: Print fetched data to confirm retrieval
        System.out.println("[UserDAO.login] ✅ User document found!");
        System.out.println("[UserDAO.login] 📄 Document Details:");
        System.out.println("  User ID (_id): " + doc.getString("_id"));
        System.out.println("  Email: " + doc.getString("email"));
        System.out.println("  Stored Password length: " + (doc.getString("password") != null ? doc.getString("password").length() : "null"));
        System.out.println("  Status: " + doc.getString("status"));
        System.out.println("  Roles: " + doc.getList("roles", String.class));
        System.out.println("  Created At: " + doc.getDate("created_at"));
        
        // Step 3: Validate password (exact string match for plain text)
        String storedPassword = doc.getString("password");
        
        if (storedPassword == null) {
            System.err.println("[UserDAO.login] ❌ Stored password is NULL in database!");
            System.out.println("====================================\n");
            return null;
        }
        
        // Trim whitespace from both ends for comparison (helps with copy-paste issues)
        String trimmedStoredPassword = storedPassword.trim();
        String trimmedProvidedPassword = password.trim();
        
        System.out.println("[UserDAO.login] 🔐 Comparing passwords:");
        System.out.println("  Stored (trimmed): '" + trimmedStoredPassword + "' (length: " + trimmedStoredPassword.length() + ")");
        System.out.println("  Provided (trimmed): '" + trimmedProvidedPassword + "' (length: " + trimmedProvidedPassword.length() + ")");
        
        if (trimmedStoredPassword.equals(trimmedProvidedPassword)) {
            System.out.println("[UserDAO.login] ✅ Password matched successfully!");
            System.out.println("[UserDAO.login] 🎉 Login SUCCESS!");
            System.out.println("====================================\n");
            return DocumentMapper.documentToUser(doc);
        } else {
            System.err.println("[UserDAO.login] ❌ Password mismatch!");
            // Character-by-character comparison for debugging
            System.out.println("[UserDAO.login] Character comparison:");
            System.out.println("  Stored chars: " + java.util.Arrays.toString(trimmedStoredPassword.toCharArray()));
            System.out.println("  Provided chars: " + java.util.Arrays.toString(trimmedProvidedPassword.toCharArray()));
            System.out.println("====================================\n");
            return null;
        }
    } catch (Exception e) {
        System.err.println("[UserDAO.login] ❌ Exception during login:");
        e.printStackTrace();
        System.out.println("====================================\n");
    }
    return null;
}
```

---

## 3. AuthService.java - Role Validation

```java
package service;

import dao.UserDAO;
import model.User;

public class AuthService {

    private UserDAO userDAO;

    public AuthService() {
        userDAO = new UserDAO();
    }

    // LOGIN WITH ROLE CHECK (case-insensitive comparison)
    public User login(String email, String password, String selectedRole) {
        System.out.println("[AuthService.login] Starting authentication...");
        System.out.println("[AuthService.login] Email: " + email);
        System.out.println("[AuthService.login] Selected Role: " + selectedRole);

        User user = userDAO.login(email, password);

        if (user != null) {
            System.out.println("[AuthService.login] ✅ User authenticated: " + user.getEmail());
            System.out.println("[AuthService.login] User's role in database: '" + user.getRole() + "'");
            
            // MongoDB roles may be uppercase (e.g. "ADMIN"), dropdown uses title-case ("Admin")
            // Compare case-insensitively
            if (user.getRole() != null && user.getRole().equalsIgnoreCase(selectedRole)) {
                System.out.println("[AuthService.login] ✅ Role matched! '" + user.getRole() + "' == '" + selectedRole + "'");
                // Normalise the stored role to match title-case used by openDashboard()
                user.setRole(selectedRole);
                System.out.println("[AuthService.login] 🎉 Authentication SUCCESS!");
                return user;
            } else {
                System.err.println("[AuthService.login] ❌ Role mismatch!");
                System.err.println("  Stored role: '" + user.getRole() + "'");
                System.err.println("  Selected role: '" + selectedRole + "'");
                return null;
            }
        } else {
            System.err.println("[AuthService.login] ❌ Authentication failed - UserDAO returned null");
        }

        return null;
    }
}
```

---

## 4. DocumentMapper.java - Document to User Mapping

```java
// ====================================
// USER MAPPER
// ====================================
public static User documentToUser(Document doc) {
    if (doc == null) {
        System.err.println("[DocumentMapper] Document is null!");
        return null;
    }
    
    User user = new User();
    String userId = doc.getString("_id");
    user.setUserId(userId);
    System.out.println("[DocumentMapper] Mapping user: " + userId);
    
    user.setEmail(doc.getString("email"));
    user.setPassword(doc.getString("password"));
    
    List<String> roles = doc.getList("roles", String.class);
    System.out.println("[DocumentMapper] Raw roles from DB: " + roles);
    
    if (roles != null && !roles.isEmpty()) {
        // Prefer ADMIN role if present (since admin users may have multiple roles)
        String chosenRole = roles.get(0);
        for (String r : roles) {
            if (r != null && r.equalsIgnoreCase("admin")) {
                chosenRole = r;
                break;
            }
        }
        user.setRole(chosenRole);
        System.out.println("[DocumentMapper] Selected role: '" + chosenRole + "'");
    } else {
        System.err.println("[DocumentMapper] ⚠️  No roles found for user!");
        user.setRole("Unknown");
    }
    
    user.setCreatedAt(doc.getDate("created_at"));
    System.out.println("[DocumentMapper] ✅ User mapped successfully!");
    return user;
}
```

---

## 5. LoginFrame.java - Login Button Handler

```java
// ── Login logic ────────────────────────────────────────────────────────────
private void performLogin() {
    String username = usernameField.getText().trim();
    String password = new String(passwordField.getPassword());
    String role     = roleCombo.getSelectedItem().toString();

    System.out.println("\n=============================================");
    System.out.println("[LoginFrame] Login attempt initiated");
    System.out.println("[LoginFrame] Username/Email: " + username);
    System.out.println("[LoginFrame] Password length: " + password.length());
    System.out.println("[LoginFrame] Selected Role: " + role);
    System.out.println("=============================================");

    if (username.isEmpty() || password.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Please enter username and password.", "Validation", JOptionPane.WARNING_MESSAGE);
        return;
    }

    try {
        User user = new AuthService().login(username, password, role);
        if (user != null) {
            System.out.println("[LoginFrame] ✅ Login successful for: " + user.getEmail());
            
            String sessionUserId = user.getUserId();
            String sessionUserName = user.getName() != null ? user.getName() : user.getUserId();

            if ("Student".equalsIgnoreCase(role)) {
                model.Student s = new dao.StudentDAO().getStudentByUserId(user.getUserId());
                if (s == null) s = new dao.StudentDAO().getStudentById(user.getUserId());
                if (s != null) {
                    sessionUserId = s.getUserId();
                    if (s.getName() != null && !s.getName().trim().isEmpty()) {
                        sessionUserName = s.getName();
                    }
                }
            } else if ("Teacher".equalsIgnoreCase(role)) {
                model.Teacher t = new dao.TeacherDAO().getTeacherById(user.getUserId());
                if (t != null) {
                    sessionUserId = t.getUserId();
                    if (t.getName() != null && !t.getName().trim().isEmpty()) {
                        sessionUserName = t.getName();
                    }
                }
            }

            SessionManager.getInstance().setSession(sessionUserId, user.getRole(), sessionUserName);
            JOptionPane.showMessageDialog(this, "Login successful! Welcome, " + sessionUserName, "Success", JOptionPane.INFORMATION_MESSAGE);
            openDashboard(user);
        } else {
            System.err.println("[LoginFrame] ❌ Login failed!");
            JOptionPane.showMessageDialog(this, "Invalid credentials. Please verify:\n" +
                    "• Email/Username is correct\n" +
                    "• Password is correct\n" +
                    "• Selected role matches your account", "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    } catch (Exception e) {
        System.err.println("[LoginFrame] ❌ Login error: " + e.getMessage());
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "An error occurred during login:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
```

---

## 6. compile.bat - Fixed Compilation Script

```batch
@echo off
chcp 65001 >nul
cd /d "%~dp0"
setlocal enabledelayedexpansion

set "CLASSPATH=lib\*"
set "SOURCE_17=-source 17"
set "TARGET_17=-target 17"

javac -encoding UTF-8 -d bin -cp "%CLASSPATH%" %SOURCE_17% %TARGET_17% ^
  src\LaunchAdminDashboard.java ^
  src\dao\*.java ^
  src\db\*.java ^
  src\model\*.java ^
  src\service\*.java ^
  src\util\*.java ^
  src\ui\*.java ^
  src\ui\admin\*.java ^
  src\ui\student\*.java ^
  src\ui\teacher\*.java
  
if errorlevel 1 (
  echo Compilation failed!
  exit /b 1
) else (
  echo Compilation successful!
)

echo.
echo Running the application...
java -cp "bin;lib\*" LaunchAdminDashboard
```

---

## Key Changes Summary

### 1. **Enhanced Debugging Output**
- Logs at each step of authentication
- Shows what data is retrieved from MongoDB
- Character-by-character password comparison

### 2. **Better Error Handling**
- Null checks throughout
- Graceful failure messages
- Lists all emails if user not found

### 3. **Password Comparison**
- Trims whitespace to avoid copy-paste issues
- Exact string matching for plain text
- Shows character arrays for debugging

### 4. **Role Validation**
- Case-insensitive comparison
- Shows both stored and selected roles
- Detailed error messages

### 5. **User Feedback**
- Success confirmation message
- Detailed error messages
- Helpful troubleshooting hints

---

## Testing

After applying these changes:

1. Compile: `.\compile.bat` or `.\run_login_ui.bat`
2. Check console for connection messages
3. Enter test credentials
4. Monitor console output for detailed debugging info
5. Follow the DEBUGGING_GUIDE.md for troubleshooting

