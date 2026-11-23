package app;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import auth.AccountAccess;
import auth.AccountStore;
import exception.DugtongBuhayException;
import gui.MainWindow;
import service.DataStore;
import service.Registry;
import service.SampleData;

/** Starts the graphical version. Uses the same data/ folder as the console version. */
public class GuiMain {
    public static void main(String[] args) {
        boolean setup = args.length == 2 && "--setup-account".equals(args[0]);
        if (args.length != 0 && !setup) {
            System.out.println("Usage: app.GuiMain [--setup-account ACCOUNT_ID]");
            return;
        }
        Registry registry = new Registry();
        DataStore store = new DataStore("data");
        AccountAccess accounts;

        try {
            if (store.hasSavedData()) {
                store.load(registry);
            } else {
                SampleData.load(registry);
                store.save(registry);
            }
            accounts = new AccountAccess(new AccountStore(Path.of("data")), registry);
        } catch (DugtongBuhayException | IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Startup problem: " + e.getMessage()
                    + "\n\nCheck the files in the data/ folder before restarting.",
                    "DUGTONG-BUHAY", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Swing windows must be created on the "event dispatch thread"
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // if the system look is not available, Java's default look is used
            }
            if (setup) {
                setupAccount(accounts, args[1]);
            } else {
                new MainWindow(registry, store, accounts).setVisible(true);
            }
        });
    }

    private static void setupAccount(AccountAccess accounts, String id) {
        JPasswordField field = new JPasswordField(24);
        JPasswordField confirmation = new JPasswordField(24);
        int choice = JOptionPane.showConfirmDialog(null, new Object[] {
            "Set the first password for " + id + ".",
            "New password (at least 15 characters)", field,
            "Confirm password", confirmation
        }, "Account Setup", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        char[] password = field.getPassword();
        char[] repeated = confirmation.getPassword();
        field.setText("");
        confirmation.setText("");
        try {
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }
            if (!Arrays.equals(password, repeated)) {
                throw new IllegalArgumentException("The passwords did not match.");
            }
            accounts.enroll(id, password);
            JOptionPane.showMessageDialog(null, "Password saved. Open DUGTONG-BUHAY and sign in as " + id + ".");
        } catch (IOException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Account Setup", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(repeated, '\0');
        }
    }
}
