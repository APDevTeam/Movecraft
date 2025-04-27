plugins {
    `java-library`
}

repositories {
    mavenLocal()
    maven("https://repo.maven.apache.org/maven2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

group = "net.countercraft"
version = "8.0.0_beta-6"

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}
