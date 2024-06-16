plugins {
    id("buildlogic.java-conventions")
    id("io.github.goooler.shadow") version "8.1.7"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(project(":movecraft-v1_18"))
    implementation(project(":movecraft-v1_20"))
    implementation(project(":movecraft-api"))
    implementation(project(":movecraft-datapack"))
    implementation("org.yaml:snakeyaml:2.0")
}

tasks.shadowJar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }

    archiveBaseName.set("Movecraft")
    archiveClassifier.set("")
    archiveVersion.set("")

    dependencies {
        include(project(":movecraft-api"))
        include(project(":movecraft-v1_18"))
        include(project(":movecraft-v1_20"))
        include(project(":movecraft-datapack"))
    }

    relocate("it.unimi", "net.countercraft.movecraft.libs.it.unimi")
    relocate("net.kyori", "net.countercraft.movecraft.libs.net.kyori")
    relocate("org.roaringbitmap", "net.countercraft.movecraft.libs.org.roaringbitmap")
}

description = "Movecraft"
