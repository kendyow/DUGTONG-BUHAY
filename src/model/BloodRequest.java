package model;

import java.time.LocalDateTime;

public class BloodRequest {

    public enum Status { OPEN, FULFILLED, CANCELLED }

    private static final int MIN_UNITS = 1;
    private static final int MAX_UNITS = 10;
    private static int counter = 0;   // shared by ALL requests

    private final String requestId;
    private final String patientCode;
    private final BloodType bloodTypeNeeded;
    private final int unitsNeeded;
    private final UrgencyLevel urgency;
    private final String hospitalName;
    private final Location hospitalLocation;
    private final String requestedByStaffId;
    private final LocalDateTime createdAt;
    private Status status;

    public BloodRequest(String patientCode, BloodType bloodTypeNeeded,
                        int unitsNeeded, UrgencyLevel urgency,
                        String hospitalName, Location hospitalLocation,
                        String requestedByStaffId) {
        if (patientCode == null || patientCode.isBlank()) {
            throw new IllegalArgumentException("Patient code is required.");
        }
        if (bloodTypeNeeded == null) {
            throw new IllegalArgumentException("Blood type needed is required.");
        }
        if (unitsNeeded < MIN_UNITS || unitsNeeded > MAX_UNITS) {
            throw new IllegalArgumentException(
                "Units needed must be between " + MIN_UNITS + " and " + MAX_UNITS + ".");
        }
        if (urgency == null) {
            throw new IllegalArgumentException("Urgency level is required.");
        }
        counter++;
        this.requestId = String.format("REQ-%03d", counter);
        this.patientCode = patientCode.trim();
        this.bloodTypeNeeded = bloodTypeNeeded;
        this.unitsNeeded = unitsNeeded;
        this.urgency = urgency;
        this.hospitalName = hospitalName;
        this.hospitalLocation = hospitalLocation;
        this.requestedByStaffId = requestedByStaffId;
        this.createdAt = LocalDateTime.now();
        this.status = Status.OPEN;
    }

    public String getRequestId() { return requestId; }
    public String getPatientCode() { return patientCode; }
    public BloodType getBloodTypeNeeded() { return bloodTypeNeeded; }
    public int getUnitsNeeded() { return unitsNeeded; }
    public UrgencyLevel getUrgency() { return urgency; }
    public String getHospitalName() { return hospitalName; }
    public Location getHospitalLocation() { return hospitalLocation; }
    public String getRequestedByStaffId() { return requestedByStaffId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Status getStatus() { return status; }

    /** When donors must respond by. */
    public LocalDateTime getExpiryTime() {
        return createdAt.plusMinutes(urgency.getResponseWindowMinutes());
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(getExpiryTime());
    }

    /** Blood type match using the compatibility rule from BloodType. */
    public boolean isCompatibleWith(Donor donor) {
        return donor.getBloodType().canDonateTo(bloodTypeNeeded);
    }

    /** Is the donor inside this request's urgency-based search radius? */
    public boolean isWithinSearchRadius(Location donorLocation) {
        return hospitalLocation.distanceKmTo(donorLocation)
                <= urgency.getSearchRadiusKm();
    }

    public void fulfill() {
        requireOpen();
        status = Status.FULFILLED;
    }

    public void cancel() {
        requireOpen();
        status = Status.CANCELLED;
    }

    private void requireOpen() {
        if (status != Status.OPEN) {
            throw new IllegalStateException(
                "Request " + requestId + " is already " + status + ".");
        }
    }

    @Override
    public String toString() {
        return requestId + " | Needs " + bloodTypeNeeded + " x" + unitsNeeded
                + " | " + urgency + " | " + hospitalName + " | " + status;
    }
}