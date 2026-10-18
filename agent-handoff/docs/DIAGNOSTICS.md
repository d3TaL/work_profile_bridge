# P0: точная TLS-диагностика

## Проблема

В work profile оба направления дают TLS_REJECTED. Раньше этот статус включал SSLHandshakeException и IllegalArgumentException, поэтому конкретная причина неизвестна. Одного изменения текста сообщения недостаточно.

## Модель результата

Определить immutable DiagnosticEvent/DiagnosticOutcome с полями:

- случайный локальный attempt ID (не токен аутентификации);
- сторона CLIENT/SERVER/SELF_CHECK и стадия;
- elapsed time от старта монотонными часами;
- безопасный reason code, success/failure/unknown и локальный статус сессии;
- TLS version/provider как технические данные, если они доступны без секретов;
- allowlisted exception class/cause category; errno только если действительно предоставлен платформой.

Не включать exception.message, stack trace с payload, URL, порт/полный адрес peer, session code, token, certificate contents, alias/private key. Device model/build добавлять в экспорт только через явный просмотр пользователем. Локальные attempt IDs двух устройств/профилей не выдавать за автоматическую корреляцию: сопоставлять по действию пользователя и направлению, либо внедрить отдельный несекретный run ID с ограниченным сроком.

Стадии: IDENTITY_LOAD, TLS_CONTEXT_INIT, BIND, LISTEN, TCP_CONNECT, TCP_ACCEPT, TLS_HANDSHAKE, PEER_VALIDATE, PROBE_WRITE, PROBE_READ, COMPLETE, SESSION_EXPIRED, STOPPED. Записывать стадию до операции и её outcome после. При ошибке обязательно оставить последнюю наблюдаемую стадию.

## Причины

| Наблюдение | Безопасная категория |
| --- | --- |
| ConnectException | CONNECTION_FAILED; REFUSED только при достоверной классификации errno |
| SocketTimeoutException | TIMEOUT с указанием текущей стадии |
| SecurityException / EACCES, EPERM | ACCESS_DENIED, без утверждения кто именно запретил |
| GeneralSecurityException при identity/context | IDENTITY_OR_CONTEXT_FAILURE с конкретной стадией |
| CertificateException внутри собственного verifier | PIN_MISMATCH / CERT_EXPIRED / CERT_NOT_YET_VALID / CERT_REJECTED, если реально различимы |
| SSLHandshakeException | HANDSHAKE_FAILED; использовать безопасную причину verifier, если наблюдалась |
| SSLProtocolException/SSLException | TLS_PROTOCOL_FAILURE; не угадывать точную причину по тексту |
| IllegalArgumentException | INVALID_CONFIGURATION с этапом; не общий «ошибка сертификата» |
| EOF/socket reset | PEER_CLOSED / CONNECTION_INTERRUPTED без утверждения об истечении токена |
| Неверный фиксированный ACK | UNEXPECTED_RESPONSE |
| Coroutine cancellation | CANCELLED, не UNAVAILABLE и не TLS_REJECTED |
| Иное | UNKNOWN_FAILURE, без утечки текста исключения |

Собственный TrustManager должен сохранять код отказа только в контексте конкретной проверки, без глобальной mutable переменной и гонки параллельных handshake. Не определять pin mismatch парсингом локализованного текста исключения. Подробности причины недоверия не передавать неаутентифицированному клиенту; клиент вправе видеть общий HANDSHAKE_FAILED, сервер локально — более точный код.

## Self-check

Добавить отдельную команду «Проверить TLS внутри этого профиля». Она создаёт ограниченную локальную тестовую сессию и соединяется с ней с правильным pin. Тест намеренно не равен пользовательскому запрету SELF для межпрофильного ввода кода. Не отключать pin и не открывать произвольные URI.

Использовать реальный AndroidKeyStore/Conscrypt путь с production-подобными настройками, не JVM fixture. Не менять пользовательскую identity. Последовательно проверить загрузку ключа, подготовку контекста, handshake и fixed ACK. По завершении закрыть оба сокета и очистить временный token. Отмена/смена экрана должны завершать тест; количество соединений ограничено.

Успех local self-check доказывает только этот путь внутри профиля. Он не доказывает доступность между профилями. Неуспех pin self-check не объяснять Android 17 cross-profile restriction.

## UI и экспорт

Показывать listener status, оставшееся время сессии, последнюю стадию клиента и локальный отказ сервера. TTL выводить из monotonic deadline; UI timer не определяет срок безопасности. Держать bounded ring buffer, например 64 события на попытку и не более 10 попыток, без постоянной фоновой записи. При process death не показывать старый LISTENING.

Кнопка «Отчёт» открывает предварительный просмотр безопасного JSON/текста. Экспорт — стандартный share/FileProvider с временным read grant, без write grant, без широких provider paths. Автоматическую отправку, сбор logcat и upload не добавлять.

## Критерии приёмки P0

1. И клиент, и listener дают стадию отказа; карта ошибок не теряется в ViewModel/UI.
2. Тайм-аут подключения отличается от handshake failure, expiry и invalid code.
3. Неверный pin закрывает соединение до отправки session token; plaintext fallback отсутствует.
4. Недоступность real device не заменяется утверждением о PASS; есть готовый эксперимент для владельца.
5. Один короткий эксперимент: self-check в обоих профилях; затем свежая сессия A → B и B → A при обеих видимых копиях, если это позволяет ОС; сохранить отчёт обеих сторон. Если разделённый экран недоступен, записать фактический lifecycle вместо объявления теста недействительным.
