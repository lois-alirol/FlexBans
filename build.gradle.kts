plugins {
    java
}

allprojects {
    group = "fr.neocle"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
        maven("https://repo.opencollab.dev/main/")
    }
}

subprojects {
    plugins.apply("java")

    plugins.withType<JavaPlugin> {
        the<JavaPluginExtension>().apply {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }

        tasks.withType<JavaCompile>().configureEach {
            sourceCompatibility = "21"
            targetCompatibility = "21"
            options.release.set(21)
        }
    }
}