plugins {
    id("buildlogic.java-conventions")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

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
        include(dependency("net.countercraft:movecraft-v1_18"))
        include(dependency("net.countercraft:movecraft-v1_20"))
        include(dependency("net.countercraft:datapack"))
    }

    relocate("it.unimi", "net.countercraft.movecraft.libs.it.unimi")
    relocate("net.kyori", "net.countercraft.movecraft.libs.net.kyori")
    relocate("org.roaringbitmap", "net.countercraft.movecraft.libs.org.roaringbitmap")

    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

description = "Movecraft"
