# Локальный агент — сборка и запуск

## Требования
- JDK 17+
- Корень репозитория: `D:\Projects\timetracker`
- Если нет `gradlew`: `gradle wrapper`

---

## 1) Быстрый запуск для разработки
Окно откроется само (браузер не нужен).

```powershell
cd D:\Projects\timetracker
.\gradlew :tracker-agent:run
```

Остановка: трей → **Exit**, или `Ctrl+C` в консоли.

---

## 2) Fat JAR (нужна Java на машине — это не .exe)
```powershell
.\gradlew :tracker-agent:shadowJar
java -jar tracker-agent\build\libs\tracker-agent-0.1.0-SNAPSHOT-all.jar
```

Один переносимый `.jar`. Для запуска нужна установленная Java 17+.

---

## 3) Установщик Windows (.msi) — проба «как после установки»
Нужен JDK 17+ с утилитой `jpackage`.  
Собирается **только под ту ОС**, на которой запускаешь задачу.

```powershell
.\gradlew :tracker-agent:jpackageNative
```

Результат: `tracker-agent\build\jpackage\` (обычно `.msi`).

---

## Кратко

| Цель | Команда |
|---|---|
| Разработка / проверка UI | `.\gradlew :tracker-agent:run` |
| Готовый jar | `shadowJar` → `java -jar ...` |
| Установщик Windows | `.\gradlew :tracker-agent:jpackageNative` |
