package parking;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ParkingFrame().setVisible(true));
    }
}
