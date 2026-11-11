plugins {
    id("buildlogic.java-conventions")
    id("io.papermc.paperweight.userdev")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

dependencies {
    api(project(":movecraft-api"))
    paperweight.paperDevBundle("26.3.build.+")
}

description = "Movecraft-v26_3"
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
