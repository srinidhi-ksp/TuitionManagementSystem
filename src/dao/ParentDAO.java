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
     * Fetch parent by user_id with Email and Phone from users collection
     */
    public Parent getByUserId(String userId) {
        if (parentCollection == null) return null;
        try {
            Document doc = parentCollection.find(Filters.eq("user_id", userId)).first();
            if (doc == null) {
                doc = parentCollection.find(Filters.eq("_id", userId)).first();
            }
            if (doc == null) return null;

            Parent p = DocumentMapper.documentToParent(doc);

            // ✅ JOIN WITH USERS COLLECTION FOR EMAIL & PHONE
            MongoDatabase db = DBConnection.getDatabase();
            Document userDoc = db.getCollection("users").find(Filters.eq("_id", userId)).first();
            if (userDoc != null) {
                p.setEmail(userDoc.getString("email"));
                
                String phone = userDoc.getString("phone");
                if (phone == null || phone.isBlank()) {
                    List<String> phones = userDoc.getList("phones", String.class);
                    if (phones != null && !phones.isEmpty()) phone = phones.get(0);
                }
                p.setPhone(phone);
            }

            return p;
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

    /**
     * Get all parents with phone numbers cross-referenced from users collection.
     * Phone is stored in users.phones[] array, linked via parents.user_id = users._id
     */
    public List<Parent> getAllParentsWithPhone() {
        List<Parent> list = new ArrayList<>();
        if (parentCollection == null) return list;
        try {
            MongoDatabase db = DBConnection.getDatabase();
            MongoCollection<Document> usersCollection = db.getCollection("users");

            for (Document doc : parentCollection.find()) {
                Parent p = DocumentMapper.documentToParent(doc);
                if (p == null) continue;

                // Step 1: Get phone from users collection via user_id
                Object userIdObj = doc.get("user_id");
                String userId = userIdObj != null ? userIdObj.toString() : null;
                String phone = "—";

                if (userId != null) {
                    Document userDoc = usersCollection.find(
                        Filters.eq("_id", userId)
                    ).first();

                    if (userDoc != null) {
                        // Check 'phone' field (string)
                        phone = userDoc.getString("phone");
                        
                        // Fallback to 'phones' array (list)
                        if (phone == null || phone.isBlank()) {
                            java.util.List<String> phones = userDoc.getList("phones", String.class);
                            if (phones != null && !phones.isEmpty()) {
                                phone = phones.get(0);
                            }
                        }
                    }
                }

                // Step 2: Fallback — search students collection for embedded parent phone
                if ("—".equals(phone) || phone == null) {
                    MongoCollection<Document> studentsCol = db.getCollection("students");
                    Document studentDoc = studentsCol.find(
                        Filters.or(
                            Filters.eq("parent_user_id", userId),
                            Filters.eq("parent.user_id", userId)
                        )
                    ).first();
                    if (studentDoc != null) {
                        Document parentSub = studentDoc.get("parent", Document.class);
                        if (parentSub != null) {
                            String fallbackPhone = parentSub.getString("phone");
                            if (fallbackPhone != null && !fallbackPhone.isBlank()) {
                                phone = fallbackPhone;
                            }
                        }
                    }
                }

                // Step 3: Set phone on Parent object
                p.setPhone(phone); 

                list.add(p);
            }
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error in getAllParentsWithPhone: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Update parent name, occupation, and annual_income.
     */
    public boolean updateParent(Parent p) {
        if (parentCollection == null) return false;
        try {
            org.bson.conversions.Bson filter = com.mongodb.client.model.Filters.eq("user_id", p.getUserId());
            Document update = new Document("$set", new Document()
                .append("name", p.getName())
                .append("occupation", p.getOccupation())
                .append("annual_income", p.getAnnualIncome())
                .append("updated_at", new java.util.Date()));
            long modified = parentCollection.updateOne(filter, update).getModifiedCount();
            System.out.println("[ParentDAO] updateParent modified=" + modified);
            return modified >= 0; // even 0 is OK if nothing changed
        } catch (Exception e) {
            System.err.println("[ParentDAO] Error updating parent: " + e.getMessage());
            return false;
        }
    }
}

