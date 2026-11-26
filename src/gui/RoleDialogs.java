package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import app.ConsoleMenu;
import app.ConsoleMenu.Role;
import app.InputReader;
import auth.AccountAccess;
import gui.forms.DonorLoginPanel;
import gui.forms.EmergencyRequestForm;
import service.DataStore;
import service.Registry;

/** Presents the shared role menus in modal Swing dialogs. */
final class RoleDialogs {
    private static final class EndSession extends RuntimeException {}

    private final Component owner;
    private final ConsoleMenu menu;
    private final AccountAccess accounts;
    private final StringWriter pending = new StringWriter();
    private String title;
    private boolean choosingDonorAction;
    private String donorId;
    private char[] donorPassword;

    RoleDialogs(Component owner, Registry registry, DataStore store, AccountAccess accounts) {
        this.owner = owner;
        this.accounts = accounts;
        PrintWriter out = new PrintWriter(pending, true);
        InputReader input = new InputReader(out, this::prompt, this::promptPassword);
        input.setRequestForm(staff -> EmergencyRequestForm.show(owner, staff));
        input.setRequestHistory(requests -> {
            takeOutput();
            RequestHistoryPanel history = new RequestHistoryPanel();
            history.setRequests(requests);
            JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(owner), "Request history", true);
            dialog.setContentPane(history);
            dialog.setSize(850, 420);
            dialog.setLocationRelativeTo(owner);
            dialog.setVisible(true);
            dialog.dispose();
        });
        menu = new ConsoleMenu(registry, store, accounts, input, out);
    }

    void open(String title, Role role) {
        this.title = title;
        choosingDonorAction = role == Role.DONOR;
        try {
            menu.runRole(role);
        } catch (EndSession ignored) {
            // Cancel closes the current role session and returns to the public window.
        } finally {
            choosingDonorAction = false;
            donorId = null;
            if (donorPassword != null) {
                Arrays.fill(donorPassword, '\0');
                donorPassword = null;
            }
        }
        String text = takeOutput();
        if (!text.isBlank()) {
            JOptionPane.showMessageDialog(owner, outputView(text), title, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String prompt(String prompt) {
        if (choosingDonorAction) {
            choosingDonorAction = false;
            takeOutput();
            Object[] options = { "Sign in", "Register", "Cancel" };
            int choice = JOptionPane.showOptionDialog(owner, "Choose how to continue.", title,
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice == 1) {
                return "1";
            }
            if (choice != 0) {
                throw new EndSession();
            }
            showDonorLogin();
            return "2";
        }
        if (donorId != null && prompt.equals("Account ID: ")) {
            String id = donorId;
            donorId = null;
            return id;
        }
        JTextField answer = new JTextField(24);
        showPrompt(prompt, answer);
        return answer.getText();
    }

    private char[] promptPassword(String prompt) {
        if (donorPassword != null && prompt.equals("Password: ")) {
            char[] password = donorPassword;
            donorPassword = null;
            return password;
        }
        JPasswordField answer = new JPasswordField(24);
        try {
            showPrompt(prompt, answer);
            return answer.getPassword();
        } finally {
            answer.setText("");
        }
    }

    private void showDonorLogin() {
        JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(owner), title, true);
        DonorLoginPanel[] panel = new DonorLoginPanel[1];
        panel[0] = new DonorLoginPanel((id, password) -> {
            boolean accepted = accounts.login(id, password)
                    .filter(login -> "Donor".equals(login.role())).isPresent();
            if (!accepted) {
                panel[0].showMessage("Sign-in failed. Check your ID and password.");
                return;
            }
            donorId = id;
            donorPassword = password.clone();
            dialog.dispose();
        }, dialog::dispose);
        dialog.setContentPane(panel[0]);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        dialog.dispose();
        if (donorPassword == null) {
            throw new EndSession();
        }
    }

    private void showPrompt(String prompt, JTextField answer) {
        JPanel content = new JPanel(new BorderLayout(0, 8));
        String text = takeOutput();
        if (!text.isBlank()) {
            content.add(outputView(text), BorderLayout.CENTER);
        }
        JPanel field = new JPanel(new BorderLayout(0, 4));
        field.add(new JLabel(prompt), BorderLayout.NORTH);
        field.add(answer, BorderLayout.CENTER);
        content.add(field, BorderLayout.SOUTH);

        JOptionPane pane = new JOptionPane(content, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog(owner, title);
        try {
            dialog.addWindowFocusListener(new WindowAdapter() {
                @Override
                public void windowGainedFocus(WindowEvent event) {
                    answer.requestFocusInWindow();
                }
            });
            answer.addActionListener(e -> pane.setValue(JOptionPane.OK_OPTION));
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
        if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue())) {
            throw new EndSession();
        }
    }

    private String takeOutput() {
        String text = pending.toString().strip();
        pending.getBuffer().setLength(0);
        return text;
    }

    private JScrollPane outputView(String text) {
        JTextArea area = new JTextArea(text, Math.min(16, (int) text.lines().count()), 64);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setCaretPosition(0);
        return new JScrollPane(area);
    }
}
