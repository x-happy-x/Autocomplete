package renue.search;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutoComplete {
    private List<Line> lines;
    private final String filename;

    public AutoComplete(String filename, int column) throws IOException, ArrayIndexOutOfBoundsException {
        this.filename = filename;
        this.lines = new ArrayList<>();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder();
        long startLine = 0, endLine = 0;
        boolean escape = false, isNumbers = true;
        int c, quote = 0, currentColumn = 0;
        while ((c = reader.read()) != -1) {
            endLine++;
            switch (c) {
                case '"':
                    if (quote == 0) quote++;
                    else if (!escape) quote--;
                    if (currentColumn == column) builder.append((char) c);
                    break;
                case '\\':
                    escape = true;
                    if (currentColumn == column) builder.append((char) c);
                    continue;
                case ',':
                    if (quote == 0) currentColumn++;
                    break;
                case '\n':
                    if (column > currentColumn) throw new ArrayIndexOutOfBoundsException(String.format("%d столбца нет в файле (Всего %d столбцов)", column +1,currentColumn+1));
                    String line = builder.toString().replaceAll("^ +| +$", "");
                    if (line.startsWith("\"")) {
                        if (isNumbers) isNumbers = false;
                        line = line.substring(1, line.length() - 1);
                    }
                    lines.add(new Line(line, startLine, endLine - startLine - 1));
                    builder.setLength(0);
                    currentColumn = 0;
                    startLine = endLine;
                    break;
                default:
                    if (currentColumn == column) {
                        builder.append((char) c);
                    }
                    break;
            }
            if (escape)
                escape = false;
        }
        reader.close();
        if (endLine > startLine) {
            if (column > currentColumn) throw new ArrayIndexOutOfBoundsException();
            lines.add(new Line(builder.toString(), startLine, endLine - startLine));
        }
        Stream<Line> stream = lines.stream();
        if (isNumbers) {
            stream = stream.sorted(Comparator.comparingDouble(o -> Double.parseDouble(o.column)));
        } else {
            stream = stream.sorted(Comparator.comparing(o -> o.column));
        }
        lines = stream.collect(Collectors.toList());
    }

    public String[][] search(String text) throws IOException {
        String fText = text.toLowerCase();
        List<Line> findLine = lines.stream()
                .filter(line -> line.column.startsWith(fText))
                .collect(Collectors.toList());
        List<Line> sortedLine = findLine.stream()
                .sorted(Comparator.comparingLong(line -> line.start))
                .collect(Collectors.toList());
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder();
        String[][] strings = new String[sortedLine.size()][2];
        int c;
        for (int i = 0; i < sortedLine.size(); i++) {
            reader.skip(i == 0 ? sortedLine.get(i).start
                    : sortedLine.get(i).start - (sortedLine.get(i - 1).start + sortedLine.get(i - 1).length));
            for (int j = 0; j < sortedLine.get(i).length; j++) {
                if ((c = reader.read()) != -1) {
                    builder.append((char) c);
                } else {
                    throw new IOException(String.format(
                            "Не удалось прочитать %d символ, возможно файл был изменён",
                            sortedLine.get(i).start + j));
                }
            }
            for (int j = 0; j < findLine.size(); j++) {
                if (findLine.get(j).start == sortedLine.get(i).start) {
                    strings[j][1] = builder.toString();
                    strings[j][0] = findLine.get(j).orig;
                    break;
                }
            }
            builder.setLength(0);
        }
        reader.close();
        return strings;
    }

    private static class Line {
        long start, length;
        String column, orig;

        public Line(String column, long start, long length) {
            this.column = column.toLowerCase();
            this.orig = column;
            this.start = start;
            this.length = length;
        }
    }
}
