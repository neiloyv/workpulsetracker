# Локальный агент — сборка и запуск

## Требования
- JDK 17+ (с `jpackage`)
- Для Windows MSI дополнительно: [WiX Toolset](https://wixtoolset.org/) 3.x в `PATH`
- Корень репозитория: проект WorkPulseTracker

---

## 1) Быстрый запуск для разработки
Окно откроется само (браузер не нужен).

```powershell
.\gradlew :tracker-agent:run
```

Остановка: трей → **Exit**, или `Ctrl+C` в консоли.

---

## 2) Fat JAR (нужна Java на машине — это не .exe)
```powershell
.\gradlew :tracker-agent:shadowJar
java -jar tracker-agent\build\libs\tracker-agent-0.1.0-SNAPSHOT-all.jar
```

---

## 3) Windows MSI для скачивания с сайта

### Сборка и публикация в `downloads/`
На **Windows**:

```powershell
.\gradlew :tracker-agent:publishWindowsMsi
```

Что делает задача:
1. Собирает Fat JAR
2. Запускает `jpackage` → `.msi`
3. Копирует установщик в  
   `downloads/workpulsetracker-agent-windows.msi`

### Раздача через API
`tracker-server` раздаёт файлы из папки `downloads/` (корень репо):

- URL файла: `http://localhost:8080/downloads/workpulsetracker-agent-windows.msi`
- Метаданные: `GET /api/downloads` (`windowsAvailable: true/false`)

UI (экран **Агент**) показывает кнопку «Скачать для Windows», если файл есть.

После публикации MSI перезапустите API, если он уже был запущен:

```powershell
.\gradlew :tracker-server:bootRun
```

Vite проксирует `/downloads` → `8080`, поэтому ссылка с `http://localhost:5173` тоже работает.

### Если MSI не собрался
Частые причины:
- нет WiX Toolset / не в `PATH`
- запускаете не на Windows
- нет JDK с `jpackage`

Проверьте вывод `jpackageNative` в `tracker-agent\build\jpackage\`.

---

## 4) Portable (app-image) — без установки
```powershell
.\gradlew :tracker-agent:jpackagePortable
```

Результат: `tracker-agent\build\jpackage-portable\`

---

## Кратко

| Цель | Команда |
|---|---|
| Разработка | `.\gradlew :tracker-agent:run` |
| Fat JAR | `.\gradlew :tracker-agent:shadowJar` |
| MSI + публикация для сайта | `.\gradlew :tracker-agent:publishWindowsMsi` |
| Portable | `.\gradlew :tracker-agent:jpackagePortable` |

macOS / Linux установщики на сайте пока помечены как «скоро».
