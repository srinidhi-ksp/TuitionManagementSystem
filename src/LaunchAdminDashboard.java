import ui.admin.AdminDashboard;
import javax.swing.SwingUtilities;

public class LaunchAdminDashboard {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AdminDashboard().setVisible(true);
        });
    }
}
