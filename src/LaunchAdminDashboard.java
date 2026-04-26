import ui.admin.AdminDashboard;
import model.User;
import javax.swing.SwingUtilities;

public class LaunchAdminDashboard {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            User admin = new User();
            admin.setUserId("admin");
            admin.setRole("Admin");
            admin.setName("Super Admin");
            new AdminDashboard(admin).setVisible(true);
        });
    }
}
