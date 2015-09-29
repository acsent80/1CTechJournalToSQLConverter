package com.acsent;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

class DBTools {

    private Connection connection;
    private Statement statement;

    public void connect(String dbName) throws SQLException, ClassNotFoundException {

        Class.forName("org.sqlite.JDBC");

        //String connectionUrl = "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;";
        String connectionUrl = "jdbc:sqlite:" + dbName + ".s3db";
        connection = DriverManager.getConnection(connectionUrl, "", "");

        statement = connection.createStatement();
    }

    public void execute(String sqlText) throws SQLException {

        System.out.println(statement);
        statement.execute(sqlText);
    }

    public void insertValues(String tableName, ArrayList<String> fields, HashMap<String, String> values) throws SQLException {

        String sqlText = "INSERT INTO " + tableName + "(";

        statement.execute(sqlText);

    }

    public void close() throws SQLException {
        connection.close();
    }

    public void execSQLfromResource(String resourceName) throws SQLException {

        InputStream inputStream = Main.class.getResourceAsStream(resourceName);

        if (inputStream != null) {
            String sqlText = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
            execute(sqlText);
        }
    }
}
