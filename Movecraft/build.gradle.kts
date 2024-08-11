import xyz.jpenilla.runpaper.task.RunServer

plugins {
    `maven-publish`
    id("buildlogic.java-conventions")
    id("io.github.goooler.shadow") version "8.1.7"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

val supportedMinecraftVersions = listOf(
    "1.18.2",
    "1.20.6",
    "1.21"
)

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
tasks {
    supportedMinecraftVersions.forEach { serverVersion ->
        register<RunServer>("run-$serverVersion") {
            minecraftVersion(serverVersion)
            jvmArgs("-DPaper.IgnoreJavaVersion=true", "-Dcom.mojang.eula.agree=true")
            group = "run paper"
            runDirectory.set(file("run-$serverVersion"))
            downloadPlugins {
                hangar("WorldEdit", "7.3.6")
                url("https://ci.lucko.me/job/spark/441/artifact/spark-bukkit/build/libs/spark-1.10.95-bukkit.jar")
                hangar("ViaVersion", "5.0.3")
                hangar("ViaBackwards", "5.0.3")
            }
            pluginJars(project.tasks.shadowJar.map { it.archiveFile }.get())
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "net.countercraft"
            artifactId = "movecraft"
            version = "${project.version}"

            artifact(tasks["shadowJar"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/apdevteam/movecraft")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

hangarPublish {
    publications.register("plugin") {
        version.set(project.version as String)
        channel.set("Release")
        id.set("Airship-Pirates/Movecraft")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms {
            register(io.papermc.hangarpublishplugin.model.Platforms.PAPER) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                platformVersions.set(supportedMinecraftVersions)
            }
        }
    }
}

description = "Movecraft"
