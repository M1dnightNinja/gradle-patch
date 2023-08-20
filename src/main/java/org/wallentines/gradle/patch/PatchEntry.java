package org.wallentines.gradle.patch;

import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.serializer.*;

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
            for(int line : supp.getLines(file)) {
                action.patch(file, line);
            }
        }
    }

    public enum Type {

        INSERT("insert", sec -> {
            String value = sec.getString("value");
            return (file, line) -> file.insertAt(line + 1, value);
        }),
        INSERT_BEFORE("insert_before", sec -> {
            String value = sec.getString("value");
            return (file, line) -> file.insertAt(line, value);
        }),
        SET("set", sec -> {
            String value = sec.getString("value");
            return (file, line) -> file.setLine(line, value);
        }),
        REPLACE("replace", sec -> {
            String find = sec.getString("find");
            String replace = sec.getString("replace");
            return (file, line) -> file.setLine(line, file.getLine(line).replace(find, replace));
        }),
        REPLACE_REGEX("replace_regex", sec -> {

            Pattern pattern = Pattern.compile(sec.getString("find"));
            String replacement = sec.getString("replace");

            return (file, line) -> {
                Matcher matcher = pattern.matcher(file.getLine(line));
                if(matcher.find()) {
                    file.setLine(line, matcher.replaceAll(replacement));
                }
            };

        });

        final String id;
        final Function<ConfigSection, Action> actionDeserializer;

        Type(String id, Function<ConfigSection, Action> actionDeserializer) {
            this.id = id;
            this.actionDeserializer = actionDeserializer;
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
        void patch(LoadedFile file, int line);
    }


    public static final Serializer<PatchEntry> SERIALIZER = new Serializer<>() {
        @Override
        public <O> SerializeResult<O> serialize(SerializeContext<O> context, PatchEntry value) {
            return null;
        }

        @Override
        public <O> SerializeResult<PatchEntry> deserialize(SerializeContext<O> context, O value) {

            if(!context.isMap(value)) {
                return SerializeResult.failure("Value must be a section!");
            }

            String typeId = context.asString(context.get("type", value));
            Type t = Type.byId(typeId);
            if(t == null) {
                return SerializeResult.failure("Unknown type " + typeId + "!");
            }

            SerializeResult<Collection<LineSupplier>> supp = LineSupplier.SERIALIZER.listOf().deserialize(context, context.get("lines", value));

            if(!supp.isComplete()) {
                return SerializeResult.failure(supp.getError());
            }

            return SerializeResult.success(new PatchEntry(t.actionDeserializer.apply(context.convert(ConfigContext.INSTANCE, value).asSection()), supp.getOrThrow()));
        }
    };

}
