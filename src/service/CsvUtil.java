package service;

import java.util.ArrayList;
import java.util.List;

public class CsvUtil {

    private CsvUtil() { }

    /** Wraps a field in quotes if it contains a comma or a quote. */
    public static String escape(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /** Turns any values into one CSV line. */
    public static String join(Object... fields) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(escape(String.valueOf(fields[i])));
        }
        return line.toString();
    }

    /** Splits one CSV line into fields, respecting quotes. */
    public static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    boolean doubledQuote = i + 1 < line.length() && line.charAt(i + 1) == '"';
                    if (doubledQuote) {
                        current.append('"');   // "" means one real quote
                        i++;
                    } else {
                        inQuotes = false;      // closing quote
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;               // opening quote
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);          // start the next field
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());        // the last field
        return fields;
    }
}