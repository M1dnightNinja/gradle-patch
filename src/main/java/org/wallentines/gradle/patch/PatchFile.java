package org.wallentines.gradle.patch;

import org.wallentines.mdcfg.serializer.Serializer;

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
    public static final Serializer<PatchFile> SERIALIZER = PatchEntry.SERIALIZER.listOf().map(PatchFile::getEntries, PatchFile::new);

}
