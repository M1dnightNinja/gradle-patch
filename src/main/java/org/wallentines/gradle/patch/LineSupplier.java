package org.wallentines.gradle.patch;

import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public interface LineSupplier {

    Collection<Integer> getLines(LoadedFile file);

    static LineSupplier single(int line) {
        return file -> Collections.singleton(file.validateLine(line));
    }

    static LineSupplier multiple(Collection<Integer> lines) {
        return file -> lines.stream().map(file::validateLine).toList();
    }

    static LineSupplier range(IntRange range) {
        return file -> new ArrayList<>(range.getValues());
    }

    static LineSupplier all() {
        return file -> {
            List<Integer> out = new ArrayList<>();
            for(int i = 1; i <= file.getLength() ; i++) {
                out.add(i);
            }
            return out;
        };
    }

    static LineSupplier find(String find, IntRange offset) {

        return file -> {
            List<Integer> out = new ArrayList<>();
            for(int i = 1 ; i <= file.getLength() ; i++) {
                if(file.getLine(i).contains(find)) {
                    for(int off : offset.getValues()) {
                        out.add(i + off);
                    }
                }
            }
            return out;
        };
    }

    static LineSupplier findRegex(Pattern find, IntRange offset) {

        return file -> {
            List<Integer> out = new ArrayList<>();
            for(int i = 1 ; i <= file.getLength() ; i++) {
                if(find.matcher(file.getLine(i)).find()) {
                    for(int off : offset.getValues()) {
                        out.add(i + off);
                    }
                }
            }
            return out;
        };
    }

    Serializer<LineSupplier> SERIALIZER = new Serializer<>() {
        @Override
        public <O> SerializeResult<O> serialize(SerializeContext<O> context, LineSupplier value) {
            return null;
        }

        @Override
        public <O> SerializeResult<LineSupplier> deserialize(SerializeContext<O> context, O value) {

            if(context.isString(value) && context.asString(value).equalsIgnoreCase("all")) {
                return SerializeResult.success(all());
            }
            if(context.isNumber(value)) {
                return SerializeResult.success(single(context.asNumber(value).intValue()));
            }
            if(context.isMap(value)) {

                Map<String, O> values = context.asMap(value);
                if(values.containsKey("value")) {
                    return deserialize(context, values.get("value"));
                }
                if(values.containsKey("values")) {
                    List<Integer> out = new ArrayList<>();
                    for(O o : values.values()) {
                        Number num = context.asNumber(o);
                        if(num == null) {
                            return SerializeResult.failure("All elements in multi line entries must be numbers!");
                        }
                        out.add(context.asNumber(o).intValue());
                    }
                    return SerializeResult.success(multiple(out));
                }
                if(values.containsKey("find")) {
                    if(!context.isString(values.get("find"))) {
                        return SerializeResult.failure("The 'find' field in find line entries must be a String!");
                    }

                    IntRange offset = new IntRange(0);
                    if(values.containsKey("offset")) {
                        offset = IntRange.SERIALIZER.deserialize(context, values.get("offset")).get().orElse(offset);
                    }

                    return SerializeResult.success(find(context.asString(values.get("find")), offset));
                }
                if(values.containsKey("find_regex")) {
                    if(!context.isString(values.get("find_regex"))) {
                        return SerializeResult.failure("The 'find' field in find_regex line entries must be a String!");
                    }
                    Pattern p;
                    try {
                        p = Pattern.compile(context.asString(values.get("find_regex")));
                    } catch (PatternSyntaxException ex) {
                        return SerializeResult.failure("The 'find_regex' field in find_regex line entries must be a valid Regex pattern!");
                    }

                    IntRange offset = new IntRange(0);
                    if(values.containsKey("offset")) {
                        offset = IntRange.SERIALIZER.deserialize(context, values.get("offset")).get().orElse(offset);
                    }

                    return SerializeResult.success(findRegex(p, offset));
                }

            }

            return IntRange.SERIALIZER.deserialize(context, value)
                    .flatMap(LineSupplier::range)
                    .mapError(err -> SerializeResult.failure("Don't know how to deserialize " + value + " as a line supplier!"));
        }
    };

}
