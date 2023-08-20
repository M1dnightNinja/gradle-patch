package org.wallentines.gradle.patch;

import org.gradle.api.Project;
import org.gradle.api.file.*;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.util.internal.GUtil;

import javax.inject.Inject;
import java.io.File;

public class PatchExtension {

    public final DirectoryProperty patchDirectory;

    private final Project project;

    @Inject
    public PatchExtension(Project project) {

        this.project = project;

        this.patchDirectory = project.getObjects().directoryProperty();
        this.patchDirectory.convention(patchDirectory.dir("patch"));

    }

    /**
     * Creates a patch set with the given parameters
     * @param name The name of the patch set. Will determine the name of the directory within the patch directory
     * @param sourceSet The source set which contains the sources to patch
     * @param sources The sources to patch
     * @param compileTask The compile task to modify to include patched sources
     */
    public void patchSet(String name, SourceSet sourceSet, SourceDirectorySet sources, JavaCompile compileTask) {

        ObjectFactory objectFactory = project.getObjects();
        TaskContainer tasks = project.getTasks();

        String dirSetName = GUtil.toWords(name) + " patches";
        SourceDirectorySet javaPatches = objectFactory.sourceDirectorySet(name, dirSetName);
        javaPatches.srcDir(project.file("patch/" + sourceSet.getName() + "/" + name));
        javaPatches.getFilter().include("**/*.json");

        File generatedSourceDir = project.file("build/generated/sources/patch/" + name + "/" + sourceSet.getName());

        TaskProvider<PatchTask> patchTask = tasks.register("patch" + capitalize(name), PatchTask.class, task -> {
            task.patches = javaPatches;
            task.sources = sources;
            task.generatedSourceDir = generatedSourceDir;
        });

        compileTask.setSource(filterSources(compileTask.getSource(), sources, javaPatches).plus(project.fileTree(generatedSourceDir)));
        compileTask.dependsOn(patchTask);
    }


    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toTitleCase(str.charAt(0)) + str.substring(1);
    }


    private FileCollection filterSources(FileTree allSources, SourceDirectorySet sources, SourceDirectorySet patches) {

        return allSources.filter(file -> {
            for(File srcDir : sources.getSourceDirectories()) {
                for (File dir : patches.getSourceDirectories()) {

                    String patchPath = dir.getAbsolutePath();
                    String sourcePath = srcDir.getAbsolutePath();

                    if (!file.getAbsolutePath().startsWith(sourcePath)) continue;

                    String suffix = file.getAbsolutePath().substring(sourcePath.length());
                    File existing = new File(patchPath + suffix.replace(".java", ".json"));

                    if (patches.contains(existing)) {
                        return false;
                    }
                }
            }

            return true;
        });
    }

}
