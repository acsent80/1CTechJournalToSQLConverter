package com.acsent;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@SuppressWarnings({"UnusedParameters", "WeakerAccess"})
public class mainController implements Initializable {

    @FXML
    TextField dirText;
    @FXML
    GridPane gridPane;

    @FXML
    TableView<TableRow> filesTableView;
    @FXML
    TableColumn<TableRow, String> tableDirName;
    @FXML
    TableColumn<TableRow, String> tableFileName;
    @FXML
    TableColumn<TableRow, Long> tableFileSize;

    @FXML
    TextField connectionStringText;

    @FXML
    Label messageLabel;

    private Stage stage;

    private final ObservableList<TableRow> data = FXCollections.observableArrayList();

    public class TableRow {
        private final SimpleStringProperty dirName;
        private final SimpleStringProperty fileName;
        private final SimpleLongProperty fileSize;

        private TableRow(File file) {

            this.dirName = new SimpleStringProperty(file.getParent());
            this.fileName = new SimpleStringProperty(file.getName());

            Long size = file.length();
            size = size / (1024 * 1024);
            this.fileSize = new SimpleLongProperty(size);
        }

        public String getDirName() {
            return dirName.get();
        }

        public void setDirName(String value) {
            dirName.set(value);
        }

        public String getFileName() {
            return fileName.get();
        }

        public void setFileName(String value) {
            fileName.set(value);
        }

        public Long getFileSize() {
            return fileSize.get();
        }

        public void setFileSize(Long value) {
            fileSize.set(value);
        }

    }

    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setOnCloseRequest(this::stageOnClose);
    }

    public void initialize(URL url, ResourceBundle resourceBundle) {

        messageLabel.setText("");

        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        dirText.setText(prefs.get("Directory", ""));

        connectionStringText.setText("D:\\Temp");

        // Привязка таблицы к данным
        tableDirName.setCellValueFactory(new PropertyValueFactory<>("dirName"));
        tableFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        tableFileSize.setCellValueFactory(new PropertyValueFactory<>("fileSize"));

        filesTableView.setItems(data);

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

    public void processButtonOnAction(ActionEvent actionEvent) {

        messageLabel.setText("");
        data.clear();

        File rootFolder = new File(dirText.getText());
        if (!rootFolder.exists()) {
            messageLabel.setText("Каталог не найден!");
            return;
        }

        String[] folders = rootFolder.list((folder, name) -> name.startsWith("rphost"));

        for (String folderName : folders) {

            File curFolder = new File(rootFolder + "//" + folderName);
            String[] filesInFolder = curFolder.list((folder, name) -> name.endsWith(".log"));

            for (String fileName : filesInFolder) {
                File tmpFile = new File(curFolder + "//" + fileName);
                TableRow row = new TableRow(tmpFile);
                data.add(row);
            }

        }

    }

    public void ButtonOnAction(ActionEvent actionEvent) throws Exception {

        //Charset UTF = Charset.forName("UTF-8");
        String pathToFile = "D:\\Logs\\15091516.log1";
/*
        InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(pathToFile), "UTF-8");
        BufferedReader br = new BufferedReader(inputStreamReader);
        String line = br.readLine();
*/
        HashMap<String, String> tokens;// = new HashMap<>();


        int counter = 0;
        Parser parser = new Parser();
        parser.openFile(pathToFile);
        tokens = parser.parseNext();
        while (tokens != null) {
            System.out.println("-----------------");
            System.out.println(tokens.toString());
            counter++;
            if (counter == 15) break;
            tokens = parser.parseNext();
        }

        parser.closeFile();
        /*
        try {

            DB db = new DB();
            db.connect("TEST1");
            db.execSQLfromResource("/create.sql");
            db.close();

        } catch (SQLException e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        */
    }

}
