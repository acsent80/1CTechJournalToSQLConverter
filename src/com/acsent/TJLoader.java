package com.acsent;

import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TJLoader {

    public int readersCount;
    public int writersCount;
    public boolean oneReaderPerFile = false;
    public TableView<mainController.TableRow> filesTableView;

    private ThreadListener threadListener;

    public enum Status {BEGIN, DONE}

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

    public interface ThreadListener {
        void onProgress(String fileName, Status status, int counter);
    }

    public void addListener(ThreadListener threadListener) {
        this.threadListener = threadListener;
    }

    //   class ParserTask implements Runnable {
    class ParserTask extends Thread {

        private mainController.TableRow tableRow;
        private AtomicInteger processedTokensCount;
        private String fileName;

        public ParserTask(mainController.TableRow tableRow) {
            this.tableRow = tableRow;
            this.fileName = tableRow.getDirName() + "\\" + tableRow.getFileName();
        }

        @Override
        public void run() {

            super.run();
            if (threadListener != null) {
                threadListener.onProgress(fileName, Status.BEGIN, 0);
            }

            processedTokensCount = new AtomicInteger(0);

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
                    if (counter == 1000) break;
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
                int portionSize = 50;

                try {
                    HashMap<String, String> tokens;
                    while (true) {

                        tokens = queue.take();

                        if (tokens.get("DONE") != null) {
                            break;
                        }

                        counter++;

                        try {
                            db.insertValues("logs", fields, tokens);

                            if (counter % portionSize == 0) {

                                if (threadListener != null) {
                                    int value = processedTokensCount.addAndGet(portionSize);
                                    threadListener.onProgress(fileName, null, value);
                                }

                            }

                            if (counter % 100 == 0) {
                                db.execute("COMMIT");

                                db.execute("BEGIN TRANSACTION");
                            }

                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    if (threadListener != null) {

                        int value = processedTokensCount.addAndGet(portionSize);
                        threadListener.onProgress(fileName, Status.DONE, value);
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

    }

}
