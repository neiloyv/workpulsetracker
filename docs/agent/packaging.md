# Локальный агент — сборка и запуск

## Требования
- JDK 17+
- Корень репозитория: `D:\Projects\workpulsetracker`
- Если нет `gradlew`: `gradle wrapper`

---

## 1) Быстрый запуск для разработки
Окно откроется само (браузер не нужен).

```powershell
cd D:\Projects\workpulsetracker
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

## 4) Portable (app-image) — без установки
Папка с приложением и встроенной Java runtime. Java на машине не нужна.  
Удобно для тестов: скопировал папку → запустил → удалил.

```powershell
.\gradlew :tracker-agent:jpackagePortable
```

Результат: `tracker-agent\build\jpackage-portable\`  
На Windows внутри — папка приложения и `.exe` (имя как у `WorkPulseTracker Agent`).

Запуск: двойной клик по `.exe` или из консоли:

```powershell
& "tracker-agent\build\jpackage-portable\WorkPulseTracker Agent\WorkPulseTracker Agent.exe"
```

Данные по-прежнему пишутся в `%USERPROFILE%\.workpulsetracker\` (не внутрь portable-папки).

---

## Кратко

| Цель | Команда |
|---|---|
| Разработка / проверка UI | `.\gradlew :tracker-agent:run` |
| Готовый jar | `shadowJar` → `java -jar ...` |
| Установщик Windows | `.\gradlew :tracker-agent:jpackageNative` |
| Portable (без установки) | `.\gradlew :tracker-agent:jpackagePortable` |

## Связь с веб-лендингом

1. Соберите и опубликуйте Windows portable ZIP (WiX не нужен):

```powershell
.\gradlew :tracker-agent:publishWindowsDownload
```

Файл появится как `downloads/workpulsetracker-agent-windows.zip`  
(внутри — папка с `.exe` и встроенной Java).

Если нужен настоящий установщик `.msi`, поставьте [WiX Toolset](https://wixtoolset.org/) 3.x+, добавьте `light.exe`/`candle.exe` в PATH и выполните:

```powershell
.\gradlew :tracker-agent:publishWindowsMsi
```

Затем укажите `DOWNLOAD_WINDOWS_URL=/downloads/workpulsetracker-agent-windows.msi`.

2. Запустите `tracker-server` с рабочей директорией в корне репозитория (или задайте `DOWNLOAD_DIRECTORY` абсолютным путём к `downloads`).

3. Vite проксирует `/downloads` → `:8080`. На лендинге кнопка Windows скачает MSI.

macOS/Linux: соберите `jpackageNative` на соответствующей ОС и положите файлы вручную:

- `downloads/workpulsetracker-agent-macos.dmg`
- `downloads/workpulsetracker-agent-linux.deb`

Для продакшена можно вместо локальной папки указать внешние URL в `DOWNLOAD_*_URL` (S3 / GitHub Releases / CDN).
