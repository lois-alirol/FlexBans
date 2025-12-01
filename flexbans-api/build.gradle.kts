plugins {
    id("java")
}

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("org.yaml:snakeyaml:2.0")
    implementation("org.snakeyaml:snakeyaml-engine:2.2")

    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
}

description = "flexbans-api"