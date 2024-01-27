package org.wallentines.gradle.patch;

import org.wallentines.mdcfg.serializer.*;

import java.util.Collection;
import java.util.List;
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

        INSERT("insert", Insert.class, ObjectSerializer.create(
                Serializer.STRING.entry("value", Insert::value),
                Insert::new
        )),
        INSERT_BEFORE("insert_before", InsertBefore.class, ObjectSerializer.create(
                Serializer.STRING.entry("value", InsertBefore::value),
                InsertBefore::new
        )),
        SET("set", Set.class, ObjectSerializer.create(
                Serializer.STRING.entry("value", Set::value),
                Set::new
        )),
        REPLACE("replace", Replace.class, ObjectSerializer.create(
                Serializer.STRING.entry("find", Replace::find),
                Serializer.STRING.entry("replace", Replace::replace),
                Replace::new
        )),
        REPLACE_REGEX("replace_regex", RegRep.class, ObjectSerializer.create(
                PATTERN_SERIALIZER.entry("find", RegRep::find),
                Serializer.STRING.entry("replace", RegRep::replace),
                RegRep::new
        ));

        final String id;
        final Serializer<Action> actionSerializer;

        @SuppressWarnings("unchecked")
        <T extends Action> Type(String id, Class<T> clazz, Serializer<T> actionSerializer) {
            this.id = id;
            this.actionSerializer = actionSerializer.map(act -> {
                if(act.getClass() != clazz) return null;
                return (T) act;
            }, act -> act);
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

            for(IntRange ir : file.find(find)) {
                if(ir.isWithin(lines)) {
                    String str = file.getLines(ir).toString();
                    Matcher matcher = find.matcher(str);
                    file.setLines(ir, matcher.replaceAll(replace));
                }
            }
        }
    }

    private static final Serializer<Pattern> PATTERN_SERIALIZER = InlineSerializer.of(Pattern::pattern, str -> Pattern.compile(str, Pattern.MULTILINE));

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

            return t.actionSerializer.deserialize(context, value).flatMap(act -> new PatchEntry(act, supp.getOrThrow()));
        }
    };

}
