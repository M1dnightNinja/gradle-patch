plugins {
    `java-gradle-plugin`
    id("maven-publish")
}

group = "org.wallentines"
version = "0.2.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven("https://maven.wallentines.org/")
    mavenLocal()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
    workingDir(file("run/test"))
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