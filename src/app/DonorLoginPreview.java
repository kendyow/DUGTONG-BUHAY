package app;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import gui.forms.DonorLoginPanel;

/**
 
Opens the donor sign-in form for a visual review.*/
public class DonorLoginPreview {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Donor Sign-in Preview");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setSize(500, 420);
            window.setMinimumSize(new Dimension(380, 360));
            window.setLocationRelativeTo(null);

            DonorLoginPanel panel = new DonorLoginPanel(
                    (accountId, password) -> JOptionPane.showMessageDialog(
                            window,
                            "The form received account ID: " + accountId,
                            "Preview",
                            JOptionPane.INFORMATION_MESSAGE
                    ),
                    window::dispose
            );

            window.setContentPane(panel);
            window.setVisible(true);
        });
    }
}