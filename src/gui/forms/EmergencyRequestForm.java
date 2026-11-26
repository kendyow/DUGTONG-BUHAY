package gui.forms;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import app.InputReader.RequestDetails;
import model.BloodType;
import model.HospitalStaff;
import model.UrgencyLevel;

/** Collects and confirms a request for the authenticated staff member's hospital. */
public final class EmergencyRequestForm {
    private EmergencyRequestForm() {}

    public static RequestDetails show(Component owner, HospitalStaff staff) {
        JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(owner), "New blood request", true);
        JTextField patientCode = new JTextField();
        JComboBox<BloodType> bloodType = new JComboBox<>(BloodType.values());
        JSpinner units = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        JComboBox<UrgencyLevel> urgency = new JComboBox<>(UrgencyLevel.values());
        RequestDetails[] result = new RequestDetails[1];

        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 12));
        fields.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        fields.add(new JLabel("Hospital"));
        fields.add(new JLabel(staff.getHospitalName()));
        fields.add(new JLabel("Patient code"));
        fields.add(patientCode);
        fields.add(new JLabel("Blood type needed"));
        fields.add(bloodType);
        fields.add(new JLabel("Units needed (1–10)"));
        fields.add(units);
        fields.add(new JLabel("Urgency"));
        fields.add(urgency);

        JButton review = new JButton("Review request");
        JButton cancel = new JButton("Cancel");
        JPanel buttons = new JPanel();
        buttons.add(review);
        buttons.add(cancel);

        review.addActionListener(event -> {
            String code = patientCode.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Enter the patient code.");
                patientCode.requestFocusInWindow();
                return;
            }
            try {
                units.commitEdit();
                int count = (Integer) units.getValue();
                if (count < 1 || count > 10) {
                    throw new ParseException("Units out of range", 0);
                }
                BloodType type = (BloodType) bloodType.getSelectedItem();
                UrgencyLevel level = (UrgencyLevel) urgency.getSelectedItem();
                String summary = "Hospital: " + staff.getHospitalName()
                        + "\nPatient code: " + code + "\nBlood type: " + type
                        + "\nUnits: " + count + "\nUrgency: " + level
                        + "\n\nConfirm and create this request?";
                if (JOptionPane.showConfirmDialog(dialog, summary, "Review request",
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    result[0] = new RequestDetails(code, type, count, level);
                    dialog.dispose();
                }
            } catch (ParseException | IllegalArgumentException e) {
                JOptionPane.showMessageDialog(dialog, "Enter a whole number of units from 1 to 10.");
                units.requestFocusInWindow();
            }
        });
        cancel.addActionListener(event -> dialog.dispose());
        dialog.add(fields, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        dialog.dispose();
        return result[0];
    }
}
