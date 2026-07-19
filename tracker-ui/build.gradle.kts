plugins {
    // Заглушка: веб-интерфейс будет подключён позже (например, Node/Vite или Java frontend tooling).
}

tasks.register("prepareFrontendStub") {
    group = "build"
    description = "Placeholder task for future tracker-ui frontend build"
    doLast {
        logger.lifecycle("tracker-ui: frontend module stub — no build yet")
    }
}
