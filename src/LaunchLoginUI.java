import ui.LoginFrame;
import javax.swing.SwingUtilities;

public class LaunchLoginUI {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}
