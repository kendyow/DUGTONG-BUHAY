package app;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import gui.dashboard.DashboardPanel;

/**
 
Opens the dashboard so its layout and buttons can be reviewed.*/
public class DashboardPreview {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("DUGTONG-BUHAY Dashboard Preview");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setSize(720, 520);
            window.setMinimumSize(new Dimension(420, 400));
            window.setLocationRelativeTo(null);

            JLabel status = new JLabel("Select an action to test the dashboard.");
            status.setBorder(new EmptyBorder(8, 32, 16, 32));

            DashboardPanel dashboard = new DashboardPanel(
                    () -> status.setText("Find Blood Nearby selected."),
                    () -> status.setText("Hospital Stock Board selected.")
            );

            JPanel content = new JPanel(new BorderLayout());
            content.add(dashboard, BorderLayout.CENTER);
            content.add(status, BorderLayout.SOUTH);

            window.setContentPane(content);
            window.setVisible(true);
        });
    }
}