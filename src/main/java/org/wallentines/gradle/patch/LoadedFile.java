package org.wallentines.gradle.patch;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LoadedFile {

    private final List<String> lines;
    private final Map<Integer, List<String>> toInsert = new TreeMap<>();

    public LoadedFile(Collection<String> lines) {
        this.lines = new ArrayList<>(lines);
    }

    public int validateLine(int line) {
        return Math.max(1, Math.min(line, lines.size()));
    }

    public int getLength() {
        return lines.size();
    }

    public String getLine(int line) {
        return lines.get(line - 1);
    }

    public void setLine(int line, String newLine) {
        lines.set(line - 1, newLine);
    }

    public void insertAt(int line, String newLine) {
        toInsert.compute(line, (k,v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(newLine);
            return v;
        });
    }

    public Collection<String> getLines() {
        return lines;
    }

    public void write(OutputStream os) throws IOException {

        Map<Integer, List<String>> inserting = new TreeMap<>(toInsert);

        for(int i = 1 ; i <= lines.size() ; i++) {
            if(inserting.containsKey(i)) {
                for(String s : inserting.get(i)) {
                    writeLine(s, os);
                }
                inserting.remove(i);
            }
            writeLine(lines.get(i - 1), os);
        }

        // Write remaining lines
        for(List<String> ss : inserting.values()) {
            for(String s : ss) {
                writeLine(s, os);
            }
        }

    }

    private void writeLine(String line, OutputStream os) throws IOException {
        os.write(line.getBytes(StandardCharsets.UTF_8));
        os.write('\n');
    }

    public static LoadedFile read(BufferedReader stream) throws IOException {

        String line;
        List<String> out = new ArrayList<>();
        while((line = stream.readLine()) != null) {
            out.add(line);
        }

        return new LoadedFile(out);
    }
}
