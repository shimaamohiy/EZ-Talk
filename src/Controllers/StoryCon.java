package Controllers;

import Models.*;
import Utils.Global;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class StoryCon {
    private User user;
    private Story currentStory;

    @FXML private Button deleteStory;
    @FXML private Button storyViewers;
    @FXML private VBox storiesBox;
    @FXML private Label Name, storyText;
    @FXML private ImageView storyView;

    private static final String BASE_STORY_STYLE = "-fx-background-color: #0F3338; -fx-background-radius: 8;";
    private static final String HOVER_STORY_STYLE = "-fx-background-color: #1E6670; -fx-background-radius: 8;";
    private static final String STORY_IMAGES_DIR = "StoryImages";
    private static final String DEFAULT_IMAGE_PATH = "/StoryImages/default_profile.png";
    private static final int MAX_STORY_TEXT_LENGTH = 500;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public StoryCon() {
        createDirectoryIfNotExists(STORY_IMAGES_DIR);
    }


    //    === Setters ===
    public void setUser(User newUser) {
        this.user = newUser;
        user.cleanupExpiredStories();
        loadAllStories();
    }
    //================================

    private void createDirectoryIfNotExists(String dir) {
        File directory = new File(dir);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    @FXML
    public void back(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Chat.fxml"));
            BorderPane root = loader.load();
            Scene scene = new Scene(root);

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            ChatCon chatCon = loader.getController();
            chatCon.setMyProfile();
            chatCon.loadAllChats();
            chatCon.setStage(currentStage);
            currentStage.setTitle("EZ Talk");
            currentStage.setScene(scene);
            currentStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Navigation Error",
                    "Could not return to chat screen: " + e.getMessage());
        }
    }

    //    === Load ===
    public void loadAllStories() {
        storiesBox.getChildren().clear();
        user.getStories().forEach(this::setStory);
        user.getContacts().stream().map(Global.mainUsersMap::get)
                .filter(contact -> contact != null && user.isMutualContact(contact))
                .forEach(contact -> {
                    contact.cleanupExpiredStories();
                    contact.getStories().forEach(this::setStory);
                });
    }

    public void setStory(Story story) {
        HBox storyItem = createStoryItem(story);
        storyItem.setOnMouseClicked(e -> loadStory(story));
        storiesBox.getChildren().add(storyItem);
    }

    public void loadStory(Story story) {
        try {
            Image image = loadStoryImage(story.getPhotoPath());

            Name.setText(story.getOwner().getFirstName());
            storyView.setImage(image);
            storyText.setText(story.getStoryText() != null ? story.getStoryText() : "");

            setButtonsVisible(story.getOwner().equals(user));
            story.markStoryAsViewed(user);
            currentStory = story;
        } catch (Exception e) {
            System.err.println("Error loading story: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to Load Story",
                    "There was an error loading the story: " + e.getMessage());
        }
    }
    //================================

    //   === Add Story ===
    @FXML
    public void addStory() {
        Stage storyStage = new Stage();
        storyStage.setTitle("Add New Story");
        storyStage.initModality(Modality.APPLICATION_MODAL);

        TextArea storyText = createStoryTextArea();

        Button uploadPhotoButton = new Button("Choose Photo");
        Label photoStatusLabel = new Label("No photo chosen");
        File[] selectedImageFile = new File[1];

        uploadPhotoButton.setOnAction(ev -> selectStoryImage(storyStage, selectedImageFile, photoStatusLabel));

        Button uploadStory = new Button("Upload");
        Label statusLabel = new Label();

        VBox vbox = new VBox(10,
                storyText,
                uploadPhotoButton,
                photoStatusLabel,
                uploadStory,
                statusLabel
        );
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #f9f9f9;");
        uploadStory.setPrefWidth(100);

        uploadStory.setOnAction(e -> handleStoryUpload(storyText, selectedImageFile, statusLabel, storyStage));

        Scene scene = new Scene(vbox, 300, 370);
        storyStage.setScene(scene);
        storyStage.showAndWait();
    }

    private TextArea createStoryTextArea() {
        TextArea storyText = new TextArea();
        storyText.setPromptText("Story Text (Max " + MAX_STORY_TEXT_LENGTH + " characters)");
        storyText.setWrapText(true);
        storyText.setPrefRowCount(3);
        storyText.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > MAX_STORY_TEXT_LENGTH) {
                storyText.setText(newValue.substring(0, MAX_STORY_TEXT_LENGTH));
            }
        });
        return storyText;
    }

    private void selectStoryImage(Stage stage, File[] selectedFile, Label statusLabel) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Story Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            selectedFile[0] = file;
            statusLabel.setText("Selected: " + file.getName());
        }
    }

    private void handleStoryUpload(TextArea storyText, File[] selectedImageFile, Label statusLabel, Stage stage) {
        String text = storyText.getText().trim();

        if (text.isEmpty() && selectedImageFile[0] == null) {
            statusLabel.setText("Please add text or choose a photo");
            return;
        }

        String photoPath = processStoryImage(selectedImageFile[0], statusLabel);
        if (selectedImageFile[0] != null && photoPath == null) {
            return; // Image processing failed
        }

        user.postStory(text, photoPath);
        statusLabel.setText("Story created successfully!");
        loadAllStories();

        PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
        delay.setOnFinished(ev -> stage.close());
        delay.play();
    }

    private String processStoryImage(File imageFile, Label statusLabel) {
        if (imageFile == null) return null;

        try {
            createDirectoryIfNotExists(STORY_IMAGES_DIR);

            String ext = getFileExtension(imageFile.getName());
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String newFileName = user.getFirstName() + "_" + user.getLastName() + "_" + timestamp + "." + ext;

            Path destination = Paths.get(STORY_IMAGES_DIR, newFileName);
            Files.copy(imageFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            return newFileName;
        } catch (IOException ex) {
            ex.printStackTrace();
            statusLabel.setText("Failed to save photo: " + ex.getMessage());
            return null;
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }
    //================================

    //   === My Story ===
    @FXML public void deleteStory() {
        if (currentStory == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "No Story Selected", "Please select a story to delete.");
            return;
        }

        if (!currentStory.getOwner().equals(user)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot Delete", "You can only delete your own stories.");
            return;
        }

        confirmAndDeleteStory();
    }

    private void confirmAndDeleteStory() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Story");
        confirmAlert.setHeaderText("Delete Story");
        confirmAlert.setContentText("Are you sure you want to delete this story?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    user.getStories().remove(currentStory);
                    deleteStoryImage();
                    resetStoryView();
                    loadAllStories();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Story Deleted", "The story has been deleted successfully.");
                } catch (Exception e) {
                    System.err.println("Error deleting story: " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to Delete",
                            "There was an error deleting the story: " + e.getMessage());
                }
            }
        });
    }

    private void deleteStoryImage() {
        if (currentStory.getPhotoPath() != null && !currentStory.getPhotoPath().isEmpty()) {
            String imageName = extractFileName(currentStory.getPhotoPath());
            File imageFile = new File(STORY_IMAGES_DIR + File.separator + imageName);
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }
    }

    private void resetStoryView() {
        storyView.setImage(null);
        storyText.setText("");
        Name.setText("");
        setButtonsVisible(false);
        currentStory = null;
    }

    @FXML public void showStoryViewers() {
        if (!validateStoryViewersRequest()) return;

        Map<User, LocalDateTime> viewersMap = currentStory.getViewers();

        Stage viewersStage = new Stage();
        viewersStage.setTitle("Story Viewers");
        viewersStage.initModality(Modality.APPLICATION_MODAL);

        VBox viewersBox = createViewersBox(viewersMap);
        Scene scene = new Scene(viewersBox, 300, 400);
        viewersStage.setScene(scene);
        viewersStage.showAndWait();
    }

    private boolean validateStoryViewersRequest() {
        if (currentStory == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "No Story Selected",
                    "Please select a story to view its viewers.");
            return false;
        }

        if (!currentStory.getOwner().equals(user)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot View",
                    "You can only see viewers of your own stories.");
            return false;
        }

        return true;
    }
    //================================

    //    === UI ===
       // ===== Scenes =====
    private  VBox createViewersBox(Map<User, LocalDateTime> viewersMap) {
        VBox viewersBox = new VBox(10);
        viewersBox.setPadding(new Insets(20));
        viewersBox.setStyle("-fx-background-color: #f9f9f9;");

        Label titleLabel = new Label(viewersMap.size() + "Views");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        viewersBox.setAlignment(Pos.TOP_CENTER);
        viewersBox.getChildren().add(titleLabel);

        if (viewersMap.isEmpty()) {
            Label noViewersLabel = new Label("No one has viewed this story yet.");
            viewersBox.getChildren().add(noViewersLabel);
        } else {
            viewersMap.forEach((viewer, viewTime) ->
                    viewersBox.getChildren().add(createViewerItem(viewer, viewTime)));
        }

        return viewersBox;
    }

    private  HBox createViewerItem(User viewer, LocalDateTime viewTime) {
        HBox viewerItem = new HBox(10);
        viewerItem.setAlignment(Pos.CENTER_LEFT);

        ImageView avatar = createCircularImageView(loadProfileImage(viewer.getProfile().getPhoto()), 30);

        VBox viewerInfo = new VBox(2);
        Label nameLabel = new Label(viewer.getFirstName() + " " + viewer.getLastName());
        Label timeLabel = new Label("Viewed: " + viewTime.format(TIME_FORMATTER));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");
        viewerInfo.getChildren().addAll(nameLabel, timeLabel);

        viewerItem.getChildren().addAll(avatar, viewerInfo);
        return viewerItem;
    }

    private  HBox createStoryItem(Story story) {
        String photoPath = story.getOwner().getProfile().getPhoto();
        ImageView avatar = createCircularImageView(loadProfileImage(photoPath), 36);

        HBox storyItem = new HBox(avatar, createStoryTextBox(story));
        storyItem.setMaxWidth(286);
        storyItem.setMaxHeight(50);
        storyItem.setStyle(BASE_STORY_STYLE);
        storyItem.setSpacing(10);
        storyItem.setPadding(new Insets(8));
        storyItem.setAlignment(Pos.CENTER_LEFT);

        // Add indicator if story is from current user
        if (story.getOwner().equals(user)) {
            Circle ownerIndicator = new Circle(5);
            ownerIndicator.setFill(Color.LIGHTGREEN);
            storyItem.getChildren().add(ownerIndicator);
        }

        // Hover effect
        storyItem.setOnMouseEntered(e -> storyItem.setStyle(HOVER_STORY_STYLE));
        storyItem.setOnMouseExited(e -> storyItem.setStyle(BASE_STORY_STYLE));

        return storyItem;
    }

    private  VBox createStoryTextBox(Story story) {
        // Chat name and last message
        Label nameLabel = new Label(story.getOwner().getFirstName() + " " + story.getOwner().getLastName());
        nameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label previewLabel = new Label(story.getPublishTime().format(DateTimeFormatter.ofPattern("hh:mm")));
        previewLabel.setStyle("-fx-text-fill: #C0C0C0 ; -fx-font-size: 12px;");

        VBox textBox = new VBox(nameLabel, previewLabel);
        textBox.setSpacing(2);

        return textBox;
    }

       // ===== Images =====
    private Image loadStoryImage(String photoPath) {
        if (photoPath != null && !photoPath.isEmpty()) {
            String imageName = extractFileName(photoPath);
            File imageFile = new File(STORY_IMAGES_DIR + File.separator + imageName);
            if (imageFile.exists()) {
                return new Image(imageFile.toURI().toString());
            }
        }

        // Load default image if no image found
        File defaultImageFile = new File(STORY_IMAGES_DIR + File.separator + "default_profile.png");
        if (defaultImageFile.exists()) {
            return new Image(defaultImageFile.toURI().toString());
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "No Image Found.", "The photo path couldn't be found.");
            return null;
        }
    }

    private String extractFileName(String path) {
        return path.contains("/") || path.contains("\\") ? new File(path).getName() : path;
    }

    private Image loadProfileImage(String path) {
        try {
            if (path == null || path.isEmpty()) {
                URL defaultUrl = getClass().getResource(DEFAULT_IMAGE_PATH);
                if (defaultUrl != null) {
                    return new Image(defaultUrl.toExternalForm());
                }
            } else {
                String imageName = extractFileName(path);
                return loadImage("/Image/ProfileImages/" + imageName, "ProfileImages/" + imageName);
            }
        } catch (Exception e) {
            System.err.println("Error loading profile image: " + e.getMessage());
        }
        return null;
    }

    private Image loadImage(String resourcePath, String filePath) {
        URL resourceUrl = getClass().getResource(resourcePath);
        if (resourceUrl != null) {
            return new Image(resourceUrl.toExternalForm());
        }

        if (filePath != null) {
            File imageFile = new File(filePath);
            if (imageFile.exists()) {
                return new Image(imageFile.toURI().toString());
            }
        }

        URL defaultUrl = getClass().getResource(DEFAULT_IMAGE_PATH);
        return defaultUrl != null ? new Image(defaultUrl.toExternalForm()) : null;
    }

    private ImageView createCircularImageView(Image image, int size) {
        if (image == null) return null;

        ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(false);

        int radius = size / 2;
        view.setClip(new Circle(radius, radius, radius));

        return view;
    }

       // ===== Misc =====
    private void setButtonsVisible(boolean visible) {
        deleteStory.setVisible(visible);
        storyViewers.setVisible(visible);
    }

    private void showAlert(Alert.AlertType alertType, String title, String headerText, String contentText) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.initStyle(StageStyle.UTILITY);
        alert.showAndWait();
    }

    private  Button imageButton(String path) {
        Image image = new Image(getClass().getResource(path).toExternalForm());
        ImageView view = new ImageView(image);
        view.setFitWidth(10);
        view.setFitHeight(10);
        Button imageButton = new Button();
        imageButton.setPrefSize(15, 15);
        imageButton.setStyle("-fx-background-color: transparent;");
        imageButton.setGraphic(view);
        return imageButton;
    }

    //================================
}