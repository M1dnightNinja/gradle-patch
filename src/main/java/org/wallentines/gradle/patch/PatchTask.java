package org.wallentines.gradle.patch;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.TaskAction;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;

import java.io.*;

public class PatchTask extends DefaultTask {

    public SourceDirectorySet patches;
    public SourceDirectorySet sources;
    public File generatedSourceDir;


    @TaskAction
    public void patch() {

        if(!generatedSourceDir.exists() && !generatedSourceDir.mkdirs()) {
            throw new IllegalStateException("Unable to create generated source directory at " + generatedSourceDir.getAbsolutePath());
        }

        for(File patch : patches) {
            for(File dir : patches.getSourceDirectories()) {

                if(!patch.getAbsolutePath().startsWith(dir.getAbsolutePath())) continue;

                String fileName = patch.getAbsolutePath().substring(dir.getAbsolutePath().length());
                fileName = fileName.replace(".json", ".java").replace("\\", "/");

                File originalFile = findSourceFile(fileName);
                if(originalFile == null) {
                    throw new IllegalStateException("Unable to find file " + fileName + " for patching!");
                }

                File outFile = new File(generatedSourceDir, fileName);
                if(!outFile.getParentFile().exists() && !outFile.getParentFile().mkdirs()) {
                    throw new IllegalStateException("Unable to create generated source directory " + outFile.getParentFile());
                }

                PatchFile pf;
                try(FileInputStream fis = new FileInputStream(patch)) {

                    pf = PatchFile.SERIALIZER.deserialize(ConfigContext.INSTANCE, JSONCodec.loadConfig(fis)).getOrThrow();

                } catch (IOException ex) {
                    throw new IllegalStateException("Unable to read patch file " + patch + "! " + ex.getMessage());
                }

                LoadedFile loadedFile;
                try(BufferedReader fis = new BufferedReader(new InputStreamReader(new FileInputStream(originalFile)))) {

                    loadedFile = LoadedFile.read(new BufferedReader(fis));

                } catch (IOException ex) {
                    throw new IllegalStateException("Unable to read source file " + originalFile + "! " + ex.getMessage());
                }

                pf.patch(loadedFile);

                try(FileOutputStream fos = new FileOutputStream(outFile)) {

                    loadedFile.write(fos);

                } catch (IOException ex) {
                    throw new IllegalStateException("Unable to generate source file for patch " + patch + "! " + ex.getMessage());
                }
            }
        }
    }

    private File findSourceFile(String fileName) {

        for(File dir : sources.getSourceDirectories()) {
            File f = new File(dir, fileName);
            if(sources.contains(f)) return f;
        }

        return null;
    }


}
