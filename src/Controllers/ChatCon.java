package Controllers;

import Models.ChatRoom;
import Models.ChatRoomType;
import Models.Message;
import Models.User;
import Utils.Global;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ChatCon {
    private static final String BASE_CHAT_STYLE = "-fx-background-color: #0F3338; -fx-background-radius: 8;";
    private static final String HOVER_CHAT_STYLE = "-fx-background-color: #1E6670; -fx-background-radius: 8;";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MMM/yyyy HH:mm a");
    private static final int MAX_TEXT_LENGTH = 60;

    @FXML
    public ScrollPane MsgScrollPane;
    Stage stage;
    ChatRoom currentRoom;
    Message currentRepliedMessage = null;
    HashSet<String> selectedContacts = new HashSet<>();
    ArrayList<String> chatPreview = new ArrayList<>();
    BorderPane root;
    private Pane currentReplyPane = null;
    @FXML
    private Pane bottom;
    @FXML
    private TextArea textArea;
    @FXML
    private TextField searchBar;
    @FXML
    private Button sendBtn, newMember, removeMember, myProfileBtn, starredMessages, leaveGroup;
    @FXML
    private VBox messagesBox, chatsBox, contactsList;
    @FXML
    private Label ChatName, lastSeen;

    public ChatCon() {
    }

    public void setRoot(BorderPane root) {
        this.root = root;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setMyProfile() {
        if(Global.user != null){
            ImageView myProfileImg = ImageSetter(Global.user.getProfile().getPhoto(), 36);
            myProfileBtn.setGraphic(myProfileImg);
        }
    }

    //    === misc ===
    public void logout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Login.fxml"));
            BorderPane root = loader.load();
            Scene scene = new Scene(root);

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            currentStage.setTitle("EZ Talk");
            currentStage.setScene(scene);
            currentStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Global.mainUsersMap.get(Global.user.getMobileNumber()).getProfile().setLastSeen();
        Global.user = null;
        currentRoom = null;
        setChatAreaVisible(false);
    }

    public void searchChats() {
        String searchKey = searchBar.getText().toLowerCase();
        chatsBox.getChildren().clear();
        if (searchKey.isEmpty()) {
            loadAllChats();
            return;
        }
        // First add rooms with matching names
        Global.mainChatRooms.values().stream()
                .filter(room -> room.getChatRoomName().toLowerCase().contains(searchKey))
                .forEach(room -> loadChatRoom(room, false));
        // Then add Contacts with matching names
        Global.user.getContacts().stream().map(Global.mainUsersMap::get)
                .filter(contact ->
                        contact.getFirstName().toLowerCase().contains(searchKey) ||
                                contact.getLastName().toLowerCase().contains(searchKey)
                )
                .forEach(this::loadSearchedContacts);

        // Then add rooms with matching messages
        Global.mainChatRooms.values().stream().filter(room -> room.getUsers().contains(Global.user.getMobileNumber())).forEach(room ->
                room.getMessages().values().stream()
                        .filter(message -> message.getText().toLowerCase().contains(searchKey))
                        .forEach(message -> loadSearchedMessagetoChatsBox(room, message))
        );
    }

    public void myProfile() {
        Stage popupStage = new Stage();
        popupStage.initOwner(stage);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Update Profile");

        // UI components
        ImageView profileImageView = ImageSetter(Global.user.getProfile().getPhoto(), 80);
        profileImageView.setPreserveRatio(true);

        String currentPhotoPath = Global.user.getProfile().getPhoto();

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

                // Preview the selected image
                try {
                    Image image = new Image(file.toURI().toString());
                    profileImageView.setImage(image);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        CheckBox visibilityCheckBox = new CheckBox("Visible to all");
        visibilityCheckBox.setSelected(Global.user.getProfile().isVisibleToAll());

        TextField firstNameField = new TextField(Global.user.getFirstName());
        firstNameField.setPromptText("First Name");

        TextField lastNameField = new TextField(Global.user.getLastName());
        lastNameField.setPromptText("Last Name");

        TextArea aboutArea = new TextArea(Global.user.getProfile().getAbout());
        aboutArea.setPromptText("About Text (Max " + MAX_TEXT_LENGTH + " characters)");
        aboutArea.setWrapText(true);
        aboutArea.setPrefRowCount(3);
        aboutArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > MAX_TEXT_LENGTH) {
                aboutArea.setText(newValue.substring(0, MAX_TEXT_LENGTH));
            }
        });

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("New Password");

        Button saveButton = new Button("Save");
        Button cancelButton = new Button("Cancel");
        Button deleteButton = new Button("Delete Account");

        saveButton.setOnAction(e -> {
            // Validate inputs
            String firstName = firstNameField.getText().trim();
            String lastName = lastNameField.getText().trim();
            String about = aboutArea.getText().trim();
            String newPassword = passwordField.getText();

            // Check for empty required fields
            if (firstName.isEmpty() || lastName.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error",
                        "Missing Information", "First name, last name, and password are required.");
                return;
            }

            Global.user.setFirstName(firstName);
            Global.user.setLastName(lastName);
            Global.user.getProfile().setAbout(about);
            Global.user.getProfile().setVisibility(visibilityCheckBox.isSelected());
            if (!newPassword.isEmpty()) {
                if (newPassword.length() < 6) {
                    showAlert(Alert.AlertType.WARNING, "Validation Error",
                            "Password Too Short", "New password must be at least 6 characters.");
                    return;
                }
                Global.user.setPassword(newPassword);
            }

            // Process profile image
            if (selectedImageFile[0] != null) {
                try {
                    File imageDir = new File("ProfileImages");
                    if (!imageDir.exists()) imageDir.mkdir();

                    String ext = getFileExtension(selectedImageFile[0].getName());
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    String newFileName = Global.user.getFirstName() + "_" + Global.user.getLastName() + "_" + timestamp + "." + ext;

                    Path destination = Paths.get(imageDir.getPath(), newFileName);
                    Files.copy(selectedImageFile[0].toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                    // Delete old profile photo if it exists
                    if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
                        try {
                            String photoPath = "ProfileImages/" + currentPhotoPath;
                            File oldPhoto = new File(photoPath);
                            if (oldPhoto.exists() && !photoPath.equals("ProfileImages/default_profile.png")) {
                                Files.delete(oldPhoto.toPath());
                            }
                        } catch (IOException ignored) {
                            // Silently ignore if deletion fails
                        }
                    }

                    Global.user.getProfile().setPhoto(newFileName);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "Failed to save photo", "There was an error saving your profile picture.");
                    return;
                }
            }
            popupStage.close();
        });

        cancelButton.setOnAction(e -> popupStage.close());

        deleteButton.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Login.fxml"));
                BorderPane root = loader.load();
                Scene scene = new Scene(root);

                stage.setTitle("EZ Talk");
                stage.setScene(scene);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            popupStage.close();
            Global.mainUsersMap.get(Global.user.getMobileNumber()).setIsDeleted(true);
            Global.mainUsersMap.get(Global.user.getMobileNumber()).getStories().clear();
            Global.user = null;
            currentRoom = null;
            setChatAreaVisible(false);

        });

        // Layout setup
        HBox buttonsBox = new HBox(10, saveButton, cancelButton, deleteButton );
        buttonsBox.setAlignment(Pos.CENTER);

        VBox layout = new VBox(10,
                new Label("Update Profile"),
                profileImageView,
                uploadPhotoButton,
                firstNameField,
                lastNameField,
                aboutArea,
                passwordField,
                visibilityCheckBox,
                buttonsBox
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));

        Scene scene = new Scene(layout, 350, 480);
        popupStage.setScene(scene);
        popupStage.setResizable(false);
        popupStage.showAndWait();
        setMyProfile();
    }

    public void navigateToStories() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Views/Story.fxml"));
            BorderPane root = loader.load();

            StoryCon con = loader.getController();
            con.setUser(Global.user);

            Scene scene = new Scene(root);
            stage.setTitle("EZ Talk");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

        currentRoom = null;
        setChatAreaVisible(false);
    }
    //================================

    //    === Load ===
    public void loadAllChats() {
        chatsBox.getChildren().clear();

        List<ChatRoom> sortedRooms = Global.mainChatRooms.values().stream().filter(room -> room.getUsers().contains(Global.user.getMobileNumber()))
                .sorted((a, b) -> b.getMessages().values().stream().toList().getLast().getTimestamp()
                        .compareTo(a.getMessages().values().stream().toList().getLast().getTimestamp()))
                .toList();

        sortedRooms.forEach(room -> loadChatRoom(room, false));
    }

    public void loadChatRoom(ChatRoom room, boolean isNew) {
        HBox chatItem = hBoxChatItemUi(room);
        chatItem.setId(room.getChatRoomID());
        chatItem.setOnMouseClicked(e -> loadMessages(room, null));

        if (isNew) {
            chatsBox.getChildren().addFirst(chatItem);
        } else {
            chatsBox.getChildren().add(chatItem);
        }
    }

    public void loadSearchedMessagetoChatsBox(ChatRoom room, Message searchedMessage) {
        Label nameLabel = new Label();
        if (room.getType().equals(ChatRoomType.ONE_TO_ONE)) {
            String names = room.getUsers().stream()
                    .map(Global.mainUsersMap::get)
                    .filter(otherUser -> otherUser != null && !otherUser.equals(Global.user))
                    .map(otherUser -> otherUser.getFirstName() + " " + otherUser.getLastName())
                    .collect(Collectors.joining(" ")); // Join with comma or space
            nameLabel.setText(names);
        } else
            nameLabel.setText(room.getChatRoomName());
        nameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label messageLabel = new Label(searchedMessage.getText());
        messageLabel.setStyle("-fx-text-fill: #C0C0C0 ; -fx-font-size: 12px;");

        VBox textBox = new VBox(nameLabel, messageLabel);
        textBox.setSpacing(2);

        HBox chatItem = hBoxSearchedMessagetoChatsBoxUI(textBox);
        chatItem.setOnMouseClicked(e -> loadMessages(room, searchedMessage));

        chatsBox.getChildren().add(chatItem);
    }

    public void loadSearchedContacts(User contact) {
        ImageView avatar = ImageSetter(contact.getProfile().getPhoto(), 36);

        Label nameLabel = new Label(contact.getFirstName() + " " + contact.getLastName());
        nameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px;");

        Label aboutLabel = new Label(contact.getProfile().getAbout());
        aboutLabel.setStyle("-fx-text-fill: #C0C0C0 ; -fx-font-size: 12px;");

        VBox textBox = new VBox(nameLabel, aboutLabel);
        textBox.setSpacing(2);

        HBox chatItem = hBoxSearchedContacttoChatsBoxUI(avatar, textBox);
        chatItem.setOnMouseClicked(e -> {
            currentRoom = null;
            Optional<ChatRoom> matchedRoom = Global.mainChatRooms.values().stream()
                    .filter(room ->
                            room.getType().equals(ChatRoomType.ONE_TO_ONE) &&
                                    room.getUsers().contains(Global.user.getMobileNumber()) &&
                                    room.getUsers().contains(contact.getMobileNumber()))
                    .findFirst();
            if (matchedRoom.isPresent()) {
                loadMessages(matchedRoom.get(), null);
            } else {
                setChatAreaVisible(true);
                leaveGroup.setVisible(false);
                ChatName.setText(nameLabel.getText());
                messagesBox.getChildren().clear();
                Message noMessage = new Message("No Message", "No Messages Here Yet. \n Send new Message Now.", "No Message");
                VBox messageVBox = new VBox();
                messageVBox.setSpacing(1);
                messageVBox.getChildren().add(createMessageLabel(noMessage, noMessage.getMessageID(), true));
                HBox messageHBox = new HBox(messageVBox);
                messageHBox.setAlignment(Pos.CENTER_RIGHT);
                messageHBox.setPadding(new Insets(5, 10, 5, 10));
                messageHBox.setId(noMessage.getMessageID());

                messagesBox.getChildren().add(messageHBox);
                sendBtn.setOnAction(ev -> {
                    if (currentRoom == null) {
                        ChatRoom newChatRoom = new ChatRoom(ChatRoomType.ONE_TO_ONE, "One to One", Global.user.getMobileNumber());
                        currentRoom = newChatRoom;
                        newChatRoom.getUsers().add(contact.getMobileNumber());
                        newChatRoom.getUsers().add(Global.user.getMobileNumber());
                        setChatAreaVisible(true);
                        Global.mainChatRooms.put(newChatRoom.getChatRoomID(), newChatRoom);
                        messagesBox.getChildren().clear();
                    }
                    addMessage(textArea.getText());
                    textArea.clear();
                });
            }
        });

        chatsBox.getChildren().add(chatItem);
    }

    public void loadMessages(ChatRoom chatRoom, Message searchedMessage) {
        messagesBox.getChildren().clear();
        currentRoom = chatRoom;
        setChatAreaVisible(currentRoom != null);
        assert chatRoom != null;
        String names = chatRoom.getUsers().stream()
                .map(Global.mainUsersMap::get)
                .filter(otherUser -> otherUser != null && !otherUser.equals(Global.user))
                .map(otherUser -> otherUser.getFirstName() + " " + otherUser.getLastName())
                .collect(Collectors.joining(" ")); // Join with comma or space
        if (chatRoom.getType().equals(ChatRoomType.ONE_TO_ONE)) {
            ChatName.setText(names);
            LocalDateTime lastSeenTime = Global.mainUsersMap.get(currentRoom.getUsers().stream()
                    .filter(user -> !user.equals(Global.user.getMobileNumber()))
                    .toList().getFirst()).getProfile().getLastSeen();

            if (lastSeenTime != null) {
                LocalDateTime now = LocalDateTime.now();
                LocalDate lastSeenDate = lastSeenTime.toLocalDate();
                LocalDate today = now.toLocalDate();
                LocalDate yesterday = today.minusDays(1);

                String formatted;
                if (lastSeenDate.equals(today)) {
                    formatted = lastSeenTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                } else if (lastSeenDate.equals(yesterday)) {
                    formatted = "yesterday";
                } else {
                    formatted = lastSeenTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }

                lastSeen.setText("Last Seen:  " + formatted);
            }else
                lastSeen.setText("");
        } else {
            ChatName.setText(chatRoom.getChatRoomName());
        }


        int highlightIndex = -1;
        int index = 0;

        List<Message> sortedMessages = currentRoom.getMessages().values().stream()
                .sorted(Comparator.comparing(Message::getTimestamp))
                .toList();

        for (Message msg : sortedMessages) {
            msg.markSeen(Global.user.getMobileNumber());
            HBox node = createMessageHBox(msg, currentRoom.getChatRoomID(), Objects.equals(msg.getMobileNumber(), Global.user.getMobileNumber()));

            if (msg.equals(searchedMessage) && highlightIndex == -1) {
                highlightIndex = index;
                node.setStyle(node.getStyle() + "; -fx-background-color: #EAA276;");
            }

            messagesBox.getChildren().add(node);
            index++;
        }

        textArea.requestFocus();

        if (highlightIndex != -1) {
            double position = (double) highlightIndex / currentRoom.getMessages().size();
            Platform.runLater(() -> MsgScrollPane.setVvalue(position));
        } else {
            Platform.runLater(() -> MsgScrollPane.setVvalue(1.0));
        }
    }
    //================================

    //    === New Contact ===
    public void newContact() {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Contacts");
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);

        Scene scene = new Scene(buildNewContactForm(), 300, 400);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private VBox buildNewContactForm() {
        TextField userPhoneNumber = new TextField();
        userPhoneNumber.setPromptText("Contact Phone Number");
        Label statusLabel = new Label();
        Button createButton = new Button("Add Contact");

        ScrollPane contactsScroll = new ScrollPane();
        contactsScroll.setPrefSize(290, 200);
        contactsList = loadContacts(false);
        contactsList.setPrefWidth(258);
        contactsScroll.setContent(contactsList);

        // Event handler
        createButton.setOnAction(e -> handleNewContact(userPhoneNumber.getText(), statusLabel));
        userPhoneNumber.setOnAction(e -> handleNewContact(userPhoneNumber.getText(), statusLabel));

        VBox layout = new VBox(10,
                new Label("Enter Contact Phone Number:"),
                userPhoneNumber,
                createButton,
                statusLabel,
                contactsScroll
        );

        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f9f9f9;");
        return layout;
    }

    private void handleNewContact(String phoneNumber, Label statusLabel) {
        if (phoneNumber.isEmpty()) {
            statusLabel.setText("Phone Number is required.");
            return;
        }

        if (phoneNumber.equals(Global.user.getMobileNumber())) {
            statusLabel.setText("This is your number.");
            return;
        }

        String result = Global.user.addContact(phoneNumber);
        statusLabel.setText(result);

        User newContact = Global.mainUsersMap.get(phoneNumber);
        if (newContact != null && Global.user.getContacts().contains(phoneNumber)) {
            ContactItem(newContact, contactsList, false, true); // add to visible list
        }
    }

    private VBox loadContacts(boolean isSelectable) {
        VBox contactsList = new VBox(10);
        contactsList.setPadding(new Insets(10));
        contactsList.setStyle("-fx-background-color: #f9f9f9;");

        Label titleLabel = new Label("Contacts List");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        contactsList.setAlignment(Pos.TOP_CENTER);
        contactsList.getChildren().add(titleLabel);

        for (User contact : Global.user.getContacts().stream().map(Global.mainUsersMap::get).filter(contact -> !contact.isDeleted()).toList()) {
            ContactItem(contact, contactsList, isSelectable, !isSelectable);
        }
        return contactsList;
    }

    private void ContactItem(User contact, VBox contactsList, boolean isSelectable, boolean isDeletable) {
        HBox contactItem = new HBox(15);
        contactItem.setAlignment(Pos.CENTER);

        ImageView avatar = ImageSetter(contact.getProfile().getPhoto(), 35);

        VBox contactInfo = new VBox(2);
        contactInfo.setMinWidth(140);
        Label nameLabel = new Label(contact.getFirstName() + " " + contact.getLastName());
        Label numberLabel = new Label(contact.getMobileNumber());
        Label aboutLabel = new Label(contact.getProfile().getAbout());
        nameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #000000; -fx-font-weight: bold;");
        numberLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #000000; -fx-font-weight: bold;");
        aboutLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");
        aboutLabel.setWrapText(true);
        aboutLabel.setMaxWidth(200);
        contactInfo.getChildren().addAll(nameLabel, numberLabel);
        if (Global.user.isMutualContact(contact) || contact.getProfile().isVisibleToAll())
            contactInfo.getChildren().add(aboutLabel);
        contactItem.getChildren().addAll(avatar, contactInfo);
        if (isDeletable) {
            Button deleteContact = imageButton("/Image/close.png");
            deleteContact.setOnAction(e -> {
                Global.user.getContacts().remove(contact.getMobileNumber());
                contactsList.getChildren().remove(contactItem);
            });
            contactItem.getChildren().add(deleteContact);
        }
        if (isSelectable) {
            CheckBox selectBox = new CheckBox();
            selectBox.setOnAction(e -> {
                if (selectBox.isSelected()) {
                    selectedContacts.add(contact.getMobileNumber());
                } else {
                    selectedContacts.remove(contact.getMobileNumber());
                }
            });
            contactItem.getChildren().add(selectBox);
            contactItem.setPrefWidth(300);
        }
        contactsList.getChildren().add(contactItem);
    }
    //================================

    //    === User in Room ===
    public void addUser() {
        showDialog("Add Member", buildNewUserForm());
    }

    private VBox buildNewUserForm() {
        TextField userPhoneNumber = new TextField();
        userPhoneNumber.setPromptText("Member Phone Number");
        Label statusLabel = new Label();
        Button createButton = new Button("Add Member");

        ScrollPane membersScroll = new ScrollPane();
        membersScroll.setPrefSize(360, 200);
        VBox memberList = loadMembers();
        membersScroll.setContent(memberList);

        if (currentRoom.getType().equals(ChatRoomType.GROUP)) {
            createButton.setOnAction(e -> handleAddMember(userPhoneNumber.getText(), statusLabel));
            userPhoneNumber.setOnAction(e -> handleAddMember(userPhoneNumber.getText(), statusLabel));
        } else if (currentRoom.getUsers().size() < 2) {
            createButton.setOnAction(e -> handleAddMember(userPhoneNumber.getText(), statusLabel));
            userPhoneNumber.setOnAction(e -> handleAddMember(userPhoneNumber.getText(), statusLabel));
        } else {
            // One-to-one room is full
            PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
            Runnable showFullRoomMessage = () -> {
                statusLabel.setText("This is a One-to-One Room and it is full.");
                delay.play();
            };

            createButton.setOnAction(e -> showFullRoomMessage.run());
            userPhoneNumber.setOnAction(e -> showFullRoomMessage.run());
            delay.setOnFinished(event -> closeCurrentDialog());
        }

        VBox layout = new VBox(10,
                new Label("Enter Member Phone Number:"),
                userPhoneNumber,
                createButton,
                statusLabel,
                membersScroll
        );

        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f9f9f9;");
        return layout;
    }

    private void handleAddMember(String PhoneNumber, Label statusLabel) {
        if (PhoneNumber.isEmpty()) {
            statusLabel.setText("Phone Number is required.");
            return;
        }

        if (!PhoneNumber.matches("\\d{11}")) {
            statusLabel.setText("Phone Number has to be 11 digits.");
            return;
        }

        if (PhoneNumber.equals(Global.user.getMobileNumber())) {
            statusLabel.setText("You are already in this room.");
            return;
        }

        User targetUser = Global.user.getContacts().stream()
                .filter(contact -> contact.equals(PhoneNumber))
                .findFirst().map(Global.mainUsersMap::get)
                .orElse(null);

        if (targetUser == null) {
            statusLabel.setText("This Number isn't contact with you.");
            return;
        }

        if (currentRoom.getUsers().contains(targetUser.getMobileNumber())) {
            statusLabel.setText("This Contact is already in room.");
            return;
        }

        currentRoom.getUsers().add(targetUser.getMobileNumber());
        addMessage(Global.user.getFirstName() + " just added " + targetUser.getFirstName() + " " + targetUser.getLastName());
        statusLabel.setText("Member Added Successfully!");

        // Auto-close the dialog after a delay
        PauseTransition delay = new PauseTransition(Duration.seconds(0.75));
        delay.setOnFinished(ev -> closeCurrentDialog());
        delay.play();
    }

    public void leaveGroup() {
        if (currentRoom.getUsers().size() == 1) {
            Global.mainChatRooms.remove(currentRoom.getChatRoomID());
            loadAllChats();
            return;
        }
        currentRoom.removeUser(Global.user.getMobileNumber());
        if (currentRoom.getAdmin().equals(Global.user.getMobileNumber())) {
            List<String> userList = new ArrayList<>(currentRoom.getUsers());
            currentRoom.setAdmin(userList.getFirst());
        }
        loadAllChats();
    }

    private VBox loadMembers() {
        VBox membersList = new VBox();
        membersList.setSpacing(5);
        membersList.setPadding(new Insets(5, 15, 5, 15));
        membersList.setPrefWidth(344);

        Label header = new Label("Members List");
        membersList.getChildren().add(header);

        int i = 1;
        for (String member : currentRoom.getUsers()) {
            User newUser = Global.mainUsersMap.get(member);
            if (!newUser.isDeleted()) {
                ContactItem(newUser, membersList, false, false);
                i++;
            }
        }
        return membersList;
    }

    @FXML
    private void removeUser() {
        showDialog("Remove Member", buildRemoveUserForm());

    }

    private VBox buildRemoveUserForm() {
        TextField userPhoneNumber = new TextField();
        userPhoneNumber.setPromptText("Member Phone Number");
        Label statusLabel = new Label();
        Button removeButton = new Button("Remove Member");

        ScrollPane membersScroll = new ScrollPane();
        membersScroll.setPrefSize(360, 200);
        VBox memberList = loadMembers();
        membersScroll.setContent(memberList);

        removeButton.setOnAction(e -> {
            boolean isRemoved = currentRoom.removeUser(userPhoneNumber.getText());
            statusLabel.setText(isRemoved ? "Member Removed Successfully." : "Member isn't found.");

            if (isRemoved) {
                PauseTransition delay = new PauseTransition(Duration.seconds(0.75));
                delay.setOnFinished(ev -> {
                    closeCurrentDialog();
                    loadAllChats();
                });
                delay.play();
            }
        });

        VBox layout = new VBox(10,
                new Label("Enter Member Phone Number:"),
                userPhoneNumber,
                removeButton,
                statusLabel,
                membersScroll
        );

        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f9f9f9;");
        return layout;
    }
    //================================

    //    === New Group ===
    public void newGroup() {
        showDialog("Create New Group", buildNewGroupForm());
    }

    private VBox buildNewGroupForm() {
        TextField chatRoomNameField = new TextField();
        chatRoomNameField.setPromptText("Chat Room Name");

        Label statusLabel = new Label();
        Button createButton = new Button("Create Chat Room");

        ScrollPane scrollPane = new ScrollPane(loadContacts(true));

        createButton.setOnAction(e -> handleCreateGroup(chatRoomNameField.getText(), statusLabel));

        VBox layout = new VBox(10,
                new Label("Enter Chat Room Name:"),
                chatRoomNameField,
                scrollPane,
                createButton,
                statusLabel
        );

        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f9f9f9;");
        return layout;
    }

    private void handleCreateGroup(String name, Label statusLabel) {
        String chatRoomName = name.trim();
        if (chatRoomName.isEmpty()) {
            statusLabel.setText("Chat Room Name is required.");
            return;
        }

        ChatRoomType type = ChatRoomType.GROUP;
        ChatRoom newChatRoom = new ChatRoom(type, chatRoomName, Global.user.getMobileNumber());
        newChatRoom.getUsers().add(Global.user.getMobileNumber());
        newChatRoom.getUsers().addAll(selectedContacts);
        selectedContacts.clear();
        newChatRoom.addMessage(new Message(
                Global.user.getMobileNumber(),
                Global.user.getFirstName() + " Just Created this Room",
                Global.user.getFirstName() + " " + Global.user.getLastName()
        ));
        currentRoom = newChatRoom;
        loadMessages(currentRoom, null);

        Global.mainChatRooms.put(newChatRoom.getChatRoomID(), newChatRoom);
        loadChatRoom(newChatRoom, true);
        statusLabel.setText("Chat Room Created Successfully!");

        PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
        delay.setOnFinished(ev -> closeCurrentDialog());
        delay.play();
    }
    //================================

    //    === Messages ===
    public void initialize() {
        sendBtn.setOnAction(event -> {
            String messageText = textArea.getText().trim();
            if (!messageText.isEmpty()) {
                addMessage(messageText);
                textArea.clear();
            }
        });

        textArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                sendBtn.fire();
            }
        });
    }

    public void addMessage(String text) {
        if (currentRoom == null) {
            showAlert(Alert.AlertType.WARNING, "No Chat Selected", null, "Please select a chat room before sending a message.");
            return;
        }

        Message newMsg = new Message(
                Global.user.getMobileNumber(),
                text,
                Global.user.getFirstName() + " " + Global.user.getLastName()
        );

        if (currentRepliedMessage != null) {
            newMsg.setReply(true);  // Mark the new message as a reply
            newMsg.setReplyTo(currentRepliedMessage);  // Set the message being replied to
            bottom.getChildren().remove(currentReplyPane);
            MsgScrollPane.setPrefHeight(470);
            currentRepliedMessage = null;
            currentReplyPane = null;
        }

        currentRoom.addMessage(newMsg);
        currentRoom.getMessages().values().stream().toList().getLast().markSeen(Global.user.getMobileNumber());
        messagesBox.getChildren().add(createMessageHBox(newMsg, currentRoom.getChatRoomID(), true));

        HBox chatBox = (HBox) chatsBox.lookup("#" + currentRoom.getChatRoomID());

        if (chatBox != null) {
            chatsBox.getChildren().remove(chatBox);
            chatsBox.getChildren().addFirst(chatBox);
        }

        // Update preview label
        Label previewLabel = (Label) chatsBox.lookup("#Label_" + currentRoom.getChatRoomID());
        if (previewLabel != null) {
            System.out.println(previewLabel.getText());
            System.out.println("#Label_" + currentRoom.getChatRoomID());
            previewLabel.setText(text);
        }

        textArea.requestFocus();
        Platform.runLater(() -> MsgScrollPane.setVvalue(1.0));
    }

    public boolean replyMessage(Message msg) {
        if (currentReplyPane != null) {
            bottom.getChildren().remove(currentReplyPane);
        }

        MsgScrollPane.setPrefHeight(410);

        Pane replyPane = new Pane();
        replyPane.setPrefSize(595, 60);
        replyPane.setStyle("-fx-background-color: #EAA276; -fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #0F3338; -fx-border-width: 3;");

        AnchorPane anchorPane = new AnchorPane();
        anchorPane.setLayoutX(5);
        anchorPane.setLayoutY(5);
        anchorPane.setMaxSize(580, 50);
        anchorPane.setMinSize(580, 50);

        Label repliedMessage = new Label(msg.getText());
        repliedMessage.setAlignment(Pos.TOP_LEFT);
        repliedMessage.setLayoutX(2);
        repliedMessage.setLayoutY(2);
        repliedMessage.setMaxSize(560, 45);
        repliedMessage.setMinSize(560, 45);
        repliedMessage.setWrapText(true);

        Button closeBtn = imageButton("/Image/close.png");
        closeBtn.setLayoutX(570);
        closeBtn.setLayoutY(2);
        closeBtn.setOnAction(e -> {
            msg.setReply(false);
            currentRepliedMessage = null;
            bottom.getChildren().remove(replyPane);
            MsgScrollPane.setPrefHeight(470);
        });

        anchorPane.getChildren().add(repliedMessage);
        replyPane.getChildren().addAll(anchorPane, closeBtn);
        replyPane.setLayoutX(15);
        replyPane.setLayoutY(415);

        bottom.getChildren().add(replyPane);
        textArea.requestFocus();

        currentReplyPane = replyPane;
        currentRepliedMessage = msg;

        return true;
    }

    public void deleteMessage(Message msg) {
        if (currentRoom.getMessages().size() <= 1)
            return;
        currentRoom.removeMessageById(msg.getMessageID());
        messagesBox.getChildren().removeIf(node -> msg.getMessageID().equals(node.getId()));
        loadAllChats();
    }

    public void starredMessages() {
        VBox starredList = new VBox();
        starredList.setSpacing(5);
        starredList.setPadding(new Insets(5, 15, 5, 15));
        starredList.setPrefWidth(344);

        Set<String> starredIndexesSet = Global.user.getStarredMessages().get(currentRoom.getChatRoomID());

        if (starredIndexesSet != null && !starredIndexesSet.isEmpty()) {
            int i = 1;
            for (Message msg : starredIndexesSet.stream()
                    .map(currentRoom.getMessages()::get)
                    .toList()) {

                Label starredMessage = new Label();
                starredMessage.setStyle("-fx-font-size: 12px; -fx-text-fill: #000000;");
                starredMessage.setText(i + "- " + msg.getSenderName() + "\n\t" + msg.getText() + "\n======================");
                starredList.getChildren().add(starredMessage);
                i++;
            }
        } else {
            Label noMessages = new Label("No starred messages in this chat.");
            noMessages.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
            starredList.getChildren().add(noMessages);
        }

        ScrollPane starredMessagesScroll = new ScrollPane(starredList);
        starredMessagesScroll.setPrefSize(360, 200);

        VBox form = new VBox(10, new Label("Starred Messages: "), starredMessagesScroll);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #f9f9f9;");

        showDialog("Starred Messages", form);
    }

    //================================

    //    === UI ===
    @FXML
    public void showMessageSeen(Message msg) {
        Map<String, LocalDateTime> seenByMap = msg.getSeenBy();

        Stage viewersStage = new Stage();
        viewersStage.setTitle("Seen By");
        viewersStage.initModality(Modality.APPLICATION_MODAL);

        VBox viewersBox = createSeenByBox(seenByMap);
        Scene scene = new Scene(viewersBox, 300, 400);
        viewersStage.setScene(scene);
        viewersStage.showAndWait();
    }

    private  VBox createSeenByBox(Map<String, LocalDateTime> viewersMap) {
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
                    viewersBox.getChildren().add(createSeenByItem(Global.mainUsersMap.get(viewer), viewTime)));
        }

        return viewersBox;
    }

    private  HBox createSeenByItem(User viewer, LocalDateTime viewTime) {
        HBox viewerItem = new HBox(10);
        viewerItem.setAlignment(Pos.CENTER_LEFT);

        ImageView avatar = ImageSetter(viewer.getProfile().getPhoto(), 30);

        VBox viewerInfo = new VBox(2);
        Label nameLabel = new Label(viewer.getFirstName() + " " + viewer.getLastName());
        Label timeLabel = new Label("Viewed: " + viewTime.format(TIME_FORMATTER));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888888;");
        viewerInfo.getChildren().addAll(nameLabel, timeLabel);

        viewerItem.getChildren().addAll(avatar, viewerInfo);
        return viewerItem;
    }

    //    === Chats ===
    private  VBox vBoxChatItemUi(ChatRoom room) {
        Label nameLabel = new Label();
        if (room.getType().equals(ChatRoomType.ONE_TO_ONE)) {
            String names = room.getUsers().stream().map(Global.mainUsersMap::get)
                    .filter(otherUser -> otherUser != null && !otherUser.equals(Global.user))
                    .map(otherUser -> otherUser.getFirstName() + " " + otherUser.getLastName())
                    .collect(Collectors.joining(" ")); // Join with comma or space
            nameLabel.setText(names);
        } else
            nameLabel.setText(room.getChatRoomName());

        nameLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold; -fx-font-size: 14px;");

        Optional<Message> latestMessage = room.getMessages()
                .values()
                .stream()
                .max(Comparator.comparing(Message::getTimestamp));

        Label previewLabel = new Label(latestMessage.map(Message::getText).orElse(""));
        previewLabel.setStyle("-fx-text-fill: #C0C0C0 ; -fx-font-size: 12px;");

        VBox textBox = new VBox(nameLabel, previewLabel);
        textBox.setSpacing(2);

        return textBox;
    }

    private  HBox hBoxChatItemUi(ChatRoom room) {
        ImageView avatar;

        if (room.getType().equals(ChatRoomType.ONE_TO_ONE)) {
            // Try to find another Global.user's profile pic for one-to-one chats
            avatar = room.getUsers().stream()
                    .map(Global.mainUsersMap::get) // maps String (mobile number) -> User
                    .filter(otherContact ->
                            otherContact != null &&
                                    !otherContact.equals(Global.user) &&
                                    otherContact.getProfile().isVisibleToAll())
                    .map(otherContact -> otherContact.getProfile().getPhoto())
                    .filter(photo -> photo != null && !photo.isEmpty())
                    .findFirst()
                    .map(photo -> ImageSetter(photo, 36))
                    .orElseGet(() -> ImageSetter("default_profile.png", 36));
        } else {
            // Default for group chats
            avatar = ImageSetter("default_profile.png", 36);
        }


        HBox chatItem = new HBox(avatar, vBoxChatItemUi(room));
        chatItem.setMaxWidth(286);  // Set the preferred width
        chatItem.setMaxHeight(50);  // Set the preferred height
        chatItem.setStyle(BASE_CHAT_STYLE);
        chatItem.setSpacing(10);
        chatItem.setPadding(new Insets(8));
        chatItem.setAlignment(Pos.CENTER_LEFT);

        // Hover effect
        chatItem.setOnMouseEntered(e -> chatItem.setStyle(HOVER_CHAT_STYLE));
        chatItem.setOnMouseExited(e -> chatItem.setStyle(BASE_CHAT_STYLE));
        return chatItem;
    }

    private  HBox hBoxSearchedMessagetoChatsBoxUI(VBox textBox) {
        final String baseStyle = "-fx-background-color: #0F3338; -fx-background-radius: 8;";
        final String hoverStyle = "-fx-background-color: #1E6670; -fx-background-radius: 8;";

        HBox chatItem = new HBox(textBox);
        chatItem.setMaxWidth(286);  // Set the preferred width
        chatItem.setMaxHeight(50);  // Set the preferred height
        chatItem.setStyle(baseStyle);
        chatItem.setSpacing(10);
        chatItem.setPadding(new Insets(8));
        chatItem.setAlignment(Pos.CENTER_LEFT);

        chatItem.setOnMouseEntered(e -> chatItem.setStyle(hoverStyle));
        chatItem.setOnMouseExited(e -> chatItem.setStyle(baseStyle));

        return chatItem;
    }

    private  HBox hBoxSearchedContacttoChatsBoxUI(ImageView view, VBox textBox) {
        final String baseStyle = "-fx-background-color: #0F3338; -fx-background-radius: 8;";
        final String hoverStyle = "-fx-background-color: #1E6670; -fx-background-radius: 8;";

        HBox chatItem = new HBox(view, textBox);
        chatItem.setMaxWidth(286);  // Set the preferred width
        chatItem.setMaxHeight(50);  // Set the preferred height
        chatItem.setStyle(baseStyle);
        chatItem.setSpacing(10);
        chatItem.setPadding(new Insets(8));
        chatItem.setAlignment(Pos.CENTER_LEFT);

        chatItem.setOnMouseEntered(e -> chatItem.setStyle(hoverStyle));
        chatItem.setOnMouseExited(e -> chatItem.setStyle(baseStyle));

        return chatItem;
    }

    //    === Message ===
    private  HBox createMessageHBox(Message msg, String roomId, boolean isSender) {
        VBox messageVBox = createMessageVBox(msg, roomId, isSender);
        HBox messageHBox = new HBox(messageVBox);
        messageHBox.setAlignment(isSender ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageHBox.setPadding(new Insets(5, 10, 5, 10));
        messageHBox.setId(msg.getMessageID());
        return messageHBox;
    }

    private  VBox createMessageVBox(Message msg, String roomId, boolean isSender) {
        final String bgColor = isSender ? "#A4F0E4" : "#0F3338";

        VBox vbox = new VBox();
        vbox.setSpacing(1);
        vbox.getChildren().add(createSenderLabel(msg, isSender));

        if (msg.isReply()) {
            vbox.getChildren().add(createReplyLabel(msg, isSender));
        }

        vbox.getChildren().addAll(
                createMessageLabel(msg, roomId, isSender),
                createFooter(msg, isSender)
        );

        vbox.setMinWidth(120);
        vbox.setMaxWidth(300);
        vbox.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 10; -fx-border-radius: 10;");
        return vbox;
    }

    private  Label createSenderLabel(Message msg, boolean isSender) {
        Label senderName = new Label(msg.getSenderName());
        senderName.setStyle("-fx-font-size: 12px; -fx-text-fill:" + (isSender ? "#000000;" : "#FFFFFF;"));
        senderName.setPadding(new Insets(3, 0, 0, 3));
        return senderName;
    }

    private  Label createReplyLabel(Message msg, boolean isSender) {
        Label repliedToLabel = new Label();
        Message reply = msg.getReplyTo();

        if (reply != null) {
            repliedToLabel.setText(reply.getText());
            repliedToLabel.setWrapText(true);
            repliedToLabel.setPadding(new Insets(10));
            repliedToLabel.setMaxWidth(300);
            repliedToLabel.setMaxHeight(100);

            String borderColor = isSender ? "#A4F0E4;" : "#0F3338;";
            String textColor = isSender ? "#000000;" : "#FFFFFF;";
            repliedToLabel.setStyle(
                    "-fx-background-color: #578F8B;" +
                            "-fx-background-radius: 15;" +
                            "-fx-border-color: " + borderColor +
                            "-fx-border-radius: 10;" +
                            "-fx-border-width: 5;" +
                            "-fx-font-size: 10px;" +
                            "-fx-text-fill: " + textColor
            );
        }
        return repliedToLabel;
    }

    private  Label createMessageLabel(Message msg, String roomId, boolean isSender) {
        Label label = new Label(msg.getText());
        label.setId("Label_" + roomId);
        chatPreview.add(label.getId());
        label.setWrapText(true);
        label.setPadding(new Insets(0, 10, 0, 10));
        label.setStyle("-fx-text-fill:" + (isSender ? "#000000;" : "#FFFFFF;") + " -fx-font-size: 12px;");
        return label;
    }

    private  HBox createFooter(Message msg, boolean isSender) {
        Label timeLabel = new Label(msg.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MMM/yyyy HH:mm")));
        timeLabel.setStyle("-fx-font-size: 8px; -fx-text-fill: " + (isSender ? "#000000;" : "#FFFFFF;"));
        timeLabel.setPadding(new Insets(0, 3, 3, 0));

        HBox footer = new HBox();
        footer.setSpacing(1);
        footer.setAlignment(Pos.BOTTOM_RIGHT);

        if (isSender) {
            Button seenBtn = imageButton("/Image/seen.png");
            seenBtn.setOnAction(e -> showMessageSeen(msg));
            footer.getChildren().add(seenBtn);
        }

        Button replyBtn = imageButton("/Image/reply.png");
        replyBtn.setOnAction(e -> msg.setReply(replyMessage(msg)));

        Button starBtn = starButton(msg);

        footer.getChildren().addAll(replyBtn, starBtn);

        if (isSender) {
            Button deleteBtn = imageButton("/Image/delete.png");
            deleteBtn.setOnAction(e -> deleteMessage(msg));
            footer.getChildren().add(deleteBtn);
        }

        footer.getChildren().add(timeLabel);
        return footer;
    }

    //    === Buttons ===
    private  Button imageButton(String path) {
        Image image = new Image(getClass().getResource(path).toExternalForm());
        ImageView view = new ImageView(image);
        view.setFitWidth(10);
        view.setFitHeight(10);

        Button button = new Button();
        button.setPrefSize(15, 15);
        button.setStyle("-fx-background-color: transparent;");
        button.setGraphic(view);
        return button;
    }

    private Button starButton(Message msg) {
        Image star = new Image(getClass().getResource("/Image/star.png").toExternalForm());
        Image unstar = new Image(getClass().getResource("/Image/star (1).png").toExternalForm());

        ImageView view = new ImageView(msg.isStarred() ? unstar : star);
        view.setFitWidth(10);
        view.setFitHeight(10);

        Button starButton = new Button();
        starButton.setPrefSize(15, 15);
        starButton.setStyle("-fx-background-color: transparent;");
        starButton.setGraphic(view);

        starButton.setOnAction(e -> {
            boolean wasStarred = msg.isStarred(); // store previous state
            msg.setStarred(!wasStarred);          // toggle

            if (!wasStarred) {
                Global.user.addStarMessage(msg.getMessageID(), currentRoom.getChatRoomID());
                view.setImage(unstar);
            } else {
                Global.user.removeStarMessage(msg.getMessageID(), currentRoom.getChatRoomID());
                view.setImage(star);
            }
        });

        return starButton;
    }

    //    === Images ===
    private ImageView ImageSetter(String path, int size) {
        try {
            Image image;

            String imageName = path.contains("/") || path.contains("\\") ? new File(path).getName() : path;
            String resourcePath = "/Image/ProfileImages/" + imageName;
            image = loadImage(resourcePath, "ProfileImages/" + imageName, size);

            return createCircularImageView(image, size);
        } catch (Exception e) {
            System.err.println("Critical error in ImageSetter: " + e.getMessage());
            ImageView view = new ImageView();
            view.setFitWidth(size);
            view.setFitHeight(size);
            return view;
        }
    }

    private Image loadImage(String resourcePath, String filePath, int size) {
        URL resourceUrl = getClass().getResource(resourcePath);
        URL defaultUrl = getClass().getResource("/Image/default_profile.png");

        if (resourceUrl != null) {
            return new Image(resourceUrl.toExternalForm());
        }

        if (filePath != null) {
            File imageFile = new File(filePath);
            if (imageFile.exists()) {
                return new Image(imageFile.toURI().toString());
            }
        }

        if (defaultUrl != null) {
            return new Image(defaultUrl.toExternalForm());
        }

        // Create a placeholder image if all else fails
        if (resourcePath.equals("/Image/defaultChat.png")) {
            WritableImage placeholder = new WritableImage(size, size);
            PixelWriter pixelWriter = placeholder.getPixelWriter();
            int radius = size / 2;

            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    double distance = Math.sqrt(Math.pow(x - radius, 2) + Math.pow(y - radius, 2));
                    pixelWriter.setColor(x, y, distance <= radius ? Color.DODGERBLUE : Color.TRANSPARENT);
                }
            }
            return placeholder;
        }

        return new WritableImage(size, size);
    }

    private ImageView createCircularImageView(Image image, int size) {
        ImageView view = new ImageView(image);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(false);

        int radius = size / 2;
        view.setClip(new Circle(radius, radius, radius));

        return view;
    }

    //    === Misc ===
    private void setChatAreaVisible(boolean bool) {
        ChatName.setVisible(bool);
        MsgScrollPane.setVisible(bool);
        textArea.setVisible(bool);
        sendBtn.setVisible(bool);
        if (currentRoom == null) {
            return;
        }
        starredMessages.setVisible(bool);
        if (currentRoom.getType().equals(ChatRoomType.ONE_TO_ONE)) {
            lastSeen.setVisible(bool);
            leaveGroup.setVisible(!bool);
        } else {
            lastSeen.setVisible(!bool);
            leaveGroup.setVisible(bool);
        }
        if (currentRoom.getAdmin().equals(Global.user.getMobileNumber()) && currentRoom.getType().equals(ChatRoomType.GROUP)) {
            newMember.setVisible(bool);
            removeMember.setVisible(bool);
        } else {
            newMember.setVisible(!bool);
            removeMember.setVisible(!bool);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String headerText, String contentText) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.initStyle(StageStyle.UTILITY); // Optional: makes the alert window minimalistic
        alert.showAndWait(); // Show the alert and wait for the Global.user to acknowledge
    }

    private void showDialog(String title, VBox content) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(stage);

        Scene scene = new Scene(content, 400, 350);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    private void closeCurrentDialog() {
        if (stage.getOwner() != null) {
            ((Stage) stage.getOwner()).close();
        }
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }
    //================================
}