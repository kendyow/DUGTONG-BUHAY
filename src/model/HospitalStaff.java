package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HospitalStaff extends User implements Notifiable {
    private final String hospitalName;
    private final Location hospitalLocation;
    private final List<String> inbox = new ArrayList<>();

    public HospitalStaff(String id, String name, String contactNumber,
                         String hospitalName, Location hospitalLocation) {
        super(id, name, contactNumber);
        if (hospitalName == null || hospitalName.isBlank()) {
            throw new IllegalArgumentException("Hospital name is required.");
        }
        if (hospitalLocation == null) {
            throw new IllegalArgumentException("Hospital location is required.");
        }
        this.hospitalName = hospitalName.trim();
        this.hospitalLocation = hospitalLocation;
    }

    @Override
    public String getRole() {
        return "Hospital Staff";
    }

    @Override
    public boolean canPostRequests() {
        return true;
    }

    public String getHospitalName() { return hospitalName; }
    public Location getHospitalLocation() { return hospitalLocation; }

    public BloodRequest postRequest(String patientCode, BloodType bloodTypeNeeded,
                                    int unitsNeeded, UrgencyLevel urgency) {
        return new BloodRequest(patientCode, bloodTypeNeeded, unitsNeeded,
                urgency, hospitalName, hospitalLocation, getId());
    }

    @Override
    public void receiveAlert(String message) {
        inbox.add(message);
    }

    @Override
    public List<String> getInbox() {
        return Collections.unmodifiableList(inbox);
    }
}