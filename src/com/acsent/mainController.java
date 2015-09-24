package com.acsent;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class mainController implements Initializable{

    @FXML
    TextField dirText;
    @FXML
    GridPane gridPane;
    @FXML
    Label testLabel;

    //private Preferences prefs;
    private Stage stage;

    public void setStage(Stage stage) {

        this.stage = stage;

        stage.setOnCloseRequest(this::stageOnClose);

    }

    public void initialize(URL url, ResourceBundle resourceBundle) {

        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        dirText.setText(prefs.get("Directory", ""));

     }

    public void stageOnClose(WindowEvent windowEvent) {

        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        prefs.put("Directory", dirText.getText());
    }

    public void dirButtonOnAction(ActionEvent actionEvent) {

        DirectoryChooser directoryChooser = new DirectoryChooser();

        File initialDirectory = new File(dirText.getText());
        if (initialDirectory.exists()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

        File selectedDir = directoryChooser.showDialog(stage);

        if (selectedDir != null) {
            dirText.setText(selectedDir.getPath());
        }
    }
}
