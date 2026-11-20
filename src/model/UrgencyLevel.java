package model;

public enum UrgencyLevel {
    //        radius(km)  response window (minutes)
    CRITICAL(100,  15),
    URGENT  ( 50,  60),
    ROUTINE ( 25, 1440);   // 24 hours

    private final double searchRadiusKm;
    private final int responseWindowMinutes;

    UrgencyLevel(double searchRadiusKm, int responseWindowMinutes) {
        this.searchRadiusKm = searchRadiusKm;
        this.responseWindowMinutes = responseWindowMinutes;
    }

    public double getSearchRadiusKm() { return searchRadiusKm; }
    public int getResponseWindowMinutes() { return responseWindowMinutes; }
}