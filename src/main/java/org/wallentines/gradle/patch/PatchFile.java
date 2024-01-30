package org.wallentines.gradle.patch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PatchFile {

    private final List<PatchEntry> entries;

    public PatchFile(Collection<PatchEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    public List<PatchEntry> getEntries() {
        return entries;
    }

    public void patch(LoadedFile file) {
        for(PatchEntry ent : entries) {
            ent.patch(file);
        }
    }

    public static PatchFile load(JsonArray array) {

        List<PatchEntry> out = new ArrayList<>();
        for(JsonElement ele : array) {
            if(ele.isJsonObject()) {
                out.add(PatchEntry.load(ele.getAsJsonObject()));
            }
        }

        return new PatchFile(out);
    }

}
