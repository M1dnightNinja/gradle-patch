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

    static LineSupplier range(int begin, int end) {
        return file -> {
            List<Integer> out = new ArrayList<>();
            for(int i = begin; i <= end ; i++) {
                out.add(file.validateLine(i));
            }
            return out;
        };
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

    static LineSupplier find(String find) {

        return file -> {
            List<Integer> out = new ArrayList<>();
            for(int i = 1 ; i <= file.getLength() ; i++) {
                if(file.getLine(i).contains(find)) {
                    out.add(i);
                }
            }
            return out;
        };
    }

    static LineSupplier findRegex(Pattern find) {

        return file -> {
            List<Integer> out = new ArrayList<>();
            for(int i = 1 ; i <= file.getLength() ; i++) {
                if(find.matcher(file.getLine(i)).find()) {
                    out.add(i);
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
            if(context.isList(value)) {
                List<O> values = List.copyOf(context.asList(value));
                if(values.size() != 2) {
                    return SerializeResult.failure("Range line entries must contain exactly two elements!");
                }
                Number min = context.asNumber(values.get(0));
                Number max = context.asNumber(values.get(1));

                if(min == null || max == null) {
                    return SerializeResult.failure("All elements in range line entries must be numbers!");
                }

                int iMin = min.intValue();
                int iMax = max.intValue();

                if(iMax <= iMin) {
                    return SerializeResult.failure("The 'max' field in ranged line entries must be greater than the min entry!");
                }

                return SerializeResult.success(range(iMin, iMax));
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

                if(values.containsKey("min") || values.containsKey("max")) {

                    int min = 0;
                    int max = Integer.MAX_VALUE;

                    if(values.containsKey("min")) {
                        Number num = context.asNumber(values.get("min"));
                        if(num == null) {
                            return SerializeResult.failure("The 'min' and 'max' fields in ranged line entries must be numbers!");
                        }
                        min = num.intValue();
                    }
                    if(values.containsKey("max")) {
                        Number num = context.asNumber(values.get("max"));
                        if(num == null) {
                            return SerializeResult.failure("The 'min' and 'max' fields in ranged line entries must be numbers!");
                        }
                        max = num.intValue();
                    }

                    if(max <= min) {
                        return SerializeResult.failure("The 'max' field in ranged line entries must be greater than the 'min' field!");
                    }

                    return SerializeResult.success(range(min, max));
                }

                if(values.containsKey("find")) {
                    if(!context.isString(values.get("find"))) {
                        return SerializeResult.failure("The 'find' field in find line entries must be a String!");
                    }
                    return SerializeResult.success(find(context.asString(values.get("find"))));
                }

                if(values.containsKey("find_regex")) {
                    if(!context.isString(values.get("find"))) {
                        return SerializeResult.failure("The 'find' field in find line entries must be a String!");
                    }
                    Pattern p;
                    try {
                        p = Pattern.compile(context.asString(values.get("find")));
                    } catch (PatternSyntaxException ex) {
                        return SerializeResult.failure("The 'find' field in find_regex line entries must be a valid Regex pattern!");
                    }

                    return SerializeResult.success(findRegex(p));
                }

            }

            return SerializeResult.failure("Don't know how to deserialize " + value + " as a line supplier!");
        }
    };

}
