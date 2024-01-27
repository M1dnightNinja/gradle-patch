import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.gradle.patch.LoadedFile;

import java.io.*;

public class TestLoadedFile {

    @Test
    public void testSet() {

        String file = "Hello\nWorld\nLast Line";

        LoadedFile lf;
        try {
            lf = LoadedFile.read(new BufferedReader(new StringReader(file)));
        } catch (IOException ex) {
            Assertions.fail("An exception occurred while reading a file!", ex);
            return;
        }

        lf.setLine(1, "Hola");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            lf.write(os);
        } catch (IOException ex) {
            Assertions.fail("An exception occurred writing a file!", ex);
            return;
        }
        String out = os.toString();

        Assertions.assertEquals("Hola\nWorld\nLast Line", out);


        lf.setLine(2, "Mundo");
        os = new ByteArrayOutputStream();
        try {
            lf.write(os);
        } catch (IOException ex) {
            Assertions.fail("An exception occurred writing a file!", ex);
            return;
        }
        out = os.toString();


        Assertions.assertEquals("Hola\nMundo\nLast Line", out);

        lf.setLine(3, "Última Línea");
        os = new ByteArrayOutputStream();
        try {
            lf.write(os);
        } catch (IOException ex) {
            Assertions.fail("An exception occurred writing a file!", ex);
            return;
        }
        out = os.toString();

        Assertions.assertEquals("Hola\nMundo\nÚltima Línea", out);

    }

}
