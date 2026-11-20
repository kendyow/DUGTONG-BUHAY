package service;

import java.util.List;

import model.Location;

public class PlaceCatalog {
    private static final List<Location> PLACES = List.of(
            new Location("Batangas City", 13.7565, 121.0583),
            new Location("Lipa City",     13.9411, 121.1631),
            new Location("Lemery",        13.8847, 120.9100),
            new Location("Tagaytay",      14.1153, 120.9621),
            new Location("Calamba",       14.2117, 121.1653),
            new Location("Manila",        14.5995, 120.9842),
            new Location("Quezon City",   14.6760, 121.0437),
            new Location("Lucena",        13.9373, 121.6171),
            new Location("Cebu City",     10.3157, 123.8854),
            new Location("Davao City",     7.1907, 125.4553));

    private PlaceCatalog() { }

    public static List<Location> getAll() {
        return PLACES;
    }

    public static Location find(String name) {
        for (Location place : PLACES) {
            if (place.getName().equalsIgnoreCase(name)) {
                return place;
            }
        }
        throw new IllegalArgumentException("Unknown place: " + name);
    }
}