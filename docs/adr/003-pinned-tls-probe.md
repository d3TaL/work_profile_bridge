# ADR-003: TLS probe с pin

Принято для 0.1.1, Samsung compatibility NOT_TESTED. Используем системные JSSE/Conscrypt TLS 1.3 и AndroidKeyStore EC P-256 key. Public leaf certificate fingerprint SHA-256 переносится вне канала в полном коде PLB2 вместе с портом и временным bearer token.

Клиент проверяет pin и срок certificate в handshake до отправки token. Сервер проверяет token только внутри TLS. Нет TrustAll, TLS 1.2 или plaintext fallback. Один listener, максимум 32 подключения/120 секунд. Закрытие прерывает active socket и очищает token.

Осознанные пределы: код должен быть взят с доверенного экрана; утёкший код даёт право диагностического ping до срока; это не mTLS. Сертификат не подтверждает тип профиля или физическую модель устройства. Долгосрочное сопряжение/URL-протокол сюда не добавляем.
