package dao;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import model.UserPhone;
import db.DBConnection;

public class UserPhoneDAO {

    private MongoCollection<Document> usersCollection;

    public UserPhoneDAO() {
        MongoDatabase database = DBConnection.getDatabase();
        if (database != null) {
            usersCollection = database.getCollection("users");
        }
    }

    // -----------------------------
    // Add Phone
    // -----------------------------
    public boolean addPhone(UserPhone phone) {
        if (usersCollection == null) return false;
        try {
            long result = usersCollection.updateOne(
                    Filters.eq("_id", phone.getUserId()),
                    Updates.set("phone", phone.getPhone())
            ).getModifiedCount();
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // -----------------------------
    // Get Phone by User
    // -----------------------------
    public UserPhone getPhoneByUser(String userId) {
        if (usersCollection == null) return null;
        try {
            Document doc = usersCollection.find(Filters.eq("_id", userId)).first();
            if (doc != null) {
                Object phoneObj = doc.get("phone");
                if (phoneObj != null) {
                    UserPhone up = new UserPhone();
                    up.setUserId(userId);
                    if (phoneObj instanceof Number) {
                        up.setPhone(((Number) phoneObj).longValue());
                    } else {
                        up.setPhone(Long.parseLong(phoneObj.toString()));
                    }
                    return up;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // -----------------------------
    // Delete Phone
    // -----------------------------
    public boolean deletePhone(String userId) {
        if (usersCollection == null) return false;
        try {
            long result = usersCollection.updateOne(
                    Filters.eq("_id", userId),
                    Updates.unset("phone")
            ).getModifiedCount();
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
