package service;

import dao.UserDAO;
import model.User;

public class AuthService {

    private UserDAO userDAO;

    public AuthService() {
        userDAO = new UserDAO();
    }

    // LOGIN WITH ROLE CHECK (case-insensitive comparison)
    public User login(String email, String password, String selectedRole) {

        User user = userDAO.login(email, password);

        if (user != null) {
            // MongoDB roles may be uppercase (e.g. "ADMIN"), dropdown uses title-case ("Admin")
            // Compare case-insensitively
            if (user.getRole() != null && user.getRole().equalsIgnoreCase(selectedRole)) {
                // Normalise the stored role to match title-case used by openDashboard()
                user.setRole(selectedRole);
                return user;
            } else {
                System.out.println("[AuthService] Role mismatch: stored='" + user.getRole()
                        + "' selected='" + selectedRole + "'");
                return null;
            }
        }

        return null;
    }
}