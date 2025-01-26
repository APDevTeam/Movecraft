plugins {
    `maven-publish`
    id("buildlogic.java-conventions")
    id("io.github.goooler.shadow") version "8.1.7"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

dependencies {
    runtimeOnly(project(":movecraft-v1_20", "reobf"))
    runtimeOnly(project(":movecraft-v1_21", "reobf"))
    implementation(project(":movecraft-api"))
    compileOnly("org.yaml:snakeyaml:2.0")
    testImplementation(libs.org.junit.jupiter.junit.jupiter.api)
    testImplementation(libs.junit.junit)
    testImplementation(libs.org.hamcrest.hamcrest.library)
    testImplementation("org.mockito:mockito-core:5.13.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.18:2.85.2")
}

tasks.shadowJar {
    archiveBaseName.set("Movecraft")
    archiveClassifier.set("")
    archiveVersion.set("")

    dependencies {
        include(project(":movecraft-api"))
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
                platformVersions.set(listOf("1.20.6", "1.21.4"))
            }
        }
    }
}

description = "Movecraft"
