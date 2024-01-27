package org.wallentines.gradle.patch;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoadedFile {

    private String data;
    private final List<Integer> lineFeeds = new ArrayList<>();
    private final TreeSet<Integer> toRemove = new TreeSet<>();
    private final SortedMap<Integer, List<String>> toInsert = new TreeMap<>();
    private final SortedMap<Integer, String> overrides = new TreeMap<>();

    public LoadedFile(String data) {
        this.data = data;

        int currentIndex = 0;
        while((currentIndex = data.indexOf('\n', currentIndex)) != -1) {
            lineFeeds.add(currentIndex++);
        }

        lineFeeds.add(data.length());
    }

    public int validateLine(int line) {
        return Math.max(1, Math.min(line, getLength()));
    }

    public int getLength() {
        return lineFeeds.size();
    }

    public CharSequence getLine(int line) {

        return data.subSequence(getLineStart(line), getLineEnd(line));
    }

    public CharSequence getLines(IntRange range) {

        return data.subSequence(getLineStart(range.min()), getLineEnd(range.max()));
    }

    public CharSequence getAllLines() {
        return data;
    }

    public void deleteLine(int line) {
        toRemove.add(line);
    }

    public void setLine(int line, String newLine) {

        setLines(new IntRange(line), newLine);
    }

    public void setLines(IntRange lines, String newLine) {

        StringBuilder out = new StringBuilder();

        int offset = 0;
        int insertIndex = lines.min() - 1;
        int oldLength = getLines(lines).length();

        if(lines.min() > 1) {
            out.append(getLines(new IntRange(1, lines.min()-1)));
            out.append("\n");
            offset = lineFeeds.get(insertIndex - 1) + 1;
        }

        out.append(newLine);
        if(lines.max() < getLength()) {
            out.append('\n');
        }

        // Correct newlines
        if(lines.max() < getLength()) {
            out.append(getLines(new IntRange(lines.max() + 1, getLength())));
        }

        List<Integer> newLineFeeds = new ArrayList<>();
        int index = -1;
        while((index = newLine.indexOf('\n', index)) != -1) {
            newLineFeeds.add(offset + index);
            index++;
        }
        newLineFeeds.add(offset + newLine.length());

        if(newLineFeeds.size() < lines.length()) {
            for(int i = 0 ; i < lines.length() - newLineFeeds.size() ; i++) {
                lineFeeds.remove(insertIndex + i);
            }
        } else if(newLineFeeds.size() > lines.length()) {
            for(int i = 0 ; i < newLineFeeds.size() - lines.length() ; i++) {
                lineFeeds.add(insertIndex + i, 0);
            }
        }

        for(int i = 0 ; i < newLineFeeds.size() ; i++) {
            lineFeeds.set(insertIndex + i, newLineFeeds.get(i));
        }

        if(newLine.length() != oldLength) {
            int difference = newLine.length() - oldLength;
            for(int i = insertIndex + newLineFeeds.size() ; i < getLength() ; i++) {
                lineFeeds.set(i, lineFeeds.get(i) + difference);
            }
        }

        this.data = out.toString();
    }

    public Collection<IntRange> find(String substr) {
        int index = 0;
        Set<IntRange> out = new HashSet<>();
        while((index = data.indexOf(substr, index)) != -1) {
            out.add(new IntRange(getLineFromIndex(index), getLineFromIndex(index + substr.length())));
            index++;
        }
        return out;
    }

    public Collection<IntRange> find(Pattern pattern) {

        Set<IntRange> out = new HashSet<>();

        Matcher matcher = pattern.matcher(data);
        while(matcher.find()) {
            out.add(new IntRange(getLineFromIndex(matcher.start()), getLineFromIndex(matcher.end())));
        }

        return out;
    }

    private int getLineStart(int line) {
        if(line <= 1) {
            return 0;
        }

        return lineFeeds.get(line - 2) + 1;
    }

    private int getLineEnd(int line) {
        if(line < 1) {
            line = 1;
        }
        return lineFeeds.get(line - 1);
    }

    private int getLineFromIndex(int index) {

        int line = 1;
        for(int i : lineFeeds) {
            if(i >= index) {
                break;
            }
            line++;
        }
        return line;
    }

    public void insertAt(int line, String newLine) {
        toInsert.compute(line, (k,v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(newLine);
            return v;
        });
    }

    public void write(OutputStream os) throws IOException {

        StringBuilder toWrite = new StringBuilder();
        for(int i = 0 ; i <= getLength() ; i++) {

            if(i > 0 && !toRemove.contains(i)) {
                if(!toWrite.isEmpty()) {
                    toWrite.append("\n");
                }
                toWrite.append(getLine(i));
            }
            if(toInsert.containsKey(i)) {
                for(String s : toInsert.get(i)) {
                    if(!toWrite.isEmpty()) {
                        toWrite.append("\n");
                    }
                    toWrite.append(s);
                }
            }
        }
        os.write(toWrite.toString().getBytes());
    }

    public static LoadedFile read(BufferedReader stream) throws IOException {

        String line;
        StringBuilder out = new StringBuilder();
        while((line = stream.readLine()) != null) {

            if(!out.isEmpty()){
                out.append('\n');
            }

            out.append(line);

        }

        return new LoadedFile(out.toString());
    }
}
