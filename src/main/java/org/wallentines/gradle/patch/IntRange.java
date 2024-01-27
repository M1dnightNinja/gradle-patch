package org.wallentines.gradle.patch;

import org.jetbrains.annotations.NotNull;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;

import java.util.*;

public record IntRange(int min, int max) implements Comparable<IntRange> {


    public IntRange(int exact) {
        this(exact, exact);
    }


    public Collection<Integer> getValues() {
        List<Integer> out = new ArrayList<>();
        for(int i = min ; i <= max ; i++) {
            out.add(i);
        }
        return out;
    }

    public int length() {
        return max - min + 1;
    }

    public boolean isWithin(IntRange range) {
        return min >= range.min && max <= range.max;
    }

    public static final Serializer<IntRange> SERIALIZER = new Serializer<>() {
        @Override
        public <O> SerializeResult<O> serialize(SerializeContext<O> context, IntRange value) {
            return SerializeResult.success(context.toList(List.of(context.toNumber(value.min),context.toNumber(value.max))));
        }

        @Override
        public <O> SerializeResult<IntRange> deserialize(SerializeContext<O> context, O value) {

            if(context.isNumber(value)) {
                return SerializeResult.success(new IntRange(context.asNumber(value).intValue()));
            }
            if(context.isList(value)) {

                List<O> values = List.copyOf(context.asList(value));
                if(values.size() != 2) {
                    return SerializeResult.failure("Int ranges must contain exactly two elements!");
                }
                Number min = context.asNumber(values.get(0));
                Number max = context.asNumber(values.get(1));

                if(min == null || max == null) {
                    return SerializeResult.failure("All elements in int ranges must be numbers!");
                }

                int iMin = min.intValue();
                int iMax = max.intValue();

                if(iMax <= iMin) {
                    return SerializeResult.failure("The second value in an int range must be greater than the first value!");
                }

                return SerializeResult.success(new IntRange(iMin, iMax));
            }
            if(context.isMap(value)) {

                Map<String, O> values = context.asMap(value);
                if(!values.containsKey("min") && !values.containsKey("max")) {
                    return SerializeResult.failure("Int range objects must contain a min or a max!");
                }

                int min = 0;
                int max = Integer.MAX_VALUE;

                if(values.containsKey("min")) {
                    Number num = context.asNumber(values.get("min"));
                    if(num == null) {
                        return SerializeResult.failure("The 'min' and 'max' fields in int ranges must be numbers!");
                    }
                    min = num.intValue();
                }
                if(values.containsKey("max")) {
                    Number num = context.asNumber(values.get("max"));
                    if(num == null) {
                        return SerializeResult.failure("The 'min' and 'max' fields in int ranges must be numbers!");
                    }
                    max = num.intValue();
                }

                if(max <= min) {
                    return SerializeResult.failure("The 'max' field in an int range must be greater than the 'min' field!");
                }

                return SerializeResult.success(new IntRange(min, max));
            }

            return SerializeResult.failure("Don't know how to turn " + value + "into an int range!");
        }
    };

    @Override
    public int compareTo(@NotNull IntRange o) {
        return o.min - min;
    }
}
