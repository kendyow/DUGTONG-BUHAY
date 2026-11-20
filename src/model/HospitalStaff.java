package model;

public class HospitalStaff extends User {
    private final String hospitalName;
    private final Location hospitalLocation;

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

    /** Creates an emergency request tied to this staff member's hospital. */
    public BloodRequest postRequest(String patientCode, BloodType bloodTypeNeeded,
                                    int unitsNeeded, UrgencyLevel urgency) {
        return new BloodRequest(patientCode, bloodTypeNeeded, unitsNeeded,
                urgency, hospitalName, hospitalLocation, getId());
    }
}