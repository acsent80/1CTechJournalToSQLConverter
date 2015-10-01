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
import java.util.ResourceBundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.scene.control.*;
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

        private TableRow(File file) {

            this.dirName = new SimpleStringProperty(file.getParent());
            this.fileName = new SimpleStringProperty(file.getName());

            Long size = file.length();
            size = size / (1024 * 1024);
            this.fileSize = new SimpleLongProperty(size);
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
                TableRow row = new TableRow(tmpFile);
                data.add(row);
            }

        }

    }

    public void clearTableButtonOnAction(ActionEvent actionEvent) {

        DBTools db = new DBTools("sqlite");
        try {

            db.connect("", "TEST1", "", "", true);
            db.execSQLfromResource("/create.sql");
            db.execute("DELETE FROM [logs]");
            db.close();

            messageLabel.setText("Таблица очищена");

        } catch (Exception e) {
            messageLabel.setText(e.toString());
            e.printStackTrace();
        }

    }

    public void processButtonOnAction(ActionEvent actionEvent) throws Exception {

        try {

            DBTools db = new DBTools("sqlite");
            db.connect("", "TEST1", "", "", true);
            db.execSQLfromResource("/create.sql");
            db.close();

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

//        processButton.setDisable(true);

        ExecutorService executorService = Executors.newFixedThreadPool(1);
        data.forEach(tableRow ->
                executorService.submit(
                        () -> processLogFile(tableRow)));

        executorService.shutdown();
//        final boolean done = executorService.awaitTermination(100, TimeUnit.MINUTES);
//        processButton.setDisable(false);

    }

    private void processLogFile(TableRow tableRow) {

        String fileName = tableRow.getDirName() + "\\" + tableRow.getFileName();
        int writersCount = 1;

        try {

            BlockingQueue<HashMap<String, String>> tokensQueue = new ArrayBlockingQueue<>(128 * writersCount, true);

            ExecutorService executorService = Executors.newFixedThreadPool(writersCount);

            for (int i = 0; i < writersCount; i++) {
                executorService.submit(new WriteToSQLThread(tokensQueue, filesTableView, tableRow));
            }

            HashMap<String, String> tokens;

            Parser parser = new Parser();
            try {
                parser.openFile(fileName);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            int counter = 0;

            while ((tokens = parser.parseNext()) != null) {

                tokensQueue.put(tokens);
                counter++;
                if (counter == 300) break;
            }

            HashMap<String, String> endOfQueue = new HashMap<>();
            endOfQueue.put("DONE", "DONE");
            tokensQueue.put(endOfQueue);

            executorService.shutdown();
            parser.closeFile();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertIntoDB(HashMap<String, String> tokens) {

    }
}

class WriteToSQLThread implements Runnable {

    private BlockingQueue<HashMap<String, String>> queue;
    private TableView<mainController.TableRow> filesTableView;
    private mainController.TableRow tableRow;

    public WriteToSQLThread(BlockingQueue<HashMap<String, String>> queue, TableView<mainController.TableRow> filesTableView, mainController.TableRow tableRow) {
        this.queue          = queue;
        this.filesTableView = filesTableView;
        this.tableRow       = tableRow;
    }

    @Override
    public void run() {

        DBTools db = new DBTools("sqlite");

        try {
            db.connect("", "TEST1", "", "", true);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        ArrayList<String> fields;
        try {
            fields = db.getTableColumns("logs");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        try {
            db.execute("PRAGMA journal_mode = MEMORY");
            db.execute("BEGIN TRANSACTION");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int counter = 0;
        try {
            HashMap<String, String> tokens;
            while (true) {

                tokens = queue.take();

                if (tokens.get("DONE") != null) {
                    break;
                }

                counter++;
                System.out.println(counter);

                try {
                    db.insertValues("logs", fields, tokens);
                    if (counter % 100 == 0) {
                        db.execute("COMMIT");

                        tableRow.setQty(counter);
                        filesTableView.refresh();

                        db.execute("BEGIN TRANSACTION");
                    }

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            tableRow.setStatus("V");
            tableRow.setQty(counter);
            filesTableView.refresh();

            try {
                db.execute("COMMIT");
                db.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (InterruptedException intEx) {
            System.out.println("Interrupted! " +
                    "Last one out, turn out the lights!");
        }

    }
 }

