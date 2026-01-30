package Models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class UserProfile implements Serializable {
    private String about;
    private String photo;
    private LocalDateTime lastSeen;
    private boolean visibleToAll;

    public UserProfile(String about) {
        this.about = about;
        this.photo = "";
        this.visibleToAll = true;
    }

    public boolean isVisibleToAll() {
        return visibleToAll;
    }

    // === Getters ===

    public String getAbout() {
        return about;
    }

    public String getPhoto() {
        return photo;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }

    // === Setters ===
    public void setAbout(String about) {
        this.about = about;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public void setVisibility(boolean visible) {
        this.visibleToAll = visible;
    }

    @Override
    public String toString() {
        return "About: " + about + ", Photo: " + photo;
    }
}
