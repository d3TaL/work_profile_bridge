# Profile Link Bridge

Восстановленный проект 0.1.1-recovered-tls, Kotlin/Compose, Android 11+ (minSdk 30), compile/target 37. Основная цель: основной профиль → рабочий профиль или Samsung Secure Folder → Ozon/банк.

После очистки среды 0.1.0 воссоздана из кода переписки. Это новая сборка, не побайтовая копия старого архива. Старая signing key не восстановлена. Подробности: [RECOVERY.md](docs/RECOVERY.md).

Сейчас реализованы HTTPS-валидация, ядро маршрутизации, локальный выбор обработчика, диагностика CrossProfileApps и новый TLS 1.3 ping с certificate pinning. Код сессии не является постоянным сопряжением. URL через TLS пока не передаются.

Android 17 запрещает межпрофильный loopback по умолчанию независимо от targetSdk. Поэтому связь двух копий через localhost не является гарантированным решением для основного устройства. Подробности и ограничения — [COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Документы

| Файл | Назначение |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Инструкции для разработки |
| [DEVELOPMENT.md](docs/DEVELOPMENT.md) | Сборка и проверка на Samsung |
| [VALIDATION.md](docs/VALIDATION.md) | Результаты этой сборки |
| [REQUIREMENTS.md](docs/REQUIREMENTS.md) | Цель и критерии MVP |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Модули и поток данных |
| [PROFILES.md](docs/architecture/PROFILES.md) | Идентичность пространств |
| [TRANSPORTS.md](docs/architecture/TRANSPORTS.md) | Способы передачи |
| [PROTOCOL.md](docs/architecture/PROTOCOL.md) | Будущий протокол URL |
| [LINK_ROUTING.md](docs/architecture/LINK_ROUTING.md) | Правила и входящие ссылки |
| [ANDROID_LIFECYCLE.md](docs/architecture/ANDROID_LIFECYCLE.md) | Фон и блокировки |
| [SECURITY.md](docs/SECURITY.md) | Модель угроз |
| [SECURITY_ALIGNMENT.md](docs/SECURITY_ALIGNMENT.md) | Применённые меры и diff |
| [UX.md](docs/UX.md) | Пользовательские состояния |
| [COMPATIBILITY.md](docs/COMPATIBILITY.md) | Матрица устройств |
| [TESTING.md](docs/TESTING.md) | Стратегия проверок |
| [IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) | Следующие этапы |
| [ADR-001](docs/adr/001-multi-transport.md) | Несколько транспортов |
| [ADR-002](docs/adr/002-diagnostic-probe-isolation.md) | Изоляция диагностики |
| [ADR-003](docs/adr/003-pinned-tls-probe.md) | TLS-проба |
| [REFERENCES.md](docs/REFERENCES.md) | Официальные источники |

## Сборка

Полный JDK 17, Android SDK Platform 37.0, Build Tools 36.0.0. SDK указывается в local.properties (не коммитить).

```bash
./gradlew :core:test :app:assembleDebug :app:lintDebug
./gradlew :app:assembleRelease :app:lintRelease
```

Release не подписан производственным ключом. Для проверки на устройстве использовать диагностический APK. CI подготовлен с фиксированными commit SHA, удалённо не запускался.
