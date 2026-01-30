package Models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRoomInfo implements Serializable {
    private String userID;
    private String chatRoomID;
    private LocalDateTime lastSeen;
    Map<String, List<ChatRoomInfo>> chatRoomInfos = new HashMap<>();
    // Map<ChatRoomID, List<Models.ChatRoomInfo>>

    public ChatRoomInfo(String userID, String chatRoomID) {
        this.userID = userID;
        this.chatRoomID = chatRoomID;
        this.lastSeen = LocalDateTime.now();
    }

    // === Update ===
    public void updateLastSeen() {
        this.lastSeen = LocalDateTime.now();
    }

    public void updateUserLastSeen(String userID, String chatRoomID) {
        List<ChatRoomInfo> infos = chatRoomInfos.get(chatRoomID);
        if (infos != null) {
            for (ChatRoomInfo info : infos) {
                if (info.getUserID().equals(userID)) {
                    info.updateLastSeen();
                    System.out.println("Last seen updated for " + userID + " in " + chatRoomID);
                    return;
                }
            }
        }
    }

    // === Getters ===
    public String getUserID() {
        return userID;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public String getChatRoomID() {
        return chatRoomID;
    }

    public LocalDateTime getUserLastSeen(String userID, String chatRoomID) {
        List<ChatRoomInfo> infos = chatRoomInfos.get(chatRoomID);
        if (infos != null) {
            for (ChatRoomInfo info : infos) {
                if (info.getUserID().equals(userID)) {
                    return info.getLastSeen();
                }
            }
        }
        return null; // Not found
    }

    public Map<String, List<ChatRoomInfo>> getChatRoomInfos() {
        return chatRoomInfos;
    }
    // === Setters ===

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public void setChatRoomID(String chatRoomID) {
        this.chatRoomID = chatRoomID;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void setChatRoomInfos(Map<String, List<ChatRoomInfo>> chatRoomInfos) {
        this.chatRoomInfos = chatRoomInfos;
    }
}