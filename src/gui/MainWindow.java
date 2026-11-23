package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import app.ConsoleMenu.Role;
import auth.AccountAccess;
import service.DataStore;
import service.Registry;

/** Public search and stock tabs with access to the three role workflows. */
public class MainWindow extends JFrame {

    public MainWindow(Registry registry, DataStore store, AccountAccess accounts) {
        super("DUGTONG-BUHAY - Rare Blood Donor Matching and Emergency Response Network");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        FinderPanel finderPanel = new FinderPanel(registry, store);
        StockPanel stockPanel = new StockPanel(registry);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Find Blood Nearby", finderPanel);
        tabs.addTab("Hospital Stock Board", stockPanel);
        tabs.addChangeListener(e -> {
            stockPanel.refresh();
            finderPanel.refresh();
        });

        JLabel footer = new JLabel("Privacy: this public view shows hospitals only. "
                + "No donor information is displayed.");
        footer.setBorder(new EmptyBorder(4, 10, 6, 10));

        setLayout(new BorderLayout());
        RoleDialogs roles = new RoleDialogs(this, registry, store, accounts);
        JPanel roleBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addRoleButton(roleBar, "Hospital Staff", Role.STAFF, roles, finderPanel, stockPanel);
        addRoleButton(roleBar, "Donor", Role.DONOR, roles, finderPanel, stockPanel);
        addRoleButton(roleBar, "Admin", Role.ADMIN, roles, finderPanel, stockPanel);
        add(roleBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        // Fit small laptop screens (for example 1366 x 768) as well as large ones
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1100, screen.width - 40), Math.min(760, screen.height - 80));
        setLocationRelativeTo(null);
    }
    private void addRoleButton(JPanel bar, String label, Role role, RoleDialogs roles,
            FinderPanel finder, StockPanel stock) {
        JButton button = new JButton(label);
        button.addActionListener(e -> {
            roles.open(label, role);
            finder.refreshAfterRole();
            stock.refresh();
        });
        bar.add(button);
    }
}
