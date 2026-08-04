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
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    implementation("com.github.librepdf:openpdf:1.3.43")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.12")

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("com.workpulsetracker.agent.TrackerAgentApplication")
}

val appDisplayName = "WorkPulseTracker Agent"
val appVendor = "WorkPulseTracker"
val packagedAppVersion = version.toString().removeSuffix("-SNAPSHOT")

tasks.shadowJar {
    archiveBaseName.set("tracker-agent")
    archiveClassifier.set("all")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "com.workpulsetracker.agent.TrackerAgentApplication"
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
 * 3) jpackagePortable — portable app-image (папка с .exe/.app и встроенной runtime, без установки).
 *    Кросс-компиляции у jpackage нет: Windows-артефакты собирай на Windows и т.д.
 */
val jpackageInputDirectory = layout.buildDirectory.dir("jpackage-input")
val jpackageOutputDirectory = layout.buildDirectory.dir("jpackage")
val jpackagePortableOutputDirectory = layout.buildDirectory.dir("jpackage-portable")

tasks.register<Copy>("prepareJpackageInput") {
    group = "distribution"
    description = "Копирует Fat JAR во входную папку для jpackage"
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(jpackageInputDirectory)
    rename { "tracker-agent.jar" }
}

fun jpackageExecutablePath(): String {
    val operatingSystemName = System.getProperty("os.name").lowercase()
    val javaHome = System.getProperty("java.home")
    return if (operatingSystemName.contains("win")) {
        "$javaHome\\bin\\jpackage.exe"
    } else {
        "$javaHome/bin/jpackage"
    }
}

fun jpackageCommonArgs(packageType: String, destinationDirectory: java.io.File): List<String> = listOf(
    "--name", appDisplayName,
    "--app-version", packagedAppVersion,
    "--vendor", appVendor,
    "--input", jpackageInputDirectory.get().asFile.absolutePath,
    "--main-jar", "tracker-agent.jar",
    "--main-class", "com.workpulsetracker.agent.TrackerAgentApplication",
    "--type", packageType,
    "--dest", destinationDirectory.absolutePath,
    "--description", "Local automatic time tracker agent"
)

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

    doFirst {
        val outputDirectory = jpackageOutputDirectory.get().asFile
        if (outputDirectory.exists()) {
            outputDirectory.deleteRecursively()
        }
        outputDirectory.mkdirs()
        logger.lifecycle(
            "jpackage: type={}, input={}, output={}",
            packageType,
            jpackageInputDirectory.get().asFile,
            outputDirectory
        )
    }

    executable = jpackageExecutablePath()
    args(jpackageCommonArgs(packageType, jpackageOutputDirectory.get().asFile))
}

tasks.register<Exec>("jpackagePortable") {
    group = "distribution"
    description = "Собирает portable app-image агента (папка с приложением и runtime, без установщика)"
    dependsOn("prepareJpackageInput")

    doFirst {
        val outputDirectory = jpackagePortableOutputDirectory.get().asFile
        if (outputDirectory.exists()) {
            outputDirectory.deleteRecursively()
        }
        outputDirectory.mkdirs()
        logger.lifecycle(
            "jpackage: type=app-image, input={}, output={}",
            jpackageInputDirectory.get().asFile,
            outputDirectory
        )
    }

    executable = jpackageExecutablePath()
    args(jpackageCommonArgs("app-image", jpackagePortableOutputDirectory.get().asFile))
}

val downloadsDirectory = rootProject.layout.projectDirectory.dir("downloads")

/**
 * Без WiX: portable app-image → ZIP для лендинга.
 * Для настоящего .msi нужен WiX Toolset в PATH, задача publishWindowsMsi.
 */
tasks.register<Zip>("publishWindowsDownload") {
    group = "distribution"
    description =
        "Собирает portable Windows-агент и кладёт ZIP в downloads/ (WiX не нужен)"
    dependsOn("jpackagePortable")
    onlyIf {
        System.getProperty("os.name").lowercase().contains("win")
    }
    from(jpackagePortableOutputDirectory)
    archiveFileName.set("workpulsetracker-agent-windows.zip")
    destinationDirectory.set(downloadsDirectory.asFile)
    doLast {
        val publishedFile = downloadsDirectory.file("workpulsetracker-agent-windows.zip").asFile
        logger.lifecycle("Published landing download: {}", publishedFile.absolutePath)
    }
}

tasks.register<Copy>("publishWindowsMsi") {
    group = "distribution"
    description =
        "Собирает Windows .msi (нужен WiX: light.exe/candle.exe в PATH) и копирует в downloads/"
    dependsOn("jpackageNative")
    onlyIf {
        System.getProperty("os.name").lowercase().contains("win")
    }
    from(jpackageOutputDirectory) {
        include("*.msi")
    }
    into(downloadsDirectory)
    rename { "workpulsetracker-agent-windows.msi" }
    doLast {
        val publishedFile = downloadsDirectory.file("workpulsetracker-agent-windows.msi").asFile
        if (!publishedFile.exists()) {
            throw GradleException(
                "Windows MSI not found after jpackage. Install WiX Toolset and ensure light.exe/candle.exe are on PATH."
            )
        }
        logger.lifecycle("Published landing MSI: {}", publishedFile.absolutePath)
    }
}
