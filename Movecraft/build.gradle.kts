plugins {
    id("buildlogic.java-conventions")
    id("io.github.goooler.shadow") version "8.1.7"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

dependencies {
    runtimeOnly(project(":movecraft-v1_18", "reobf"))
    runtimeOnly(project(":movecraft-v1_20", "reobf"))
    runtimeOnly(project(":movecraft-v1_21", "reobf"))
    implementation(project(":movecraft-api"))
    compileOnly("org.yaml:snakeyaml:2.0")
}

tasks.shadowJar {
    archiveBaseName.set("Movecraft")
    archiveClassifier.set("")
    archiveVersion.set("")

    dependencies {
        include(project(":movecraft-api"))
        include(project(":movecraft-v1_18"))
        include(project(":movecraft-v1_20"))
        include(project(":movecraft-v1_21"))
    }
}

tasks.processResources {
    from(rootProject.file("LICENSE.md"))
    from(project(":movecraft-datapack").file("build/zip/movecraft-data.zip"))
    dependsOn(project(":movecraft-datapack").tasks.build)
    filesMatching("*.yml") {
        expand(mapOf("projectVersion" to project.version))
    }
}

description = "Movecraft"
