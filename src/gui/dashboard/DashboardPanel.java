package gui.dashboard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 
Displays the public dashboard actions.*/
public class DashboardPanel extends JPanel {
    private static final Color BACKGROUND = new Color(248, 248, 248);
    private static final Color RED = new Color(170, 30, 45);
    private static final Color TEXT = new Color(45, 45, 45);

    public DashboardPanel(Runnable openFinder, Runnable openStock) {
        Objects.requireNonNull(openFinder, "openFinder is required");
        Objects.requireNonNull(openStock, "openStock is required");

        setLayout(new BorderLayout(0, 24));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(32, 32, 32, 32));

        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 8));
        heading.setOpaque(false);
JLabel title = new JLabel("DUGTONG-BUHAY");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(RED);

        JLabel subtitle = new JLabel("How can we help you today?");
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 16f));
        subtitle.setForeground(TEXT);

        heading.add(title);
        heading.add(subtitle);

        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 16));
        actions.setOpaque(false);

        actions.add(createAction(
                "Find Blood Nearby",
                "Search participating hospitals for available blood.",
                openFinder
        ));

        actions.add(createAction(
                "Hospital Stock Board",
                "View last-reported hospital blood stock.",
                openStock
        ));

        add(heading, BorderLayout.NORTH);
        add(actions, BorderLayout.CENTER);
    }

    private JPanel createAction(String title, String description, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(16, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 225)),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel text = new JPanel(new GridLayout(0, 1, 0, 8));
        text.setOpaque(false);

        JLabel heading = new JLabel(title);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        heading.setForeground(TEXT);

        JLabel details = new JLabel("<html>" + description + "</html>");
        details.setForeground(new Color(90, 90, 90));

        text.add(heading);
        text.add(details);

        JButton button = new JButton("Open " + title);
        button.setForeground(Color.WHITE);
        button.setBackground(RED);
        button.setFocusPainted(true);
button.setHorizontalAlignment(SwingConstants.CENTER);
        button.addActionListener(event -> action.run());

        card.add(text, BorderLayout.CENTER);
        card.add(button, BorderLayout.SOUTH);
        return card;
    }
}