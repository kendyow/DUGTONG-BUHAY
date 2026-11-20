package model;

public abstract class User {
    private final String id;
    private String name;
    private String contactNumber;

    protected User(String id, String name, String contactNumber) {
        this.id = id;
        setName(name);
        setContactNumber(contactNumber);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getContactNumber() { return contactNumber; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        this.name = name.trim();
    }

    public void setContactNumber(String contactNumber) {
        // Philippine mobile format: 11 digits starting with 09
        if (contactNumber == null || !contactNumber.matches("09\\d{9}")) {
            throw new IllegalArgumentException(
                "Contact number must be 11 digits starting with 09.");
        }
        this.contactNumber = contactNumber;
    }

    /** Each subclass must say what role it plays (polymorphism). */
    public abstract String getRole();

        /** Can this kind of user post blood requests? (polymorphism) */
    public abstract boolean canPostRequests();

    
    @Override
    public String toString() {
        return getRole() + " [" + id + "] " + name;
    }
}