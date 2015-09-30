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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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

        BlockingQueue<String> filesQueue          = new ArrayBlockingQueue<>(data.size() + 1, true);
        BlockingQueue<String> processedFilesQueue = new ArrayBlockingQueue<>(data.size() + 1, true);

        for (TableRow tableRow: data) {

            String dirName  = tableRow.getDirName();
            String fileName = tableRow.getFileName();

            filesQueue.add(dirName + "\\" + fileName);
        }
        filesQueue.add("DONE");

        Thread thread = new Thread(new ReadFromTJThread(filesQueue, processedFilesQueue, 1));
        thread.start();

    }

}

class ReadFromTJThread implements Runnable {

    private BlockingQueue<HashMap<String, String>> tokensQueue;
    private BlockingQueue<String> filesQueue;
    private BlockingQueue<String> processedFilesQueue;
    private int writersCount;

    public ReadFromTJThread(BlockingQueue<String> filesQueue, BlockingQueue<String> processedFilesQueue, int writersCount) {
        this.filesQueue          = filesQueue;
        this.processedFilesQueue = processedFilesQueue;
        this.writersCount        = writersCount;
    }

    @Override
    public void run() {

        while (true) {

            try {

                String fileName = filesQueue.take();
                if (fileName.equals("DONE")) {
                    break;
                }

                // auto start queue reader
                if (tokensQueue == null) {
                    tokensQueue = new ArrayBlockingQueue<>(128, true);

                    for (int i = 1; i <= writersCount; i++){
                        (new Thread(new WriteToSQLThread(tokensQueue))).start();
                    }
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

                parser.closeFile();

                processedFilesQueue.add(fileName);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
}

class WriteToSQLThread implements Runnable {

     private BlockingQueue<HashMap<String, String>> queue;

     public WriteToSQLThread(BlockingQueue<HashMap<String, String>> queue) {
         this.queue = queue;
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
                 counter++;

                 if (tokens.get("DONE") != null) {
                     break;
                 }

                 try {
                     db.insertValues("logs", fields, tokens);
                     if (counter % 100 == 0) {
                         db.execute("COMMIT");
                         db.execute("BEGIN TRANSACTION");
                     }

                 } catch (SQLException e) {
                     e.printStackTrace();
                 }
             }

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

