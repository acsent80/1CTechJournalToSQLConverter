package com.acsent;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        URL url = getClass().getResource("main.fxml");

        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();

        mainController controller = loader.getController();
        controller.setStage(primaryStage);

        Scene scene = new Scene(root, 300, 275);
        primaryStage.setScene(scene);

        primaryStage.setTitle("My first java fx app");
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}

