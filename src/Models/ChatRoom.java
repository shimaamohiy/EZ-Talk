package Models;

import Utils.Generator;
import Utils.Global;

import java.io.Serializable;
import java.util.*;

public class ChatRoom implements Serializable {
    private final String ChatRoomID;
    private final String ChatRoomName;
    private final ChatRoomType type;
    private String Admin;
    private final HashSet<String> Users = new HashSet<>();
    private final HashMap<String, Message> messages = new HashMap<>();

    public ChatRoom(ChatRoomType type, String Name, String Admin) {
        ChatRoomName = Name;
        this.Admin = Admin;
        ChatRoomID = Generator.generateId("CR");
        this.type = type;
    }

    public void addMessage(Message msg) {
        messages.put(msg.getMessageID(), msg); // O(1)
    }

    public void removeMessageById(String messageId) {
        messages.remove(messageId); // O(1)
    }

    public Message getMessageByID(String messageId) {
        return messages.get(messageId); // O(1)
    }

    public boolean removeUser(String mobileNumber){
        return this.getUsers().remove(mobileNumber);
    }

    // === Getters ===
    public String getChatRoomID() {
        return ChatRoomID;
    }

    public HashSet<String> getUsers() {
        return Users;
    }

    public HashMap<String, Message> getMessages() {
        return messages;
    }

    public String getChatRoomName() {
        return ChatRoomName;
    }

    public ChatRoomType getType() {
        return type;
    }

    public String getAdmin() {
        return Admin;
    }

    // === Setters ===
    public void setAdmin(String admin){
        Admin = admin;
    }

}
