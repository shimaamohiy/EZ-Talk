package Controllers;

import Models.User;
import Utils.Authenticator;
import Utils.Global;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.io.File;
import java.io.IOException;

public class LoginCon {

    @FXML private PasswordField password;
    @FXML private TextField phoneNumber;
    @FXML private Pane left;

    private void showAlert(Alert.AlertType alertType, String title, String headerText, String contentText) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();
    }

    public void Authenticate(ActionEvent event) throws IOException {
        String enteredMobileNumber = phoneNumber.getText();
        String enteredPassword = password.getText();

        if (enteredMobileNumber.isEmpty() || enteredPassword.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login Error", "Missing Credentials",
                    "Please enter both username and password.");
            return;
        }

        User user = Authenticator.Authenticate(enteredMobileNumber, enteredPassword);
        if (user != null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Chat.fxml"));
            BorderPane root = loader.load();
            Scene scene = new Scene(root);

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            ChatCon chatCon = loader.getController();
            chatCon.setMyProfile();
            chatCon.loadAllChats();
            chatCon.setStage(currentStage);
            chatCon.setRoot(root);
            currentStage.setTitle("EZ Talk");
            currentStage.setScene(scene);
            currentStage.show();
            return;
        }

        showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid Credentials",
                "The username or password you entered is incorrect. Please try again.");
    }

    public void SignUp(ActionEvent event) throws IOException {
        Stage stage = new Stage();
        stage.setTitle("Sign Up");
        stage.initModality(Modality.APPLICATION_MODAL);

        TextField mobileField = new TextField();
        mobileField.setPromptText("Mobile Number");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");

        Button uploadPhotoButton = new Button("Choose Photo");
        Label photoStatusLabel = new Label("No photo chosen");

        File[] selectedImageFile = new File[1];

        uploadPhotoButton.setOnAction(ev -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Profile Picture");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                selectedImageFile[0] = file;
                photoStatusLabel.setText("Selected: " + file.getName());
            }
        });

        Button signUpButton = new Button("Sign Up");
        Label statusLabel = new Label();

        VBox vbox = new VBox(10,
                mobileField,
                passwordField,
                firstNameField,
                lastNameField,
                uploadPhotoButton,
                photoStatusLabel,
                signUpButton,
                statusLabel
        );

        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #f9f9f9;");
        signUpButton.setPrefWidth(100);

        signUpButton.setOnAction(e -> {
            String mobile = mobileField.getText().trim();
            String password = passwordField.getText().trim();
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();

            if (mobile.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
                statusLabel.setText("All fields are required.");
                return;
            }

            if (!mobile.matches("\\d{11}")) {
                statusLabel.setText("Invalid mobile number.");
                return;
            }

            if (password.length() < 6) {
                statusLabel.setText("Password must be at least 6 characters.");
                return;
            }

            if (Global.mainUsersMap.containsKey(mobile)) {
                User user = Global.mainUsersMap.get(mobile);
                if (user.isDeleted()) {
                    Global.mainUsersMap.remove(mobile);
                } else {
                statusLabel.setText("Phone Number already exists.");
                return;
                }
            }

            Authenticator.SignUp(mobile, password, firstName, lastName, selectedImageFile[0]);

            statusLabel.setText("Account created successfully!");

            PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
            delay.setOnFinished(ev -> stage.close());
            delay.play();
        });

        Scene scene = new Scene(vbox, 300, 370);
        stage.setScene(scene);
        stage.showAndWait();
    }

    @FXML
    private void Hover() {
        left.setOnMouseExited(e -> left.setStyle("-fx-background-color: #EAA276"));
        left.setOnMouseEntered(e -> left.setStyle("-fx-background-color: #eca981"));
    }
}