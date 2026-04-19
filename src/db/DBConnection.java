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
                        mongoClient = MongoClients.create(CONNECTION_STRING);
                        System.out.println("MongoDB Connected Successfully to: " + DATABASE_NAME);
                        
                        // Option: create initial indexes here or in DAO
                    } catch (Exception e) {
                        System.err.println("Database Connection Failed.");
                        e.printStackTrace();
                    }
                }
            }
        }
        return mongoClient.getDatabase(DATABASE_NAME);
    }
    
    // Optional utility to close the connection on shutdown
    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB Connection Closed.");
        }
    }
}