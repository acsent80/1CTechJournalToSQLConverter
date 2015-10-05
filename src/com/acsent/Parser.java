package com.acsent;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class Parser implements Iterator<HashMap<String, String>> {

    public char separator = ',';
    public char quoteChar1 = '\'';
    public char quoteChar2 = '"';

    private boolean inQuotes = false;
    private Integer fieldNumber = 1;
    private char openQuoteChar = '"';
    private String fieldName = "";

    private String pending = null;

    private String fileName;
    private BufferedReader bufferedReader;

    private Pattern regexpFrom = Pattern.compile("\\sFROM\\s+([^ ,]+)(?:\\s*,\\s*([^ ,]+))*\\s+");
    private Pattern regexpJoin = Pattern.compile("\\sJOIN\\s+([^ ,]+)(?:\\s*,\\s*([^ ,]+))*\\s+");
    private Pattern regexpInto = Pattern.compile("\\sINTO\\s+([^ ,]+)(?:\\s*,\\s*([^ ,]+))*\\s+");

    public boolean parseLine(String nextLine, HashMap<String, String> tokens) throws IOException {

        if (!inQuotes) {
            fieldNumber = 1;
        }
        Integer nextLineLength = nextLine.length();
        StringBuilder sb;

        if (pending != null) {
            sb = new StringBuilder(nextLineLength + pending.length());
            sb.append(pending);
            sb.append("\n");
            pending = null;
        } else {
            sb = new StringBuilder(nextLineLength);
        }

        for (int i = 0; i < nextLineLength; i++) {

            char c = nextLine.charAt(i);
            if (c == quoteChar1 || c == quoteChar2) {

                if (!inQuotes) {
                    openQuoteChar = c;
                    inQuotes = true;
                } else if (c == openQuoteChar) {
                    inQuotes = !inQuotes;
                } else {
                    sb.append(c);
                }

            } else if ((c == separator || i == nextLineLength-1) && !inQuotes) {

                addField(tokens, sb);
                fieldNumber++;

            } else if (c == '=' && !inQuotes) {

                fieldName = sb.toString();
                sb.setLength(0);

            } else {
                sb.append(c);
            }
        }

        if (inQuotes) {
            pending = sb.toString();
        } else {
            pending = null;
            addField(tokens, sb);
        }

        return !inQuotes;
    }

    public void addField(HashMap<String, String> tokens, StringBuilder sb) {

        String value = sb.toString() ;

        if (fieldNumber == 1) {

            //06:36.085000-156969
            int index = value.indexOf('-');
            String timeValue     = value.substring(0, index);
            String durationValue = value.substring(index + 1);

            tokens.put("Time",     timeValue);
            tokens.put("Duration", durationValue);
            tokens.put("DateTime", timeValue);

        } else if (fieldNumber == 2) {

            tokens.put("Name", value);

        } else if (fieldNumber == 3) {
            tokens.put("Level", value);

        } else {
            fieldName = fieldName.replace(":", "_");

            tokens.put(fieldName, value);

            if (fieldName.equals("Context")) {

                String[] rows = value.split("\n");
                if (rows.length > 0) {
                    tokens.put("ContextLastRow", rows[rows.length - 1]);
                }

            } else if (fieldName.equals("Sql") || fieldName.equals("Sdbl")) {

                String tableList = getTableList(value);
                tokens.put("TablesList", tableList);

            }
        }

        sb.setLength(0);
    }

    public String getTableList(String sqlText) {

        StringBuilder tableList = new StringBuilder();
        // FROM
        Matcher matcher = regexpFrom.matcher(sqlText);
        while(matcher.find())
        {
            String tableName = matcher.group();
            tableName = tableName.replace("FROM ", "").trim();
            tableName = tableName.replace("FROM\n", "").trim();

            if (!tableName.startsWith("(") && !tableName.startsWith("#")) {
                if (tableList.length() > 0) {
                    tableList.append(" | ");
                }
                tableList.append(tableName.replace("dbo.", ""));
            }
        }

        // JOIN
        matcher = regexpJoin.matcher(sqlText);
        while(matcher.find())
        {
            String tableName = matcher.group();
            tableName = tableName.replace("JOIN ", "").trim();
            tableName = tableName.replace("JOIN\n", "").trim();

            if (!tableName.startsWith("(") && !tableName.startsWith("#")) {
                if (tableList.length() > 0) {
                    tableList.append(" | ");
                }
                tableList.append(tableName.replace("dbo.", ""));
            }
        }

        // JOIN
        matcher = regexpInto.matcher(sqlText);
        while(matcher.find())
        {
            String tableName = matcher.group();
            tableName = tableName.replace("INTO ", "").trim();
            tableName = tableName.replace("INTO\n", "").trim();

            if (!tableName.startsWith("(") && !tableName.startsWith("#")) {
                if (tableList.length() > 0) {
                    tableList.append(" | ");
                }
                tableList.append(tableName.replace("dbo.", ""));
            }
        }

        return tableList.toString();
    }

    public void openFile(String fileName) throws FileNotFoundException, UnsupportedEncodingException {

        this.fileName = fileName;
        InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
        bufferedReader = new BufferedReader(inputStreamReader);

    }

    public void closeFile() throws IOException {

        bufferedReader.close();

    }

    public HashMap<String, String> next() {

        HashMap<String, String> tokens = new HashMap<>();

        String line;

        try {
            while ((line = bufferedReader.readLine()) != null) {

                boolean isEndOfLine = parseLine(line, tokens);
                if (isEndOfLine) {
                    tokens.put("FileName", fileName);
                    return tokens;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean hasNext() {

        try {
            return bufferedReader.ready();
        } catch (IOException e) {
            return false;
        }

    }

    public void remove(){

    }

}
