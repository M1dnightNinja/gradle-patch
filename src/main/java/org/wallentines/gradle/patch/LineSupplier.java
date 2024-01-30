package org.wallentines.gradle.patch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.regex.Pattern;

public interface LineSupplier {

    Collection<IntRange> getLines(LoadedFile file);

    static LineSupplier single(int line) {
        return file -> Collections.singleton(new IntRange(file.validateLine(line)));
    }

    static LineSupplier multiple(Collection<Integer> lines) {
        return file -> lines.stream().map(file::validateLine).map(IntRange::new).toList();
    }

    static LineSupplier range(IntRange range) {
        return file -> List.of(range);
    }

    static LineSupplier all() {
        return file -> List.of(new IntRange(1, file.getLength()));
    }

    static LineSupplier find(String find, IntRange offset) {

        return file -> {
            TreeSet<IntRange> out = new TreeSet<>();
            for(IntRange i : file.find(find)) {
                out.add(new IntRange(i.min() + offset.min(), i.max() + offset.max()));
            }
            return out;
        };
    }

    static LineSupplier findRegex(Pattern find, IntRange offset) {

        return file -> {
            TreeSet<IntRange> out = new TreeSet<>();
            for(IntRange i : file.find(find)) {
                out.add(new IntRange(i.min() + offset.min(), i.max() + offset.max()));
            }
            return out;
        };
    }

    static LineSupplier multi(List<LineSupplier> children) {

        return file -> {
            List<IntRange> out = new ArrayList<>();
            for(LineSupplier supp : children) {
                out.addAll(supp.getLines(file));
            }
            return out;
        };
    }


    static LineSupplier load(JsonElement ele) {

        if(ele.isJsonPrimitive() && ele.getAsJsonPrimitive().isString() && ele.getAsString().equalsIgnoreCase("all")) {
            return all();
        }
        if(ele.isJsonPrimitive() && ele.getAsJsonPrimitive().isNumber()) {
            return single(ele.getAsInt());
        }
        if(ele.isJsonObject()) {
            JsonObject obj = ele.getAsJsonObject();
            if(obj.has("value")) {
                return load(obj.get("value"));
            }
            if(obj.has("values")) {
                JsonArray arr = obj.getAsJsonArray("values");
                List<LineSupplier> out = new ArrayList<>();
                for(JsonElement child : arr) {
                    out.add(load(child));
                }
                return multi(out);
            }
            IntRange offset = new IntRange(0);
            if(obj.has("offset")) {
                offset = IntRange.load(obj.get("offset"));
            }

            if(obj.has("find")) {
                return find(obj.get("find").getAsString(), offset);
            }

            if(obj.has("find_regex")) {
                return findRegex(Pattern.compile(obj.get("find_regex").getAsString(), Pattern.MULTILINE), offset);
            }
        }

        throw new IllegalArgumentException("Don't know how to read " + ele + " as a line serializer!");
    }

}
