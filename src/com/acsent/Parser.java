package com.acsent;

import java.io.*;
import java.util.HashMap;

@SuppressWarnings("WeakerAccess")
public class Parser {

    public char separator = ',';
    public char quoteChar1 = '\'';
    public char quoteChar2 = '"';

    private boolean inQuotes = false;
    private Integer fieldNumber = 1;
    private char openQuoteChar = '"';
    private String fieldName = "";

    private String pending = null;

    private BufferedReader bufferedReader;

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
                    if (i == nextLineLength-1) {
                        //tokens.put(fieldName, sb.toString());
                        addField(tokens, sb);
                    }
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

        if (fieldNumber == 1) {

            //06:36.085000-156969
            String value = sb.toString();
            int index = value.indexOf('-');
            String timeValue     = value.substring(0, index);
            String durationValue = value.substring(index + 1);

            tokens.put("time",     timeValue);
            tokens.put("duration", durationValue);

        } else if (fieldNumber == 2) {

            tokens.put("name", sb.toString());

        } else if (fieldNumber == 3) {
            tokens.put("xxx", sb.toString());

        } else {
            tokens.put(fieldName, sb.toString());
        }

        sb.setLength(0);
    }

    public void openFile(String fileName) throws FileNotFoundException, UnsupportedEncodingException {

        InputStreamReader inputStreamReader = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
        bufferedReader = new BufferedReader(inputStreamReader);

    }

    public void closeFile() throws IOException {

        bufferedReader.close();

    }

    public HashMap<String, String> parseNext() throws IOException {

        HashMap<String, String> tokens = new HashMap<>();

        String line = bufferedReader.readLine();
        while (line != null) {

            boolean isEndOfLine = parseLine(line, tokens);
            if (isEndOfLine) {
                return tokens;
            } else {
                line = bufferedReader.readLine();
            }
        }

        return null;
    }
}
