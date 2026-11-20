package model;

public class Location {
    // Rough bounding box of the Philippines
    private static final double MIN_LAT = 4.0,   MAX_LAT = 22.0;
    private static final double MIN_LON = 116.0, MAX_LON = 128.0;

    private final String name;       // e.g., "Batangas City"
    private final double latitude;
    private final double longitude;

    public Location(String name, double latitude, double longitude) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Location name cannot be empty.");
        }
        if (latitude < MIN_LAT || latitude > MAX_LAT) {
            throw new IllegalArgumentException(
                "Latitude must be within the Philippines (" + MIN_LAT + " to " + MAX_LAT + ").");
        }
        if (longitude < MIN_LON || longitude > MAX_LON) {
            throw new IllegalArgumentException(
                "Longitude must be within the Philippines (" + MIN_LON + " to " + MAX_LON + ").");
        }
        this.name = name.trim();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    public double distanceKmTo(Location other) {
        return GeoUtils.distanceKm(latitude, longitude,
                                   other.latitude, other.longitude);
    }

    @Override
    public String toString() {
        return name + " (" + latitude + ", " + longitude + ")";
    }
}