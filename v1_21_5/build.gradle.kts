plugins {
    id("buildlogic.java-conventions")
    id("io.papermc.paperweight.userdev")
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

dependencies {
    api(project(":movecraft-api"))
    paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
}

description = "Movecraft-v1_21_5"
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
