package model;

import java.util.List;

public interface Notifiable {
    void receiveAlert(String message);
    List<String> getInbox();
}