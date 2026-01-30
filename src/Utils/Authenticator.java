package Utils;

import Models.User;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Authenticator {

    public static User Authenticate(String phoneNumber, String password) {
        if (phoneNumber == null || password == null || phoneNumber.isEmpty() || password.isEmpty()) {
            return null;
        }

        User user = Global.mainUsersMap.get(phoneNumber);
        if (user != null && user.getPassword().equals(password) && !user.isDeleted()) {
            Global.user = user;
            return user;
        }

        return null;
    }

    public static void SignUp(String mobile,String password,String firstname,String lastname, File photo) {
        String photoPath = processProfileImage(photo, firstname, lastname);

        User newUser = new User(mobile, password, firstname, lastname);
        newUser.getProfile().setPhoto(Objects.requireNonNullElse(photoPath, "default_profile.png"));

        Global.mainUsersMap.put(mobile, newUser);
    }

    private static String processProfileImage(File imageFile, String firstName, String lastName) {
        if (imageFile == null) return null;

        try {
            File imageDir = new File("ProfileImages");
            if (!imageDir.exists()) imageDir.mkdir();

            String ext = getFileExtension(imageFile.getName());
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String newFileName = firstName + "_" + lastName + "_" + timestamp + "." + ext;

            Path destination = Paths.get(imageDir.getPath(), newFileName);
            Files.copy(imageFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            return newFileName;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }
}
