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
    configurations = listOf(project.configurations.runtimeClasspath.get())
    dependencies { exclude { it.moduleGroup != "org.bstats" } }
    relocate("org.bstats", project.group.toString())

    archiveBaseName.set("FlexBans")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

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
        val shadowJarTask = tasks.named("shadowJar").get() as Jar
        val jarFile = shadowJarTask.archiveFile.get().asFile
        val targetDir = file("../build/libs/")
        val targetFile = File(targetDir, "${rootProject.name}-${project.version}.jar")

        targetDir.mkdirs()
        jarFile.copyTo(targetFile, overwrite = true)
        jarFile.delete()
    }
}

tasks.named("build") {
    finalizedBy(moveJar)
}
