# Протоколы

## Реализованный TLS diagnostic v2

TLS 1.3; client проверяет pin leaf и срок сертификата до отправки токена. Внутри TLS запрос: 4 байта magic PLB2 (big endian) + 32 байта token. Ответ: 4 байта magic PLBO. Неверный token не получает ACK. В кадре нет места для URL или команды. Приёмник: одна обработка за раз, максимум 32 соединения, accept timeout 500 ms, handshake/read 2000 ms, срок 120 секунд по monotonic clock. Client connect/read: 2000 ms.

PLB2:port:base64url(pin):base64url(token), без padding; parser проверяет каноническую форму, длину и диапазон порта. Code.toString скрывает содержимое. Никакого plaintext downgrade.

## Планируемый URL protocol v1

Поля: version, requestId, senderId, receiverId, createdAt, expiresAt, targetPackage и originalUrl. Лимит URL 16 KiB, frame 64 KiB, TTL по умолчанию 60 секунд. Отправитель обязан быть связан с аутентифицированным peer; получатель применяет собственную политику.

Статусы: RECEIVED, AWAITING_USER, DISPATCHING, LAUNCH_DISPATCHED, REJECTED, EXPIRED, CANCELLED, UNKNOWN_OUTCOME. Перед побочным эффектом атомарно сохранить requestId и DISPATCHING. При падении в этом состоянии не повторять автоматически. Exactly-once выполнение чужой Activity не гарантируется транзакцией моста.

Дубликаты по (peerId, requestId) возвращают статус, изменённый payload с тем же id отклоняется. После timeout — status_query, без автоматического fallback. Дедупликация хранится 24 часа с лимитом; не вытеснять неистёкшие записи ради новых.
