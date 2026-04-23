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