# Локальный агент — обзор

Десктопное приложение на Java (Swing). Считает активное время за компьютером, показывает статистику локально, позже сможет синхронизироваться с веб-аккаунтом.

## Что умеет сейчас
- Глобальный перехват мыши/клавиатуры (JNativeHook)
- Определение активного окна на Windows (JNA); Linux/macOS — заглушки фокуса
- Idle: нет активности N секунд → статус IDLE (время работы не пишется)
- Start / Pause записи времени
- Локальная статистика: день / неделя / месяц / год / всё время
- Трей: сворачивание в иконку
- Активация ключом или режим «только локально»

## Чего ещё нет
- Реальная отправка данных на сервер
- Проверка activation key на бэкенде
- Смена языка из UI (язык берётся из настроек/конфига)
- Полноценный фокус окна на Linux/macOS

## Как устроен код (кратко)

```
tracker-agent/
  ui/           — окно, вкладки, трей, диалог активации
  tracking/     — TrackingEngine: Start/Pause + мониторы
  activity/     — мышь/клавиатура
  idle/         — IDLE / ACTIVE
  focus/        — активное окно ОС (windows/linux/macos)
  buffer/       — интервалы текущей сессии в памяти
  storage/      — settings.json, intervals.json
  stats/        — агрегация статистики
```

Общий код (i18n и т.п.): модуль `tracker-common`.

## Данные на диске
Каталог: `%USERPROFILE%\.timetracker\` (Linux/macOS: `~/.timetracker/`)

| Файл | Назначение |
|---|---|
| `settings.json` | язык, ключ, local-only, пройдена ли активация |
| `intervals.json` | закрытые рабочие интервалы (для статистики) |

## Конфиг приложения
`tracker-agent/src/main/resources/application.properties`

| Ключ | Смысл | По умолчанию |
|---|---|---|
| `app.language` | `en` / `uk` (до первого сохранения настроек) | `en` |
| `idle.timeout.seconds` | через сколько секунд бездействия → IDLE | `60` (1 минута) |
| `idle.check.interval.seconds` | как часто проверять idle | `5` |
| `focus.poll.interval.seconds` | как часто читать активное окно | `10` |

## Связанные документы
- [Интерфейс и кнопки](ui.md)
- [Сборка и запуск](packaging.md)

## PlantUML
Схемы процессов модуля: [`tracker-agent/docs/puml/`](../../tracker-agent/docs/puml/)
- `overview.puml` — компоненты
- `startup-activation.puml` — старт и активация
- `tracking-loop.puml` — Start/Pause/IDLE
- `sync-button.puml` — условия кнопки Send to server
- `statistics.puml` — агрегация статистики
