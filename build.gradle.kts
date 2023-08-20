plugins {
    `java-gradle-plugin`
    id("maven-publish")
}

group = "org.wallentines"
version = "0.1.1-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://maven.wallentines.org/")
    mavenLocal()
}

dependencies {
    implementation("org.wallentines:midnightcfg-api:2.0.0-SNAPSHOT")
    implementation("org.wallentines:midnightcfg-codec-json:2.0.0-SNAPSHOT")
}

gradlePlugin {
    val multiVersion by plugins.creating {
        id = "org.wallentines.gradle-patch"
        implementationClass = "org.wallentines.gradle.patch.PatchPlugin"
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}