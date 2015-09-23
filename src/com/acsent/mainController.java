package com.acsent;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class mainController implements Initializable {

    @FXML
    javafx.scene.control.Label dirLabel;
    @FXML
    javafx.scene.control.TextField  dirText;
    @FXML
    javafx.scene.control.Button  dirButton;
    @FXML
    javafx.scene.control.Button  processButton;

    public void initialize(URL url, ResourceBundle resourceBundle) {
        dirText.setText("123");
    }

    public void dirButtonOnAction(ActionEvent actionEvent) {

        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            dirText.setText(selectedFile.getPath());
        }

    }
}
