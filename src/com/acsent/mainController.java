package com.acsent;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javax.swing.*;
import java.io.File;

public class mainController {

    @FXML
    javafx.scene.control.Label dirLabel;
    @FXML
    javafx.scene.control.TextField  dirText;

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
