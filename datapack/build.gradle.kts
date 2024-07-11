plugins {
    base
}

val zipTask = tasks.register<Zip>("zipFolder") {
    archiveFileName.set("movecraft-data.zip")
    destinationDirectory.set(file("$buildDir/zip"))

    from("src/main/resources/movecraft-data") {
        include("**/*")  // Include all files and folders within the specified folder
    }
}

tasks.build {
    dependsOn(zipTask)
}

description = "Movecraft-Datapack"
