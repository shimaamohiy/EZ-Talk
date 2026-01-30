import Controllers.LoginCon;
import Utils.FilesUtil;
import Utils.Global;
import javafx.scene.image.Image;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        try{
            FilesUtil.readUsers();
            FilesUtil.readChatRooms();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Views/Login.fxml"));
            BorderPane root = loader.load();

            Scene scene1 = new Scene(root);
            stage.setTitle("EZ Talk");
            Image icon = new Image(getClass().getResourceAsStream("/Image/Designer1.png"));
            stage.getIcons().add(icon);
            stage.setOnCloseRequest(event -> {
                FilesUtil.writeUsers();
                FilesUtil.writeChatRooms();
                if (Global.user != null) {
                    Global.user.getProfile().setLastSeen();
                }
            });
            stage.setScene(scene1);
            stage.setResizable(false);
            stage.show();
            FilesUtil.writeUsers();
        }catch (Exception e) {
            FilesUtil.writeUsers();
            FilesUtil.writeChatRooms();
            if (Global.user != null) {
                Global.user.getProfile().setLastSeen();
            }
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
