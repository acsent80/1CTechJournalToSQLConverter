package com.acsent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBTools {

    public static Connection connection;
    public static Statement statement;

    public static void connect(String dbName) throws SQLException, ClassNotFoundException {

        Class.forName("org.sqlite.JDBC");

        //String connectionUrl = "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;";
        String connectionUrl = "jdbc:sqlite:" + dbName + ".s3db";
        connection = DriverManager.getConnection(connectionUrl, "", "");

        statement = connection.createStatement();
    }

    public static void execute(String sqlText) throws SQLException {

        System.out.println(statement);
        statement.execute(sqlText);
    }

    public static void close() throws SQLException {
        connection.close();
    }
}
