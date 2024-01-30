package org.wallentines.gradle.patch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatchEntry {

    private final Action action;
    private final List<LineSupplier> lines;

    public PatchEntry(Action action, Collection<LineSupplier> lines) {
        this.action = action;
        this.lines = List.copyOf(lines);
    }

    public void patch(LoadedFile file) {
        for(LineSupplier supp : lines) {
            for(IntRange range : supp.getLines(file)) {
                action.patch(file, range);
            }
        }
    }

    public enum Type {

        INSERT("insert", ele -> new Insert(ele.get("value").getAsString())),
        INSERT_BEFORE("insert_before", ele -> new InsertBefore(ele.get("value").getAsString())),
        SET("set", ele -> new Set(ele.get("value").getAsString())),
        REPLACE("replace", ele -> new Replace(ele.get("find").getAsString(), ele.get("replace").getAsString())),
        REPLACE_REGEX("replace_regex", ele -> new RegRep(Pattern.compile(ele.get("find").getAsString(), Pattern.MULTILINE), ele.get("replace").getAsString()));

        final String id;
        final Function<JsonObject, Action> actionSerializer;

        Type(String id, Function<JsonObject, Action> actionDeserializer) {
            this.id = id;
            this.actionSerializer = actionDeserializer;
        }

        public String getId() {
            return id;
        }

        public static Type byId(String id) {
            for(Type t : values()) {
                if(t.id.equals(id)) return t;
            }
            return null;
        }
    }

    public interface Action {
        void patch(LoadedFile file, IntRange lines);
    }

    public record Insert(String value) implements Action {
        @Override
        public void patch(LoadedFile file, IntRange lines) {
            file.insertAt(lines.max() + 1, value);
        }
    }

    public record InsertBefore(String value) implements Action {
        @Override
        public void patch(LoadedFile file, IntRange lines) {
            file.insertAt(lines.min(), value);
        }
    }


    public record Set(String value) implements Action {
        @Override
        public void patch(LoadedFile file, IntRange lines) {
            file.setLines(lines, value);
        }
    }


    public record Replace(String find, String replace) implements Action {
        @Override
        public void patch(LoadedFile file, IntRange lines) {
            for(IntRange ir : file.find(find)) {
                if(ir.isWithin(lines)) {
                    file.setLines(ir, file.getLines(ir).toString().replace(find, replace));
                }
            }
        }
    }

    public record RegRep(Pattern find, String replace) implements Action {
        @Override
        public void patch(LoadedFile file, IntRange lines) {

            CharSequence in = file.getLines(lines);
            Matcher matcher = find.matcher(in);
            StringBuilder builder = new StringBuilder();
            while(matcher.find()) {
                matcher.appendReplacement(builder, replace);
            }
            if(!builder.isEmpty()) {
                matcher.appendTail(builder);
                file.setLines(lines, builder.toString());
            }
        }
    }


    public static PatchEntry load(JsonObject obj) {

        String typeId = obj.get("type").getAsString();
        Type t = Type.byId(typeId);
        if(t == null) {
            throw new IllegalArgumentException("Unknown entry type " + typeId + "!");
        }

        List<LineSupplier> supps = new ArrayList<>();
        JsonArray lines = obj.getAsJsonArray("lines");
        for(JsonElement ele : lines) {
            supps.add(LineSupplier.load(ele));
        }

        return new PatchEntry(t.actionSerializer.apply(obj), supps);
    }

}
