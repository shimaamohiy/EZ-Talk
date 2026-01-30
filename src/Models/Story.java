package Models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Story implements Serializable {
    private final User Owner;
    private final String text;
    private final String photoPath;
    private final LocalDateTime publishTime;
    private final Map<User, LocalDateTime> viewers = new LinkedHashMap<>();

    public Story(User user, String text, String photoPath) {
        this.Owner = user;
        this.text = text;
        this.photoPath = photoPath;
        this.publishTime = LocalDateTime.now();  // Story's publish time is the current time
    }

    public LocalDateTime getPublishedTime() {
        return publishTime;
    }

    public void markStoryAsViewed(User viewer) {
        viewers.putIfAbsent(viewer, LocalDateTime.now());
    }

    public Map<User, LocalDateTime> getViewers() {
        return viewers;
    }

    public int getViewersCount() {
        return viewers.size();  // Return the total number of viewers
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public String getStoryText() {
        return text;
    }

    public User getOwner() {
        return Owner;
    }

    public String getPhotoPath() {
        return photoPath;
    }


}
