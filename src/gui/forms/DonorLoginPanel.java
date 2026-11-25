package gui.forms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Collects donor sign-in details and passes them to the supplied handler.
 */
public class DonorLoginPanel extends JPanel {
    /**
     * Receives the account ID and password when the user selects Sign in.
     * The password array is cleared after this method returns.
     */
    @FunctionalInterface
    public interface SignInHandler {
        void signIn(String accountId, char[] password);
    }

    private final JTextField accountIdField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);
    private final JLabel messageLabel = new JLabel(" ");

    public DonorLoginPanel(SignInHandler signInHandler, Runnable cancelAction) {
        Objects.requireNonNull(signInHandler, "Sign-in handler is required.");
        Objects.requireNonNull(cancelAction, "Cancel action is required.");

        setLayout(new BorderLayout());
        setBackground(new Color(248, 248, 248));
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 225)),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 18, 0);

        JLabel title = new JLabel("Donor sign in");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(new Color(170, 30, 45));
        form.add(title, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 6, 0);
        JLabel accountLabel = new JLabel("Account ID");
        accountLabel.setLabelFor(accountIdField);
        form.add(accountLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 14, 0);
        form.add(accountIdField, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 6, 0);
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setLabelFor(passwordField);
        form.add(passwordLabel, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 10, 0);
        form.add(passwordField, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 14, 0);
        messageLabel.setForeground(new Color(160, 25, 35));
        form.add(messageLabel, constraints);

        JButton signInButton = new JButton("Sign in");
        JButton cancelButton = new JButton("Cancel");

        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.weightx = 0.5;
        constraints.insets = new Insets(0, 0, 0, 6);
        form.add(signInButton, constraints);

        constraints.gridx = 1;
        constraints.insets = new Insets(0, 6, 0, 0);
        form.add(cancelButton, constraints);

        signInButton.addActionListener(event -> {
            String accountId = accountIdField.getText().trim();
            char[] password = passwordField.getPassword();

            try {
                if (accountId.isEmpty()) {
                    showMessage("Enter your account ID.");
                    accountIdField.requestFocusInWindow();
                    return;
                }
                if (password.length == 0) {
                    showMessage("Enter your password.");
                    passwordField.requestFocusInWindow();
                    return;
                }

                showMessage(" ");
                signInHandler.signIn(accountId, password);
            } finally {
                Arrays.fill(password, '\0');
                passwordField.setText("");
            }
        });

        passwordField.addActionListener(event -> signInButton.doClick());

        cancelButton.addActionListener(event -> {
            passwordField.setText("");
            cancelAction.run();
        });

        add(form, BorderLayout.CENTER);
    }

    /**
     * Displays feedback supplied by the window using this panel.
     */
    public void showMessage(String message) {
        messageLabel.setText(message);
    }
}