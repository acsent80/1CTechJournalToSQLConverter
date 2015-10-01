package com.acsent;

import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;

public class TJLoader {

    public int readersCount;
    public int writersCount;
    public boolean oneReaderPerFile = false;
    public TableView<mainController.TableRow> filesTableView;

    public TJLoader() {
    }

    public void processAllFiles(ObservableList<mainController.TableRow> data) {

        if (oneReaderPerFile) {
            readersCount = data.size();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(readersCount);
        data.forEach(tableRow ->
                executorService.submit(new ParserTask(tableRow)));

        executorService.shutdown();

    }

    class ParserTask implements Runnable {

        mainController.TableRow tableRow;

        public ParserTask(mainController.TableRow tableRow) {
            this.tableRow = tableRow;
        }

        @Override
        public void run() {

            tableRow.setStatus("+");
            filesTableView.refresh();

            String fileName = tableRow.getDirName() + "\\" + tableRow.getFileName();

            try {

                BlockingQueue<HashMap<String, String>> tokensQueue = new ArrayBlockingQueue<>(128 * writersCount, true);

                ExecutorService executorService = Executors.newFixedThreadPool(writersCount);

                for (int i = 0; i < writersCount; i++) {
                    executorService.submit(new DBTask(tokensQueue, tableRow));
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

                while (parser.hasNext()) {

                    tokens = parser.next();

                    tokensQueue.put(tokens);
                    counter++;
                    System.out.println(counter);
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
    }

    class DBTask implements Runnable {

        private BlockingQueue<HashMap<String, String>> queue;
        private mainController.TableRow tableRow;

        public DBTask(BlockingQueue<HashMap<String, String>> queue, mainController.TableRow tableRow) {
            this.queue          = queue;
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

}
