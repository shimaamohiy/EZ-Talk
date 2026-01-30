package Models;

import Utils.Global;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

public class User implements Serializable {
    private final String mobileNumber;
    private String password;
    private String firstName;
    private String lastName;
    private final Map <String, HashSet<String>> starredMessages = new HashMap<>();
    private HashSet<String> contacts;
    private Queue<Story> stories;
    private UserProfile profile;
    private boolean isDeleted = false;

    public User(String mobileNumber, String password, String firstName, String lastName) {
        this.mobileNumber = mobileNumber;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;

        this.contacts = new HashSet<>();
        this.stories = new LinkedList<>();
        this.profile = new UserProfile("Hey there, I am using EZ Talk.");
    }
    public User(){
        mobileNumber = "00000000000";
    }

    // === Contacts ===
    public String addContact(String contactNumber) {
        if(Global.mainUsersMap.containsKey(contactNumber)){
            User newContact = Global.mainUsersMap.get(contactNumber);
            if (newContact.isDeleted()){
                return "This Contact isn't on EZ Talk.";
            }
            else if(!contacts.contains(contactNumber)){
                contacts.add(contactNumber);
                return "Contact added successfully.";
            }
                return "This Contact already exists";
        }
        else
            return "This Contact isn't on EZ Talk.";
    }

    public boolean isMutualContact(User other) {
        return this.contacts.contains(other.getMobileNumber()) && other.contacts.contains(getMobileNumber());
    }

    public void addStarMessage(String msgID, String roomID){
        HashSet<String> newSet;
        if(starredMessages.containsKey(roomID))
            newSet = starredMessages.get(roomID);
        else
            newSet = new HashSet<>();
        newSet.add(msgID);
        Global.mainChatRooms.get(roomID).getMessages().get(msgID).setStarred(true);
        starredMessages.put(roomID, newSet);
    }

    public void removeStarMessage(String msgID, String roomID){
        HashSet<String> newSet;
        if(starredMessages.containsKey(roomID))
            newSet = starredMessages.get(roomID);
        else
            newSet = new HashSet<>();
        newSet.remove(msgID);
        Global.mainChatRooms.get(roomID).getMessages().get(msgID).setStarred(false);
        starredMessages.put(roomID, newSet);
    }

    // === Stories ===
    public void postStory(String text, String photo) {
        stories.offer(new Story(this, text, photo));
    }

    public void cleanupExpiredStories() {
        while (!stories.isEmpty() && LocalDateTime.now().isAfter(stories.peek().getPublishedTime().plusHours(24))) {
            stories.poll(); // remove expired story
        }
    }

    // === Getters ===
    public HashSet<String> getContacts() {
        return contacts;
    }

    public Queue<Story> getStories() {
        return stories;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Map<String, HashSet<String>> getStarredMessages() {
        return starredMessages;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    // === Setters ===

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setIsDeleted(boolean bool){
        isDeleted = bool;
    }
}
