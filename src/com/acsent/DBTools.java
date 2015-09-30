package com.acsent;

import java.io.InputStream;
import java.sql.*;
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

        statement.execute(sqlText);
    }

    public ResultSet executeQuery(String sqlText) throws SQLException {

       return statement.executeQuery(sqlText);
    }

    public void insertValues(String tableName, ArrayList<String> fields, HashMap<String, String> values) throws SQLException {

        System.out.println(values);
        String sqlText = "INSERT INTO " + tableName + "(";

        //statement.execute(sqlText);

    }

    public ArrayList<String> getTableColumns(String tableName) throws SQLException  {

        ArrayList<String> arrayList = new ArrayList<>();

        ResultSet resultSet = executeQuery("PRAGMA table_info('" + tableName + "')");
        while (resultSet.next()) {
            arrayList.add(resultSet.getString("name"));
        }

        resultSet.close();
        return arrayList;
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
