# Документация WorkPulseTracker

Здесь храним описание того, **как устроен продукт** и **как им пользоваться** (кнопки, режимы, ограничения).

Код меняется часто — если поведение UI/логики поменяли, обнови соответствующий файл в `docs/` **и** связанные `.puml` в модуле.

## Оглавление

### Локальный агент (`tracker-agent`)
| Документ | О чём |
|---|---|
| [Обзор](agent/overview.md) | Что делает агент, основные модули, данные на диске |
| [Интерфейс](agent/ui.md) | Окно, вкладки, кнопки, условия показа/блокировки |
| [Сборка и запуск](agent/packaging.md) | `run` / Fat JAR / установщик Windows |

### Веб
| Документ | О чём |
|---|---|
| [Сервер](server/overview.md) | API, Hibernate, Liquibase, Google OAuth, БД |
| [Веб-UI](ui/overview.md) | Страницы лендинга, онбординга, организации |

### PlantUML-схемы по модулям
| Модуль | Папка |
|---|---|
| `tracker-agent` | [docs/puml](../tracker-agent/docs/puml/) |
| `tracker-common` | [docs/puml](../tracker-common/docs/puml/) |
| `tracker-server` | [docs/puml](../tracker-server/docs/puml/) |
| `tracker-ui` | [docs/puml](../tracker-ui/docs/puml/) |
| `tracker-android` | [docs/puml](../tracker-android/docs/puml/) |

## Языки
- **UI пользователя:** английский / украинский
- **Логи (консоль):** всегда английский
