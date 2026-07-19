plugins {
    application
    id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
    implementation(project(":tracker-common"))

    implementation("com.github.kwhat:jnativehook:2.2.2")
    implementation("net.java.dev.jna:jna:5.15.0")
    implementation("net.java.dev.jna:jna-platform:5.15.0")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.formdev:flatlaf:3.5.4")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.12")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("com.timetracker.agent.TrackerAgentApplication")
}

val appDisplayName = "TimeTracker Agent"
val appVendor = "TimeTracker"
val packagedAppVersion = version.toString().removeSuffix("-SNAPSHOT")

tasks.shadowJar {
    archiveBaseName.set("tracker-agent")
    archiveClassifier.set("all")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "com.timetracker.agent.TrackerAgentApplication"
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

/**
 * Кроссплатформенная установка:
 * 1) shadowJar — один Fat JAR, запускается на Windows/macOS/Linux при наличии JDK/JRE 17+.
 * 2) jpackageNative — нативный установщик ТОЛЬКО для той ОС, на которой запускаешь задачу
 *    (Windows → .msi/.exe, macOS → .dmg/.pkg, Linux → .deb/.rpm).
 *    Кросс-компиляции у jpackage нет: Windows-установщик собирай на Windows и т.д.
 */
val jpackageInputDirectory = layout.buildDirectory.dir("jpackage-input")
val jpackageOutputDirectory = layout.buildDirectory.dir("jpackage")

tasks.register<Copy>("prepareJpackageInput") {
    group = "distribution"
    description = "Копирует Fat JAR во входную папку для jpackage"
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(jpackageInputDirectory)
    rename { "tracker-agent.jar" }
}

tasks.register<Exec>("jpackageNative") {
    group = "distribution"
    description = "Собирает нативный установщик агента для текущей ОС (нужен JDK 17+ с jpackage)"
    dependsOn("prepareJpackageInput")

    val operatingSystemName = System.getProperty("os.name").lowercase()
    val packageType = when {
        operatingSystemName.contains("win") -> "msi"
        operatingSystemName.contains("mac") || operatingSystemName.contains("darwin") -> "dmg"
        else -> "deb"
    }

    val javaHome = System.getProperty("java.home")
    val jpackageExecutable = if (operatingSystemName.contains("win")) {
        "$javaHome\\bin\\jpackage.exe"
    } else {
        "$javaHome/bin/jpackage"
    }

    doFirst {
        val outputDirectory = jpackageOutputDirectory.get().asFile
        outputDirectory.mkdirs()
        logger.lifecycle(
            "jpackage: type={}, input={}, output={}",
            packageType,
            jpackageInputDirectory.get().asFile,
            outputDirectory
        )
    }

    executable = jpackageExecutable
    args(
        listOf(
            "--name", appDisplayName,
            "--app-version", packagedAppVersion,
            "--vendor", appVendor,
            "--input", jpackageInputDirectory.get().asFile.absolutePath,
            "--main-jar", "tracker-agent.jar",
            "--main-class", "com.timetracker.agent.TrackerAgentApplication",
            "--type", packageType,
            "--dest", jpackageOutputDirectory.get().asFile.absolutePath,
            "--description", "Local automatic time tracker agent"
        )
    )
}
