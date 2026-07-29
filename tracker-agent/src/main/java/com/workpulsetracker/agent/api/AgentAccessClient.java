package com.workpulsetracker.agent.api;

/**
 * Клиент проверки доступа агента к веб-сервису по email и access key.
 */
public final class AgentAccessClient {

    /**
     * Проверяет, что для указанного email существует access key на веб-сервисе.
     *
     * @param email     email пользователя
     * @param accessKey access key (роль пароля для агента)
     * @return {@code true}, если доступ разрешён
     */
    public boolean validateAccess(String email, String accessKey) {
        // TODO: вызвать endpoint веб-сервиса для проверки email + access key
        return true;
    }
}
