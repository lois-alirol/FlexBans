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
    implementation(project(":flexbans-api"))

    implementation("org.eclipse.jetty:jetty-server:9.4.56.v20240826")
    implementation("org.eclipse.jetty.websocket:websocket-server:9.4.56.v20240826")
    implementation("org.eclipse.jetty.websocket:websocket-servlet:9.4.56.v20240826")
    implementation("org.eclipse.jetty:jetty-util:9.4.56.v20240826")
    implementation("org.eclipse.jetty:jetty-http:9.4.56.v20240826")
    implementation("org.eclipse.jetty:jetty-io:9.4.56.v20240826")
    implementation("org.eclipse.jetty:jetty-servlet:9.4.56.v20240826")
    implementation("org.eclipse.jetty:jetty-rewrite:9.4.56.v20240826")

    implementation("commons-codec:commons-codec:1.16.1")

    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.3")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")

    implementation("javax.servlet:javax.servlet-api:4.0.1")

    implementation("org.mindrot:jbcrypt:0.4")

    implementation("org.yaml:snakeyaml:2.0")
    implementation("org.snakeyaml:snakeyaml-engine:2.2")
    implementation("com.zaxxer:HikariCP:7.0.2")

    implementation("org.jclarion:image4j:0.7")

    implementation("net.kyori:adventure-api:4.19.0")
    implementation("net.kyori:adventure-text-minimessage:4.19.0")
    implementation("net.kyori:adventure-platform-bungeecord:4.3.4")

    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    compileOnly("org.geysermc.geyser:api:2.4.2-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")

    compileOnly("com.gitlab.ruany:LiteBansAPI:0.5.0")

    implementation("com.google.code.gson:gson:2.10.1")
}


description = "flexbans-core"