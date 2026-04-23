package util;

public class SessionManager {
    private static SessionManager instance;
    private String userId;
    private String role;
    private String userName;
    private String userEmail;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setSession(String userId, String role, String userName, String userEmail) {
        this.userId = userId;
        this.role = role;
        this.userName = userName;
        this.userEmail = userEmail;
    }

    public void clearSession() {
        this.userId = null;
        this.role = null;
        this.userName = null;
        this.userEmail = null;
    }

    public String getUserId() { return userId; }
    public String getRole() { return role; }
    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
}
