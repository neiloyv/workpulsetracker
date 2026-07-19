plugins {
    // Frontend is built with Node/Vite. Use npm scripts inside tracker-ui.
}

tasks.register("npmInstall") {
    group = "build"
    description = "Install tracker-ui npm dependencies"
    doLast {
        exec {
            workingDir = projectDir
            commandLine(if (System.getProperty("os.name").lowercase().contains("win")) listOf("npm.cmd", "install") else listOf("npm", "install"))
        }
    }
}

tasks.register("npmBuild") {
    group = "build"
    description = "Build tracker-ui production bundle"
    dependsOn("npmInstall")
    doLast {
        exec {
            workingDir = projectDir
            commandLine(if (System.getProperty("os.name").lowercase().contains("win")) listOf("npm.cmd", "run", "build") else listOf("npm", "run", "build"))
        }
    }
}
