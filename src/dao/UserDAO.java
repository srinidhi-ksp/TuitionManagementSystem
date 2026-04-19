package dao;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import model.User;
import db.DBConnection;
import db.DocumentMapper;

public class UserDAO {
    private MongoCollection<Document> userCollection;

    public UserDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            userCollection = database.getCollection("users");
        }
    }

    // -------------------------------
    // Login User
    // -------------------------------
    public User login(String email, String password) {
        if (userCollection == null) return null;
        try {
            Document doc = userCollection.find(
                Filters.and(
                    Filters.eq("email", email),
                    Filters.eq("password", password)
                )
            ).first();

            return DocumentMapper.documentToUser(doc);
        } catch (Exception e) {
            e.printStackTrace();
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