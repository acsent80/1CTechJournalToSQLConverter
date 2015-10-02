package com.acsent;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class optionsController implements Initializable {

    @FXML
    ComboBox<String> dbDriverComboBox;
    @FXML
    TextField serverText;
    @FXML
    TextField databaseText;
    @FXML
    TextField tableText;
    @FXML
    Button selectDatabaseButton;

    private Stage stage;

    public void initialize(URL url, ResourceBundle resourceBundle) {

        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        serverText.setText(  prefs.get("Server",   ""));
        databaseText.setText(prefs.get("Database", "logs"));
        tableText.setText(   prefs.get("Table",    "logs"));

        dbDriverComboBox.getItems().addAll(
                "SQLite",
                "MS SQL"
        );

        dbDriverComboBox.setValue(prefs.get("dbDriver", "SQLite"));
        setControlStatus();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void cancelButtonOnAction(ActionEvent actionEvent) throws IOException {
        stage.close();
    }

    public void okButtonOnAction(ActionEvent actionEvent) throws IOException {

        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        prefs.put("Server",   serverText.getText());
        prefs.put("Database", databaseText.getText());
        prefs.put("Table",    tableText.getText());

        prefs.put("dbDriver", dbDriverComboBox.getValue());

        stage.close();
    }
    public void dbDriverComboBoxOnAction(ActionEvent actionEvent) throws IOException {
        setControlStatus();
    }

    public void selectDatabaseButtonOnAction(ActionEvent actionEvent) throws IOException {

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("SQLite database (*.s3db)", "*.s3db"));

        File currentDatabase = new File(databaseText.getText());
        fileChooser.setInitialFileName(currentDatabase.getName());

        File selectedFile = fileChooser.showSaveDialog(stage);

        if (selectedFile != null) {
            databaseText.setText(selectedFile.getPath());
        }
    }

    private void setControlStatus() {

        String dbDriver = dbDriverComboBox.getValue();
        if (dbDriver.equals("SQLite")) {
            serverText.setDisable(true);
            selectDatabaseButton.setDisable(false);
        } else {
            serverText.setDisable(false);
            selectDatabaseButton.setDisable(true);
        }
    }
}
