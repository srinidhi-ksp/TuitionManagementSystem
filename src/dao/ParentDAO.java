package dao;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import db.DBConnection;
import db.DocumentMapper;
import model.Parent;

/**
 * Parent DAO - Handles all parent-related database operations
 */
public class ParentDAO {

    private MongoCollection<Document> parentCollection;

    public ParentDAO() {
        try {
            MongoDatabase database = DBConnection.getDatabase();
            if (database != null) {
                parentCollection = database.getCollection("parents");
                System.out.println("[ParentDAO] ✅ Connected to 'parents' collection");
            } else {
                System.err.println("[ParentDAO] ❌ Database connection failed!");
            }
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error initializing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Fetch parent by user_id
     */
    public Parent getByUserId(String userId) {
        if (parentCollection == null) return null;
        try {
            Document doc = parentCollection.find(Filters.eq("user_id", userId)).first();
            if (doc == null) {
                // Fallback to searching by _id
                doc = parentCollection.find(Filters.eq("_id", userId)).first();
            }
            return DocumentMapper.documentToParent(doc);
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error fetching parent by user_id: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get parent by email
     */
    public Parent getByEmail(String email) {
        if (parentCollection == null) return null;
        try {
            Document doc = parentCollection.find(Filters.eq("email", email)).first();
            return DocumentMapper.documentToParent(doc);
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error fetching parent by email: " + e.getMessage());
            return null;
        }
    }

    /**
     * Add a new parent record
     */
    public boolean addParent(Parent p) {
        if (parentCollection == null) return false;
        try {
            Document doc = DocumentMapper.parentToDocument(p);
            parentCollection.insertOne(doc);
            return true;
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error adding parent: " + e.getMessage());
            return false;
        }
    }

    public List<Parent> getAllParents() {
        List<Parent> list = new ArrayList<>();
        if (parentCollection == null) return list;
        try {
            for (Document doc : parentCollection.find()) {
                list.add(DocumentMapper.documentToParent(doc));
            }
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error fetching all parents: " + e.getMessage());
        }
        return list;
    }
}
