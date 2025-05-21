package io.github.kambaa.javafxdemo;

import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class JavaFxApplication extends Application {
  @Override
  public void start(Stage stage) throws IOException {
    FXMLLoader fxmlLoader = new FXMLLoader(JavaFxApplication.class.getResource("Main.fxml"));
    Scene scene = new Scene(fxmlLoader.load());
    stage.setResizable(false);
    stage.getIcons().add(new Image("file:src/main/resources/icon.png"));
    URL iconUrl = getClass().getResource("/icon.png");
    if (iconUrl != null) {
      stage.getIcons().add(new Image(iconUrl.toString()));
    } else {
      System.err.println("Icon not found!");
    }
    // Get the controller instance and inject the stage
    MainController controller = fxmlLoader.getController();
    controller.setPrimaryStage(stage);
    stage.setTitle("JDK8+ Cert Importer By Kambaa!");
    stage.setScene(scene);
    stage.show();
  }

  public static void main(String[] args) {
    launch();
  }
}