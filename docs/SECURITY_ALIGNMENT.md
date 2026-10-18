# Best Practices and Security Alignment Update

Приоритет высокий: TLS peer authentication, входящие Intent и секреты. Применены официальные android-intent-security и android-permissions-security. Сам факт применения skills не является аудитом.

| Область | Риск | Изменения |
| --- | --- | --- |
| TLS | Ложный local listener, downgrade | core/tls: TLS 1.3, SHA-256 pin leaf, certificate validity до отправки token, без fallback |
| Ключи | Экспорт signing key | AndroidTlsIdentity: EC P-256 в AndroidKeyStore, off-main generation, отдельный diagnostic alias |
| Код сессии | Подмена/утечка credentials | PairingCode: pin+random256 token, canonical parsing, bounded size, redacted toString; доверие к перенесённому коду обязательно |
| Приёмник | Команды и бесконечное чтение | TlsProbe: только fixed ping, 127.0.0.1, lifetime 120 секунд, 32 connections, timeouts, active socket close |
| Intent | URI grants, caller spoofing | IncomingLinkReader/MainActivity: allowlist sanitize, одинаково cold/warm, очищенный Intent, не доверяем caller extras |
| Запуск | Неявный пакет/фон | LocalLinkLauncher: новый package-scoped ACTION_VIEW, проверка выбора и RESUMED после async query |
| Privacy | Логи и backup | Redacted types, нет Log/analytics, URL только в памяти, onStop cleanup, backup/transfer disabled, FLAG_SECURE |
| Supply chain | Изменение зависимостей | Fixed versions, wrapper checksum, regenerated lockfiles/verification, immutable Action SHA |

Изменённые области: core/src/main/kotlin/dev/profilebridge/core/{links,tls}, app/src/main/kotlin/dev/profilebridge/app/{platform,ui}, MainActivity, AndroidManifest, res/xml, Gradle files и CI. Это восстановление новых файлов, не diff с найденным старым архивом.

Полный unified diff текущих текстовых исходников относительно пустой базы: [security-recovery.patch](security-recovery.patch). Проверки — [VALIDATION.md](VALIDATION.md).

Риски, которые остаются: подмена out-of-band кода; отсутствие долгосрочного сопряжения/отзыва и mTLS; не проверенный Samsung AndroidKeyStore/Conscrypt путь; отсутствие URL delivery state machine и политики получателя. Локальная диагностика не запускает банк через сеть. Перед настоящими банковскими ссылками требуется отдельный review.
