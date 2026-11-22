package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import app.ConsoleMenu;
import app.ConsoleMenu.Role;
import app.InputReader;
import service.DataStore;
import service.Registry;

/** Presents the shared role menus in modal Swing dialogs. */
final class RoleDialogs {
    private static final class EndSession extends RuntimeException {}

    private final Component owner;
    private final ConsoleMenu menu;
    private final StringWriter pending = new StringWriter();
    private String title;

    RoleDialogs(Component owner, Registry registry, DataStore store) {
        this.owner = owner;
        PrintWriter out = new PrintWriter(pending, true);
        menu = new ConsoleMenu(registry, store, new InputReader(out, this::prompt), out);
    }

    void open(String title, Role role) {
        this.title = title;
        try {
            menu.runRole(role);
        } catch (EndSession ignored) {
            // Cancel closes the current role session and returns to the public window.
        }
        String text = takeOutput();
        if (!text.isBlank()) {
            JOptionPane.showMessageDialog(owner, outputView(text), title, JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String prompt(String prompt) {
        JPanel content = new JPanel(new BorderLayout(0, 8));
        String text = takeOutput();
        if (!text.isBlank()) {
            content.add(outputView(text), BorderLayout.CENTER);
        }
        JTextField answer = new JTextField(24);
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
        return answer.getText();
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
