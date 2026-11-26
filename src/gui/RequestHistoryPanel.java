package gui;

import java.awt.BorderLayout;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import model.BloodRequest;

/**
 
Displays a read-only list of hospital blood requests.*/
public class RequestHistoryPanel extends JPanel {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final DefaultTableModel tableModel;
    private final JLabel emptyMessage;

    public RequestHistoryPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Hospital request history");
        add(title, BorderLayout.NORTH);

        String[] columns = {
            "Request ID",
            "Blood type",
            "Units",
            "Urgency",
            "Status",
            "Created"
        };
tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        add(new JScrollPane(table), BorderLayout.CENTER);

        emptyMessage = new JLabel("No requests to display.");
        add(emptyMessage, BorderLayout.SOUTH);
    }

    /**
     
Replaces the rows with the supplied requests.*/
  public void setRequests(List<BloodRequest> requests) {
      tableModel.setRowCount(0);

        for (BloodRequest request : requests) {
            tableModel.addRow(new Object[] {
                request.getRequestId(),
                request.getBloodTypeNeeded(),
                request.getUnitsNeeded(),
                request.getUrgency(),
                request.getStatus(),
                request.getCreatedAt().format(DATE_FORMAT)
            });
        }

        emptyMessage.setVisible(requests.isEmpty());
    }
}