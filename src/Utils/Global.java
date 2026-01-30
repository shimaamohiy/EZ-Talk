package Utils;

import Models.ChatRoom;
import Models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Global {
    // Prevent instantiation
    private Global() {}

    public static User user;

    // Static map to store counters per model
    public static final Map<String, User> mainUsersMap = new HashMap<>();

    public static final Map<String, ChatRoom> mainChatRooms = new HashMap<>();

}