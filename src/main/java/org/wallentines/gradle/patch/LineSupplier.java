package org.wallentines.gradle.patch;

import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
                        p = Pattern.compile(context.asString(values.get("find_regex")), Pattern.MULTILINE);
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
