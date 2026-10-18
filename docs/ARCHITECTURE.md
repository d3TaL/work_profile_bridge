# Архитектура

## Реализованные компоненты

| Компонент | Ответственность |
| --- | --- |
| core/links | SafeLink, Route, RouteResolver без Android |
| core/tls | TLS contexts, pin validation, session invitation, bounded fixed ping |
| platform/AndroidTlsIdentity | EC P-256 signing key в AndroidKeyStore |
| platform/IncomingLinkReader | Allowlist IntentSanitizer, ограниченный текст |
| platform/LocalLinkLauncher | Локальный ACTION_VIEW по пользовательскому выбору |
| platform/ProfileDiagnostics | Доступные копии и запуск своего launcher |
| ui/ProbeViewModel | Срок жизни TLS-сессии, отмена, безопасные статусы |
| MainActivity/BridgeScreen | Диагностический интерфейс, URL только в памяти |

## Планируемый поток URL

Приём URL → валидация/правило → выбор endpoint/transport → аутентификация → политика получателя → дедупликация → допустимый запуск или действие через уведомление → отдельный статус.

Transport.probe, submit, queryStatus — будущие контракты. TLS ping пока не реализует submit и не принимает URL. После неизвестного результата запуска нельзя повторять действие через другой транспорт автоматически.

Endpoint: случайный instanceId, пользовательская метка, транспорт, key alias и версия протокола. Route: scheme/host/path, endpoint, package и доверенная подпись при необходимости. У каждого пространства отдельное хранилище и жизненный цикл.
