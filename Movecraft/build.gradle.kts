plugins {
    `maven-publish`
    id("buildlogic.java-conventions")
    id("com.gradleup.shadow") version "8.3.6"
    id("io.papermc.hangar-publish-plugin") version "0.1.3"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

dependencies {
    runtimeOnly(project(":movecraft-v1_20_6"))
    runtimeOnly(project(":movecraft-v1_21_1"))
    runtimeOnly(project(":movecraft-v1_21_4"))
    runtimeOnly(project(":movecraft-v1_21_5"))
    runtimeOnly(project(":movecraft-v1_21_8"))
    runtimeOnly(project(":movecraft-v1_21_10"))
    runtimeOnly(project(":movecraft-v1_21_11"))
    implementation(project(":movecraft-api"))
    compileOnly("org.yaml:snakeyaml:2.0")
}

tasks.shadowJar {
    archiveBaseName.set("Movecraft")
    archiveClassifier.set("")
    archiveVersion.set("")

    dependencies {
        include(project(":movecraft-api"))
        include(project(":movecraft-v1_20_6"))
        include(project(":movecraft-v1_21_1"))
        include(project(":movecraft-v1_21_4"))
        include(project(":movecraft-v1_21_5"))
        include(project(":movecraft-v1_21_8"))
        include(project(":movecraft-v1_21_10"))
        include(project(":movecraft-v1_21_11"))
    }

    manifest.attributes(
        "paperweight-mappings-namespace" to "mojang"
    )
}

tasks.processResources {
    from(rootProject.file("LICENSE.md"))
    from(project(":movecraft-datapack").file("build/zip/movecraft-data.zip"))
    dependsOn(project(":movecraft-datapack").tasks.build)
    filesMatching("*.yml") {
        expand(mapOf("projectVersion" to project.version))
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
                platformVersions.set(listOf("1.20.6", "1.21.1", "1.21.4", "1.21.5", "1.21.8", "1.21.10", "1.21.11"))
            }
        }
    }
}

description = "Movecraft"
