import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.internal.os.OperatingSystem

plugins {
    java
    id("java-library")
    id("io.freefair.lombok") version "5.3.0"
    id("com.github.johnrengelman.shadow") version "4.0.4"
}

val lwjglVersion = "3.2.3"
val jomlVersion = "1.10.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val lwjglNatives = when (OperatingSystem.current()) {
    OperatingSystem.LINUX -> "natives-linux"
    OperatingSystem.MAC_OS -> "natives-macos"
    OperatingSystem.WINDOWS -> if (System.getProperty("os.arch").contains("64")) "natives-windows" else "natives-windows-x86"
    else -> throw Error("Unrecognized or unsupported Operating system. Please set \"lwjglNatives\" manually")
}

group = "nl.tue.visualcomputingproject.group9a"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral() {
        content {
            excludeGroup("javax.media") //Exclude this group since it needs to be looked up in the OSgeo repository.
        }
    }
    maven(url = "https://repo.osgeo.org/repository/release/")
    maven(url = "https://raw.github.com/SpinyOwl/repo/releases/")
}


dependencies {
    implementation("com.google.guava:guava:30.1-jre")

    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("org.slf4j:slf4j-simple:1.7.30")
    implementation("org.slf4j:jcl-over-slf4j:1.7.30")

    implementation("org.geotools:geotools:24.2")
    implementation("org.geotools:gt-wfs-ng:24.2")
    implementation("org.geotools:gt-epsg-hsql:24.2")
    implementation("org.geotools:gt-coverage:24.2")
    implementation("org.geotools:gt-geotiff:24.2")
    implementation("org.geotools:gt-wmts:24.2")
    implementation("org.geotools:gt-swing:24.2")

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjglx:lwjgl3-awt:0.1.8")

    implementation("im.bci:pngdecoder:0.13")

    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-assimp")
    implementation("org.lwjgl", "lwjgl-bgfx")
    implementation("org.lwjgl", "lwjgl-egl")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-jawt")
    implementation("org.lwjgl", "lwjgl-nanovg")
    implementation("org.lwjgl", "lwjgl-nuklear")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-par")
    implementation("org.lwjgl", "lwjgl-stb")
    implementation("org.lwjgl", "lwjgl-vulkan")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-bgfx", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-nanovg", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-nuklear", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-par", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)
    if (lwjglNatives == "natives-macos") runtimeOnly("org.lwjgl", "lwjgl-vulkan", classifier = lwjglNatives)
    implementation("org.joml", "joml", jomlVersion)

    compileOnly("org.projectlombok:lombok:1.18.16")
    annotationProcessor("org.projectlombok:lombok:1.18.16")

    testCompileOnly("org.projectlombok:lombok:1.18.16")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.16")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("project-full-jar")
        mergeServiceFiles()
        manifest {
            attributes(mapOf(
                    "Main-Class" to "nl.tue.visualcomputingproject.group9a.project.Main",
                    "Specification-Title" to "3D terrain reconstruction and navigation",
                    "Specification-Version" to "1.0",
                    "Specification-Vendor" to "Group 9a",
                    "Implementation-Title" to "nl.tue.visualcomputingproject.group9a.project",
                    "Implementation-Version" to "1.0",
                    "Implementation-Vendor" to "Group 9a"
            ))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

