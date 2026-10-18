# Проверки

## JVM

- URL parser: допустимый URL, размер, userinfo, host confusion, dot-boundary, IDN-политика, управляющие символы, raw query/fragment.
- Routing: точные правила, приоритет/длина пути, равнозначные маршруты и ручной выбор.
- Session code: canonical encoding, размер, неверный pin/token, срок, отсутствие секретов в toString.
- TLS: настоящий положительный handshake, неверный pin до отправки token, неправильный token, недопустимая версия, timeout/EOF, закрытие и отмена.
- Diagnostics: сохранение этапа через ViewModel, bounded buffer, safe export, отсутствие exception.message и секретных значений.
- Delivery после P3: TTL, replay, duplicate, отмена, обрыв после send и UNKNOWN_OUTCOME без автоповтора.

Для проверки сертификата использовать независимый негативный пример; не тестировать verifier, который всегда разрешает соединение. Сетевые тесты ограничить временем и гарантированно закрывать сервер. JVM Keystore fixture не доказывает Android provider compatibility.

## Android / instrumentation

AndroidKeyStore key и TLS self-check на реальном API/provider. Cold/warm Intent sanitization, malicious extras/grants, выбор handler, lifecycle race перед запуском, уничтожение процесса, конфигурационные изменения и секреты вне saved state. Собирать диагностический результат с клиента и listener.

Возможности окружения агента не предполагать: при отсутствии девайса или emulator instrumentation пометить NOT_RUN. Mock CrossProfileApps не является физическим межпрофильным тестом.

## Ручной эксперимент владельца после P0

1. Записать APK version/SHA, API, тип профиля и направление. Оба экземпляра должны быть одной версии.
2. Выполнить local self-check в основном и рабочем профиле; сохранить безопасные отчёты.
3. Создать свежую сессию в B, перенести код в A, проверить A → B до expiry. Если разделённый экран доступен — держать обе копии видимыми; иначе записать actual foreground/background.
4. Повторить B → A с новой сессией и новым кодом. Не использовать старый код из другой папки/профиля.
5. Если работоспособность доказана, отдельно проверить сворачивание, остановку listener и expiry. Secure Folder проверять независимо, когда доступен.

Для каждого шага применять [DEVICE_REPORT_TEMPLATE.md](DEVICE_REPORT_TEMPLATE.md). Одно и то же общее сообщение повторно без новых данных не запрашивать.

## Сборка и артефакты

Команды адаптировать к реальным модулям. Для прежней структуры:

```bash
./gradlew :core:test :app:assembleDebug :app:lintDebug :app:assembleRelease :app:lintRelease --dependency-verification strict
```

Подтвердить signature verification debug APK, zipalign с 16 KiB page alignment и PT_LOAD для включённых native libs. Проверить, что test fixtures и production keys не попали в APK. Release без производственной подписи явно так и назвать.

docs/VALIDATION.md содержит дату, commit, toolchain, команды, exit status, число тестов, lint результат, SHA-256 APK и NOT_RUN с причиной. Не копировать исторический PASS как свежую проверку. При обновлении verification metadata документировать bootstrap trust и источник артефактов; не автоматически принимать изменившиеся checksum.
