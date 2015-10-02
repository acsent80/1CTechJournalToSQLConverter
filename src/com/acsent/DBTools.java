package com.acsent;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

class DBTools {

    private Connection connection;
    private DriverType driverType;

    public enum DriverType{SQLite, MSSQL}

    public DBTools(DriverType driverType) {
        this.driverType = driverType;
    }

    public void connect(String serverName, String dbName, String user, String password, boolean integratedSecurity) throws SQLException, ClassNotFoundException {

        if (driverType == DriverType.SQLite) {
            connectSQLite(dbName);
        } else if (driverType == DriverType.MSSQL) {
            connectMSSQL(serverName, dbName, user, password, integratedSecurity);
        }

    }

    public void connectSQLite(String dbName) throws SQLException, ClassNotFoundException {

        Class.forName("org.sqlite.JDBC");
        String connectionUrl = "jdbc:sqlite:" + dbName + ".s3db";

        connection = DriverManager.getConnection(connectionUrl, "", "");

    }

    public void connectMSSQL(String serverName, String dbName, String user, String password, boolean integratedSecurity) throws SQLException, ClassNotFoundException {

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String connectionUrl = "jdbc:sqlserver://" + serverName + " :1433;databaseName=" + dbName;

        if (integratedSecurity) {
            connectionUrl = connectionUrl + ";IntegratedSecurity=true";
        }

        connection = DriverManager.getConnection(connectionUrl, user, password);

    }

    public void beginTransaction() throws SQLException {
        if (driverType == DriverType.SQLite) {
            execute("PRAGMA journal_mode = MEMORY");
            execute("BEGIN TRANSACTION");
        }
    }

    public void commitTransaction() throws SQLException {
        if (driverType == DriverType.SQLite) {
            execute("COMMIT");
        }
    }

    PreparedStatement prepareInsertStatement(String tableName, ArrayList<String> fields) throws SQLException {

        StringBuilder sqlText = new StringBuilder(1024);
        sqlText.append("INSERT INTO ").append(tableName).append("(");

        StringBuilder strFields = new StringBuilder(1024);
        StringBuilder strValues = new StringBuilder(1024);

        for (String field : fields) {

            if (strFields.length() > 0) {
                strFields.append(", ");
                strValues.append(", ");
            }
            strFields.append(field);
            strValues.append("?");
        }

        sqlText.append(strFields);
        sqlText.append(") VALUES (");
        sqlText.append(strValues);
        sqlText.append(")");

        return connection.prepareStatement(sqlText.toString());

    }

    public void insertValues(PreparedStatement preparedStatement, ArrayList<String> fields, HashMap<String, String> values) throws SQLException {

        int fieldNumber = 1;
        for (String field : fields) {

            String fieldValue = values.get(field);
            preparedStatement.setString(fieldNumber, fieldValue);

            fieldNumber++;
        }

        preparedStatement.execute();
    }

     public ArrayList<String> getTableColumns(String tableName) throws SQLException  {

        ArrayList<String> arrayList = new ArrayList<>();

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("PRAGMA table_info('" + tableName + "')");
        while (resultSet.next()) {
            arrayList.add(resultSet.getString("name"));
        }

        resultSet.close();
        statement.close();
        return arrayList;
    }

    public void close() throws SQLException {
        connection.close();
    }

    public void execSQLFromResource(String resourceName) throws SQLException {

        InputStream inputStream = Main.class.getResourceAsStream(resourceName);

        if (inputStream != null) {

            String sqlText = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
            execute(sqlText);

        }
    }

    public void execute(String sqlText) throws SQLException {

        Statement statement = connection.createStatement();
        statement.execute(sqlText);
        statement.close();
    }
}
