package com.acsent;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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
    TableColumn<TableRow, String> tableStatus;
    @FXML
    TableColumn<TableRow, Integer> tableQty;

    @FXML
    TextField connectionStringText;

    @FXML
    Button findFilesButton;
    @FXML
    Button clearTableButton;
    @FXML
    Button processButton;
    @FXML
    Label messageLabel;

    private Stage stage;

    private final ObservableList<TableRow> data = FXCollections.observableArrayList();
    private HashMap<String, TableRow> rowsByFile;

    public class TableRow {
        private final SimpleStringProperty dirName;
        private final SimpleStringProperty fileName;
        private final SimpleLongProperty fileSize;
        private final SimpleStringProperty status;
        private final SimpleIntegerProperty qty;

        private TableRow() {
            this.dirName  = new SimpleStringProperty(null);
            this.fileName = new SimpleStringProperty(null);
            this.fileSize = new SimpleLongProperty(0L);
            this.status   = new SimpleStringProperty(null);
            this.qty      = new SimpleIntegerProperty(0);
        }

        private TableRow(File file, Long fileSize) {

            this.dirName = new SimpleStringProperty(file.getParent());
            this.fileName = new SimpleStringProperty(file.getName());

            fileSize = fileSize / (1024 * 1024);
            this.fileSize = new SimpleLongProperty(fileSize);
            this.status   = new SimpleStringProperty("");
            this.qty      = new SimpleIntegerProperty(0);
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

        public String getStatus() {
            return status.get();
        }

        public void setStatus(String value) {
            status.set(value);
        }

        public void setQty(Integer value) {
            qty.set(value);
        }

        public Integer getQty() {
            return qty.get();
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
        tableDirName. setCellValueFactory(new PropertyValueFactory<>("dirName"));
        tableFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        tableFileSize.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        tableStatus.  setCellValueFactory(new PropertyValueFactory<>("status"));
        tableQty.     setCellValueFactory(new PropertyValueFactory<>("qty"));

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

    public void findFilesButtonOnAction(ActionEvent actionEvent) {

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
                Long size = tmpFile.length();

                if (size > 0) {
                    TableRow row = new TableRow(tmpFile, size);
                    data.add(row);
                }
            }

        }

    }

    public void clearTableButtonOnAction(ActionEvent actionEvent) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("Clear table 'logs'?");
        //alert.setContentText("Clear table 'logs'?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() != ButtonType.OK){
            return;
        }

        DBTools db = new DBTools("sqlite");
        try {

            db.connect("", "TEST1", "", "", true);
            db.execSQLFromResource("/create.sql");
            db.execute("DELETE FROM [logs]");
            db.close();

            messageLabel.setText("Таблица очищена");

        } catch (Exception e) {
            messageLabel.setText(e.toString());
            e.printStackTrace();
        }

    }

    class UpdateTableThreadListener implements TJLoader.ThreadListener {

        public synchronized void setProgress(String fileName, int counter) {

            TableRow tableRow = rowsByFile.get(fileName);

            if (tableRow != null) {

                if (counter != 0) {
                    tableRow.setQty(counter);
                    System.out.println(counter);
                }

                 filesTableView.refresh();
                //filesTableView.getProperties().put(TableViewSkinBase.REFRESH, Boolean.TRUE);
            }
        }


        public synchronized void setStatus(String fileName, TJLoader.Status status) {

            TableRow tableRow = rowsByFile.get(fileName);

            if (tableRow != null) {

                if (status == TJLoader.Status.BEGIN) {
                    tableRow.setStatus("+");
                } else if (status == TJLoader.Status.DONE) {
                    tableRow.setStatus("V");
                    System.out.println("end");
                }

                filesTableView.refresh();
                //filesTableView.getProperties().put(TableViewSkinBase.REFRESH, Boolean.TRUE);
            }
        }

    }

    public void processButtonOnAction(ActionEvent actionEvent) throws Exception {

        try {

            DBTools db = new DBTools("sqlite");
            db.connect("", "TEST1", "", "", true);
            db.execSQLFromResource("/create.sql");
            db.close();

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        UpdateTableThreadListener updateTableThreadListener = new UpdateTableThreadListener();

        TJLoader tjLoader = new TJLoader();
        tjLoader.readersCount = 1;
        tjLoader.writersCount = 1;
        tjLoader.addListener(updateTableThreadListener);

        ArrayList<String> filesArrayList = new ArrayList<>();

        rowsByFile = new HashMap<>();
        for (TableRow tableRow: data) {
            String fileName = tableRow.getDirName() + "\\" + tableRow.getFileName();
            rowsByFile.put(fileName, tableRow);
            filesArrayList.add(fileName);
        }

        tjLoader.processAllFiles(filesArrayList);

    }

}


