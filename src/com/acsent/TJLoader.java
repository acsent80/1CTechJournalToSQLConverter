package com.acsent;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TJLoader {

    public DBTools.DriverType driverType;
    public String serverName;
    public String databaseName;
    public String user;
    public String password;
    public boolean integretedSecurity;

    public int readersCount;
    public int writersCount;
    public boolean oneReaderPerFile = false;

    private ThreadListener threadListener;

    public enum Status {BEGIN, DONE}

    public TJLoader() {
    }

    public void processAllFiles(ArrayList<String> filesArrayList) {

        if (oneReaderPerFile) {
            readersCount = filesArrayList.size();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(readersCount);
        filesArrayList.forEach(fileName ->
                executorService.submit(new ParserTask(fileName)));

        executorService.shutdown();

    }

    public interface ThreadListener {
        void setProgress(String fileName, int counter);
        void setStatus(String fileName, Status status);
    }

    public void addListener(ThreadListener threadListener) {
        this.threadListener = threadListener;
    }

    //   class ParserTask implements Runnable {
    class ParserTask extends Thread {

        private AtomicInteger processedTokensCount;
        private AtomicBoolean done;
        private CountDownLatch doneSignal;
        private String fileName;

        public ParserTask(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void run() {

            super.run();
            if (threadListener != null) {
                threadListener.setStatus(fileName, Status.BEGIN);
            }

            processedTokensCount = new AtomicInteger(0);
            done = new AtomicBoolean(false);
            doneSignal = new CountDownLatch(writersCount);

            try {

                BlockingQueue<HashMap<String, String>> tokensQueue = new ArrayBlockingQueue<>(128 * writersCount, true);

                ExecutorService executorService = Executors.newFixedThreadPool(writersCount);

                for (int i = 0; i < writersCount; i++) {
                    executorService.submit(new DBTask(tokensQueue));
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
                    if (counter == 500) break;
                }

                done.set(true);

                executorService.shutdown();
                parser.closeFile();

                if (threadListener != null) {
                    doneSignal.await();
                    threadListener.setStatus(fileName, Status.DONE);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        class DBTask implements Runnable {

            private BlockingQueue<HashMap<String, String>> tokensQueue;

            public DBTask(BlockingQueue<HashMap<String, String>> tokensQueue) {
                this.tokensQueue = tokensQueue;
            }

            @Override
            public void run() {

                DBTools db = new DBTools(driverType);

                try {
                    db.connect(serverName, databaseName, user, password, integretedSecurity);
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

                PreparedStatement preparedStatement;
                try {
                    preparedStatement = db.prepareInsertStatement("logs", fields);
                } catch (SQLException e) {
                    e.printStackTrace();
                    return;
                }

                try {
                    db.beginTransaction();
                } catch (SQLException e) {
                    e.printStackTrace();
                    return;
                }

                int counter = 0;
                int listenerPortionSize = 50;
                int dbPortionSize = 100;

                try {
                    while (!done.get() || !tokensQueue.isEmpty()) {

                        HashMap<String, String> tokens = tokensQueue.take();
                        counter++;

                        try {

                            db.insertValues(preparedStatement, fields, tokens);

                            if (counter % listenerPortionSize == 0) {

                                if (threadListener != null) {
                                    int value = processedTokensCount.addAndGet(listenerPortionSize);
                                    threadListener.setProgress(fileName, value);
                                }

                            }

                            if (counter % dbPortionSize == 0) {
                                db.commitTransaction();
                                db.beginTransaction();
                            }

                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }

                    if (threadListener != null && (counter % listenerPortionSize != 0)) {
                        int value = processedTokensCount.addAndGet(counter % listenerPortionSize);
                        threadListener.setProgress(fileName, value);
                    }

                    doneSignal.countDown();

                    try {
                        db.commitTransaction();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    try {
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
