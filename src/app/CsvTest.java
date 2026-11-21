package app;

import service.CsvUtil;

public class CsvTest {
    public static void main(String[] args) {
        String line = CsvUtil.join("D009", "Dela Cruz, Juan", "He said \"hi\"", 5);
        System.out.println(line);
        System.out.println(CsvUtil.parseLine(line));
    }
}