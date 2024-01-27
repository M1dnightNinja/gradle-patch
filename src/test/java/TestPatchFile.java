import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.gradle.patch.LineSupplier;
import org.wallentines.gradle.patch.LoadedFile;
import org.wallentines.gradle.patch.PatchEntry;
import org.wallentines.gradle.patch.PatchFile;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TestPatchFile {

    @Test
    public void testReplace() {

        String file = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\nUt enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\nDuis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.\nExcepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

        LoadedFile lf;
        try {
            lf = LoadedFile.read(new BufferedReader(new StringReader(file)));
        } catch (IOException ex) {
            Assertions.fail("An exception occurred while reading a file!", ex);
            return;
        }

        PatchEntry ent = new PatchEntry(new PatchEntry.Replace("dolor", "replaced"), List.of(LineSupplier.all()));
        ent.patch(lf);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            lf.write(os);
        } catch (IOException ex) {
            Assertions.fail("An exception occurred writing a file!", ex);
            return;
        }
        String out = os.toString();

        Assertions.assertEquals("Lorem ipsum replaced sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et replacede magna aliqua.\nUt enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.\nDuis aute irure replaced in reprehenderit in voluptate velit esse cillum replacede eu fugiat nulla pariatur.\nExcepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.", out);

    }

    @Test
    public void testReplaceFile() {

        File toPatch = new File("SpigotPlayer.java");
        File patch = new File("SpigotPlayer.json");

        PatchFile pf;
        try {
            pf = PatchFile.SERIALIZER.deserialize(ConfigContext.INSTANCE, JSONCodec.fileCodec().loadFromFile(ConfigContext.INSTANCE, patch, StandardCharsets.UTF_8)).getOrThrow();
        } catch (Exception ex) {
            Assertions.fail("An exception occurred while reading a file!", ex);
            return;
        }


        LoadedFile lf;
        try {
            lf = LoadedFile.read(new BufferedReader(new FileReader(toPatch)));
        } catch (Exception ex) {
            Assertions.fail("An exception occurred while reading a file!", ex);
            return;
        }

        pf.patch(lf);


        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            lf.write(os);
        } catch (IOException ex) {
            Assertions.fail("An exception occurred writing a file!", ex);
            return;
        }
        String out = os.toString();

        Assertions.assertFalse(out.contains("->"));
    }

}
