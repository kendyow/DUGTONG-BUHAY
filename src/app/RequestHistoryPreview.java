package app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import gui.RequestHistoryPanel;
import model.BloodRequest;
import model.BloodType;
import model.Location;
import model.UrgencyLevel;

/**
 
Opens the request history panel with sample records.*/
public class RequestHistoryPreview {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Request History Preview");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setSize(850, 420);
            window.setMinimumSize(new Dimension(600, 300));
            window.setLocationRelativeTo(null);

            Location location = new Location("Batangas City", 13.7565, 121.0583);

            BloodRequest openRequest = new BloodRequest(
                    "TEST-001",
                    BloodType.A_POS,
                    2,
                    UrgencyLevel.URGENT,
                    "Sample Hospital",
                    location,
                    "S001"
            );

            BloodRequest fulfilledRequest = new BloodRequest(
                    "TEST-002",
                    BloodType.O_NEG,
                    1,
                    UrgencyLevel.ROUTINE,
                    "Sample Hospital",
                    location,
                    "S001"
            );
            fulfilledRequest.fulfill();
List<BloodRequest> sampleRequests =
                    List.of(openRequest, fulfilledRequest);

            RequestHistoryPanel history = new RequestHistoryPanel();
            history.setRequests(sampleRequests);

            JButton showSamples = new JButton("Show sample requests");
            showSamples.addActionListener(
                    event -> history.setRequests(sampleRequests)
            );

            JButton showEmpty = new JButton("Show empty state");
            showEmpty.addActionListener(
                    event -> history.setRequests(List.of())
            );

            JPanel controls = new JPanel();
            controls.add(showSamples);
            controls.add(showEmpty);

            window.add(history, BorderLayout.CENTER);
            window.add(controls, BorderLayout.SOUTH);
            window.setVisible(true);
        });
    }
}