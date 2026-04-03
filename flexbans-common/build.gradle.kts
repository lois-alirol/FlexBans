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
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation(project(":flexbans-core"))
    implementation(project(":flexbans-api"))

    implementation("org.eclipse.jetty:jetty-server:9.4.56.v20240826")
    implementation("org.eclipse.jetty.websocket:websocket-server:9.4.56.v20240826")
    implementation("org.eclipse.jetty.websocket:websocket-servlet:9.4.56.v20240826")
    implementation("org.eclipse.jetty:jetty-util:9.4.56.v20240826")
    implementation("org.eclipse.jetty:jetty-http:9.4.56.v20240826")
    implementation("org.eclipse.jetty:jetty-io:9.4.56.v20240826")
    implementation("org.eclipse.jetty:jetty-servlet:9.4.56.v20240826")
    implementation("org.eclipse.jetty:jetty-rewrite:9.4.56.v20240826")

    implementation("net.kyori:adventure-api:4.19.0")
    implementation("net.kyori:adventure-text-minimessage:4.19.0")
    implementation("net.kyori:adventure-platform-bungeecord:4.3.4")
}

tasks.test {
    useJUnitPlatform()
}