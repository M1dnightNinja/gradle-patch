package org.wallentines.gradle.patch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

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

    public static IntRange load(JsonElement ele) {

        if(ele.isJsonPrimitive() && ele.getAsJsonPrimitive().isNumber()) {
            return new IntRange(ele.getAsInt());
        }
        if(ele.isJsonArray()) {

            JsonArray arr = ele.getAsJsonArray();
            if(arr.size() != 2) {
                throw new IllegalArgumentException("Int ranges must contain exactly two elements!");
            }
            int min = arr.get(0).getAsInt();
            int max = arr.get(1).getAsInt();

            if(max <= min) {
                throw new IllegalArgumentException("The second value in an int range must be greater than the first value!");
            }

            return new IntRange(min, max);
        }
        if(ele.isJsonObject()) {

            JsonObject obj = ele.getAsJsonObject();

            if(!obj.has("min") && !obj.has("max")) {
                throw new IllegalArgumentException("Int range objects must contain a min or a max!");
            }

            int min = 0;
            int max = Integer.MAX_VALUE;

            if(obj.has("min")) {
                min = obj.get("min").getAsInt();
            }
            if(obj.has("max")) {
                max = obj.get("max").getAsInt();
            }

            if(max <= min) {
                throw new IllegalArgumentException("The 'max' field in an int range must be greater than the 'min' field!");
            }

            return new IntRange(min, max);
        }

        throw new IllegalArgumentException("Don't know how to turn " + ele + "into an int range!");
    }

    @Override
    public int compareTo(@NotNull IntRange o) {
        return o.min - min;
    }
}
