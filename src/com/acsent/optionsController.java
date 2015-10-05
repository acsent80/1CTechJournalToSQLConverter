package com.acsent;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
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
    TextField userText;
    @FXML
    PasswordField passwordText;
    @FXML
    Button selectDatabaseButton;
    @FXML
    CheckBox integratedSecurityCheckBox;

    @FXML
    TextField readingTreadsText;
    @FXML
    TextField writingTreadsText;
    @FXML
    CheckBox oneThreadPerFileCheckBox;

    private Stage stage;

    public String connectionString;

    public void initialize(URL url, ResourceBundle resourceBundle) {

        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        serverText.setText(  prefs.get("Server",   ""));
        databaseText.setText(prefs.get("Database", "logs"));
        tableText.setText(   prefs.get("Table",    "logs"));
        integratedSecurityCheckBox.setSelected(prefs.getBoolean("IntegratedSecurity", false));

        dbDriverComboBox.getItems().addAll(
                "SQLite",
                "MS SQL"
        );

        dbDriverComboBox.setValue(prefs.get("dbDriver", "SQLite"));

        readingTreadsText.setText(String.valueOf(prefs.getInt("ReadersCount", 1)));
        writingTreadsText.setText(String.valueOf(prefs.getInt("WritersCount", 1)));
        oneThreadPerFileCheckBox.setSelected(prefs.getBoolean("OneReaderPerFile", false));

        setControlStatus();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void cancelButtonOnAction(ActionEvent actionEvent) throws IOException {
        stage.close();
    }

    public void okButtonOnAction(ActionEvent actionEvent) throws IOException {

        String serverName = serverText.getText();
        String dbName     = databaseText.getText();
        String driverType = dbDriverComboBox.getValue();
        boolean integratedSecurity = integratedSecurityCheckBox.isSelected();

        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        prefs.put("Server",   serverName);
        prefs.put("Database", dbName);
        prefs.put("Table",    tableText.getText());
        prefs.put("user",     userText.getText());
        prefs.put("password", passwordText.getText());
        prefs.putBoolean("IntegratedSecurity", integratedSecurity);
        prefs.putInt("ReadersCount", Integer.valueOf(readingTreadsText.getText()));
        prefs.putInt("WritersCount", Integer.valueOf(writingTreadsText.getText()));
        prefs.putBoolean("OneReaderPerFile", oneThreadPerFileCheckBox.isSelected());

        prefs.put("dbDriver", driverType);

        if (driverType.equals("SQLite")) {
            connectionString = "jdbc:sqlite:" + dbName;
        } else if (driverType.equals("MS SQL")) {
            connectionString = "jdbc:sqlserver://" + serverName + " :1433;databaseName=" + dbName + ";IntegratedSecurity=" + String.valueOf(integratedSecurity);
        }

        stage.close();
    }

    public void dbDriverComboBoxOnAction(ActionEvent actionEvent) {
        setControlStatus();

        String dbDriver = dbDriverComboBox.getValue();
        if (dbDriver.equals("SQLite")) {
            readingTreadsText.setText("1");
            writingTreadsText.setText("1");
            oneThreadPerFileCheckBox.setSelected(false);
        }
    }

    public void integratedSecurityCheckBoxOnAction(ActionEvent actionEvent) {
        setControlStatus();
    }

    public void oneThreadPerFileCheckBoxOnAction(ActionEvent actionEvent) {
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
            userText.setDisable(true);
            passwordText.setDisable(true);
            selectDatabaseButton.setDisable(false);

            readingTreadsText.setDisable(true);
            writingTreadsText.setDisable(true);
            oneThreadPerFileCheckBox.setDisable(true);

        } else {
            serverText.setDisable(false);
            userText.setDisable(false);
            passwordText.setDisable(false);
            selectDatabaseButton.setDisable(true);

            readingTreadsText.setDisable(false);
            writingTreadsText.setDisable(false);
            oneThreadPerFileCheckBox.setDisable(false);

            if (oneThreadPerFileCheckBox.isSelected()) {
                readingTreadsText.setDisable(true);
            } else {
                readingTreadsText.setDisable(false);
            }

        }

        if (integratedSecurityCheckBox.isSelected()) {
            userText.setDisable(true);
            passwordText.setDisable(true);
        } else {
            userText.setDisable(false);
            passwordText.setDisable(false);
        }

    }
}
