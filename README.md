**EZ-Talk** is a modern, Java-based chat application that combines real-time messaging, ephemeral stories, contact management, and group conversations in an intuitive interface. Inspired by popular messaging platforms, EZ-Talk provides a seamless and secure communication experience.

---

## 🚀 Features
- **Private & Group Chats:** One-on-one conversations or group chats with multiple users.  
- **Message Management:** Reply, star important messages, and track read receipts.  
- **Ephemeral Stories:** Share temporary updates that disappear after 24 hours.  
- **Contact System:** Add and manage contacts with mutual connection detection.  
- **User Profiles:** Customize your profile with bio, photos, and privacy settings.  
- **Search Functionality:** Quickly find messages and contacts.  
- **Security:** Secure authentication and user management to protect your data.


---

## 🧱 Project Structure
The application uses **MVC (Model-View-Controller) architecture**:

**Models**
- `User`: Stores user information, contacts, and preferences.  
- `ChatRoom`: Manages one-to-one and group conversations.  
- `Message`: Handles message content, timestamps, sender info, and read status.  

**Views**
- JavaFX-based UI components for chat windows, contact lists, story displays, and profile management.  

**Controllers**
- Handle user interactions, update models, and refresh views.

---

## 🛠️ Installation

### Prerequisites
- Java Development Kit (JDK) 17 or higher  
- JavaFX SDK 23 or higher  

### Steps

1. **Clone the Repository**

   ```bash
   git clone https://github.com/shimaamohiy/EZ-Talk.git
   cd EZ-Talk

2. **Build the Project**

    ```bash
   mvn clean install

3. **Set the Project Structure**
   Add
   ```bash
     --module-path YOUR PATH(JavaFX lib Path) --add-modules javafx.controls,javafx.fxml
   ```
   to the VM Options in Run Configurations
   
   Include JavaFX in the Project Structure Modules and Libraries
  
5. **Run the Application**

     ```bash
    mvn javafx:run
