package service;

import dao.UserDAO;
import model.User;

public class AuthService {

    private UserDAO userDAO;

    public AuthService() {
        userDAO = new UserDAO();
    }

    // ✅ LOGIN WITH DYNAMIC ROLE CHECKING (supports multiple roles)
    public User login(String email, String password, String selectedRole) {
        System.out.println("\n[AuthService.login] ========================================");
        System.out.println("[AuthService.login] Starting authentication...");
        System.out.println("[AuthService.login] Email: " + email);
        System.out.println("[AuthService.login] Selected Role: " + selectedRole);

        User user = userDAO.login(email, password);

        if (user != null) {
            System.out.println("[AuthService.login] ✅ User authenticated: " + user.getEmail());
            System.out.println("[AuthService.login] User ID: " + user.getUserId());
            System.out.println("[AuthService.login] User's roles array from DB: " + user.getRoles());
            
            // ✅ Check if user has the selected role (case-insensitive, array-based)
            if (user.hasRole(selectedRole)) {
                System.out.println("[AuthService.login] ✅ Role check PASSED!");
                System.out.println("[AuthService.login]    User has role: '" + selectedRole + "'");
                System.out.println("[AuthService.login]    Available roles: " + user.getRoles());
                
                // Set the selected role as primary role
                user.setRole(selectedRole);
                System.out.println("[AuthService.login] 🎉 Authentication SUCCESS!");
                System.out.println("[AuthService.login] ========================================\n");
                return user;
            } else {
                System.err.println("[AuthService.login] ❌ Role check FAILED!");
                System.err.println("[AuthService.login]    User does NOT have role: '" + selectedRole + "'");
                System.err.println("[AuthService.login]    Available roles: " + user.getRoles());
                System.err.println("[AuthService.login] ========================================\n");
                return null;
            }
        } else {
            System.err.println("[AuthService.login] ❌ Authentication failed - UserDAO returned null");
            System.err.println("[AuthService.login] ========================================\n");
        }

        return null;
    }
}