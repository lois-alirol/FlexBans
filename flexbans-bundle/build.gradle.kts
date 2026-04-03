plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

dependencies {
    implementation(project(":flexbans-api"))
    implementation(project(":flexbans-core"))
    implementation(project(":flexbans-bukkit"))
    implementation(project(":flexbans-bungee"))
    implementation(project(":flexbans-velocity"))
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    relocate("org.bstats", "fr.neocle.flexbans.bstats")
    relocate("org.eclipse.jetty", "fr.neocle.flexbans.libs.jetty")

    exclude("META-INF/LICENSE")
    exclude("META-INF/LICENSE.txt")
    exclude("META-INF/NOTICE")
    exclude("META-INF/NOTICE.txt")
    exclude("about.html")

    archiveBaseName.set("FlexBans")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    mergeServiceFiles()
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    manifest {
        attributes["Main-Class"] = "fr.neocle.flexbans.GuiLauncher"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

val moveJar by tasks.registering {
    dependsOn(tasks.named("shadowJar"))
    doLast {
        val shadowJarTask = tasks.named("shadowJar").get() as com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
        val jarFile = shadowJarTask.archiveFile.get().asFile

        if (!jarFile.exists()) {
            throw IllegalStateException("Le JAR n'a pas été généré : ${jarFile.absolutePath}")
        }

        val targetDirs = listOf(
            file("../build/libs/"),
            file("/Users/loisalirol/Documents/DevServers/paperproxied/plugins"),
            file("/Users/loisalirol/Documents/DevServers/velocity/plugins")
        )

        targetDirs.forEach { dir ->
            dir.mkdirs()
            val targetFile = File(dir, jarFile.name)
            jarFile.copyTo(targetFile, overwrite = true)
        }
    }
}

tasks.named("build") {
    finalizedBy(moveJar)
}

tasks.named("build") {
    finalizedBy(moveJar)
}
