package db;

import com.mongodb.client.MongoDatabase;

public class TestDB {

    public static void main(String[] args) {

        MongoDatabase db = DBConnection.getDatabase();

        if (db != null) {
            System.out.println("Connection Success. Database name: " + db.getName());
        } else {
            System.out.println("Connection Failed.");
        }

    }
}