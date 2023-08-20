## GradlePatch

### Overview
This project is designed to let you patch files included from other source sets for compilation with another source set.
It was designed for use with my [GradleMultiVersion plugin](https://github.com/M1dnightNinja/gradle-multi-version), but
can be used with any project given you know the source set and source directory set you want to patch.

## Usage
### Setup
The plugin is currently not hosted on any public maven repository. To use it, first clone the repository. Then build it
by running `gradlew build` in its root. Then publish it to the local maven repository using
`gradlew publishToMavenLocal`. Then, add the local maven repository to your `settings.gradle.kts`:
```
pluginManagement {
    repositories {
        mavenLocal()
    }
}
```
Alternatively, you can reference it in your project directly by adding a reference to the cloned repository to your
`settings.gradle.kts`:
```
pluginManagement {
    includeBuild("../path/to/cloned/repo")
}
```

Then, add the plugin to your `build.gradle.kts`:
```
plugins {
    id("org.wallentines.gradle-patch") version "0.1.0"
}
```

Then, you can start configuring the plugin

### Patch Sets
To use the plugin, you first need to define a patch set. To do this, you need to know 1) What it should be called, 2)
Which source directory set to use to find sources to patch, and 3) The compile task to modify to use those patched 
sources. To define a patch set, put the following in the `patch` section of your `build.gradle.kts`:
```
patch {
    patchSet("java", sourceSets["main"], sourceSets["main"].java, tasks.compileJava.get())
}
```
Once defined, create a directory with the following name: `patch/[source set name]/[name]/`, and put your patch files in
it. Patch files will automatically patch source files which share the same name and path as them. For example, a patch 
file located at `patch/main/java/Main.json` will patch the file at `src/main/java/Main.java`

### Patch Files
Patch files are written in JSON format. The root of each patch file should be an array with zero or more *patch entries*.
Patch entries are JSON objects which define how the file in question should be patched. Each patch entries needs at 
least two things: A *patch type* (`"type": "insert"/"set"/"replace"/etc.`) and one or more *line rules* (`"lines": []`)

### Patch Types
Patch entries each have a single patch type. This determines the action which will be taken on all affected lines, as
well as additional required fields on the patch entry object. As of now, there are five patch types: `insert`, 
`insert_before`, `set`, `replace`, and `replace_regex`

#### insert
A patch entry of type `insert` adds the additional required field `value`. It will insert a line with the contents of 
`value` after each affected line.

#### insert_before
A patch entry of type `insert_before` adds the additional required field `value`. It will insert a line with the 
contents of `value` before each affected line.

#### set
A patch entry of type `set` adds the additional required field `value`. It will change each affected line to contain 
the contents of `value`.

#### replace
A patch entry of type `replace` adds the additional required fields `find` and `replace`. It will look for all instances 
of the contents of `find` on all affected lines, and replace all of them with the contents of `replace`

#### replace_regex
A patch entry of type `replace_regex` adds the additional required fields `find` and `replace`. It will look for all 
instances of the regex pattern defined by `find` on all affected lines, and replace all of them with the contents of 
`replace`

### Line Rules
Line rules define which lines should be affected by a given patch entry. There is an array of line rules in each patch
entry. Each line rule may be formatted in a number of ways. See the example below:
```
"lines": [
    6,   // Just line 6
    "all"   // All lines in the file
    [ 10, 14 ],   // All lines between 10 and 14 (inclusive)
    { "value": 6 }   // Just line 6
    { "values": [ 6,7,10,3 ] }   // Lines 6, 7, 10, and 3 (in that order)
    { "min": 10, "max": 14 },   // All lines between 10 and 14 (inclusive),
    { "find": "Hello" },    // All lines with the which contain text "Hello"
    { "find_regex": "^    public void (.*)\(\) {" }  // All lines which match the given regex pattern
]
```

### Example
An example patch file may look something like the following:
```json
[
        {
            "type": "set",
            "lines": [ 32 ],
            "value": "    return value.toString();"
        },
        {
            "type": "insert",
            "lines": [
                {
                    "value": 11
                }
            ],
            "value": "    value = 33;"
        },
        {
            "type": "replace",
            "lines": [ "all" ],
            "find": "toFind()",
            "replace": "replaced()"
        },
        {
            "type": "replace_regex",
            "lines": [ 32, 55 ],
            "find": "^    toFind\\((.*)\\);",
            "replace": "    replaced($1);"
        }
    ]
```
