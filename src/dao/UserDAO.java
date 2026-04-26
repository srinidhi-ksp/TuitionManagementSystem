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

    // -------------------------------
    // Login User
    // -------------------------------
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

    // ==========================
    // ➕ ADD USER
    // ==========================
    public boolean addUser(User user) {
        if (userCollection == null) return false;
        try {
            Document doc = DocumentMapper.userToDocument(user);
            userCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==========================
    // 🔍 GET USER BY ID
    // ==========================
    public User getUserById(String userId) {
        if (userCollection == null) return null;
        try {
            Document doc = userCollection.find(Filters.eq("_id", userId)).first();
            return DocumentMapper.documentToUser(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==========================
    // 📅 GET CREATED_AT by user _id (for teacher T001 lookup)
    // ==========================
    public java.util.Date getCreatedAt(String userId) {
        if (userCollection == null) return null;
        try {
            org.bson.Document doc = userCollection.find(
                com.mongodb.client.model.Filters.eq("_id", userId)
            ).first();
            if (doc != null) return doc.getDate("created_at");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==========================
    // 📧 GET CREATED_AT by email (most reliable — email matches across users/students/teachers)
    // ==========================
    public java.util.Date getCreatedAtByEmail(String email) {
        if (userCollection == null || email == null) return null;
        try {
            org.bson.Document doc = userCollection.find(
                Filters.eq("email", email)
            ).first();
            if (doc != null) return doc.getDate("created_at");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==========================
    // ❌ DELETE USER
    // ==========================
    public boolean deleteUser(String userId) {
        if (userCollection == null) return false;
        try {
            long deletedCount = userCollection.deleteOne(Filters.eq("_id", userId)).getDeletedCount();
            return deletedCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}