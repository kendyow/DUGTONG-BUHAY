package app;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import model.BloodType;
import model.UrgencyLevel;

/**
 * Standalone preview of an emergency request form.
 */
public class EmergencyRequestPreview {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(EmergencyRequestPreview::showWindow);
    }

    private static void showWindow() {
        JFrame window = new JFrame("Emergency Request Preview");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextField hospitalField = new JTextField();
        JTextField patientCodeField = new JTextField();

        JComboBox<BloodType> bloodTypeField =
                new JComboBox<>(BloodType.values());

        JSpinner unitsField =
                new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));

        JComboBox<UrgencyLevel> urgencyField =
                new JComboBox<>(UrgencyLevel.values());

        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 12));
        fields.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        fields.add(new JLabel("Hospital"));
        fields.add(hospitalField);

        fields.add(new JLabel("Patient code"));
        fields.add(patientCodeField);

        fields.add(new JLabel("Blood type needed"));
        fields.add(bloodTypeField);

        fields.add(new JLabel("Units needed"));
        fields.add(unitsField);

        fields.add(new JLabel("Urgency"));
        fields.add(urgencyField);

        JButton reviewButton = new JButton("Review request");
        JButton clearButton = new JButton("Clear");

        JPanel buttons = new JPanel();
        buttons.add(reviewButton);
        buttons.add(clearButton);

        reviewButton.addActionListener(event -> {
            String hospital = hospitalField.getText().trim();
            String patientCode = patientCodeField.getText().trim();

            if (hospital.isEmpty()) {
                JOptionPane.showMessageDialog(
                        window,
                        "Enter the hospital name.",
                        "Hospital required",
                        JOptionPane.WARNING_MESSAGE
                );
                hospitalField.requestFocusInWindow();
                return;
            }

            if (patientCode.isEmpty()) {
                JOptionPane.showMessageDialog(
                        window,
                        "Enter the patient code.",
                        "Patient code required",
                        JOptionPane.WARNING_MESSAGE
                );
                patientCodeField.requestFocusInWindow();
                return;
            }

            String summary =
                    "Hospital: " + hospital
                    + "\nPatient code: " + patientCode
                    + "\nBlood type: " + bloodTypeField.getSelectedItem()
                    + "\nUnits: " + unitsField.getValue()
                    + "\nUrgency: " + urgencyField.getSelectedItem();

            JOptionPane.showMessageDialog(
                    window,
                    summary,
                    "Review request",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        clearButton.addActionListener(event -> {
            hospitalField.setText("");
            patientCodeField.setText("");
            bloodTypeField.setSelectedIndex(0);
            unitsField.setValue(1);
            urgencyField.setSelectedIndex(0);
            hospitalField.requestFocusInWindow();
        });

        window.setLayout(new BorderLayout());
        window.add(fields, BorderLayout.CENTER);
        window.add(buttons, BorderLayout.SOUTH);
        window.pack();
        window.setMinimumSize(window.getSize());
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}