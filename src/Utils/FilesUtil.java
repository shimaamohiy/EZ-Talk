package Utils;

import Models.ChatRoom;
import Models.User;

import java.io.*;
import java.util.Map;
import java.util.function.Function;

public class FilesUtil {

    private static final File USERS_FILE = new File("Files/UsersFile.txt");
    private static final File CHATROOMS_FILE = new File("Files/ChatRoomsFile.txt");

    static {
        createIfMissing(USERS_FILE);
        createIfMissing(CHATROOMS_FILE);
    }

    // Ensure file and parent directories exist
    private static void createIfMissing(File file) {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            System.err.println("Error creating file: " + file.getPath());
            e.printStackTrace();
        }
    }

    // Generic read method
    private static <T> void readObjects(File file, Class<T> clazz, Function<T, String> keyMapper, Map<String, T> destinationMap) {
        if (file.length() == 0) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            while (true) {
                try {
                    Object obj = ois.readObject();
                    if (clazz.isInstance(obj)) {
                        T casted = clazz.cast(obj);
                        destinationMap.put(keyMapper.apply(casted), casted);
                    }
                } catch (EOFException e) {
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error reading file: " + file.getPath());
            e.printStackTrace();
        }
    }

    // Generic write method
    private static <T> void writeObjects(File file, Iterable<T> objects) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            for (T obj : objects) {
                oos.writeObject(obj);
            }
        } catch (IOException e) {
            System.err.println("Error writing file: " + file.getPath());
            e.printStackTrace();
        }
    }

    // Public methods for users
    public static void readUsers() {
        readObjects(USERS_FILE, User.class, User::getMobileNumber, Global.mainUsersMap);
    }

    public static void writeUsers() {
        writeObjects(USERS_FILE, Global.mainUsersMap.values());
    }

    // Public methods for chat rooms
    public static void readChatRooms() {
        readObjects(CHATROOMS_FILE, ChatRoom.class, ChatRoom::getChatRoomID, Global.mainChatRooms);
    }

    public static void writeChatRooms() {
        writeObjects(CHATROOMS_FILE, Global.mainChatRooms.values());
    }
}
