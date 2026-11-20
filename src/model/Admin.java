package model;

public class Admin extends User {

    public Admin(String id, String name, String contactNumber) {
        super(id, name, contactNumber);
    }

    @Override
    public String getRole() {
        return "Admin";
    }

    @Override
    public boolean canPostRequests() {
        return false;
    }

    /** Privacy tool: stops all alerts to this donor. */
    public void forceWithdrawConsent(Donor donor) {
        if (donor == null) {
            throw new IllegalArgumentException("Donor cannot be null.");
        }
        donor.withdrawConsent();
    }
}