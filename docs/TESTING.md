# Тестирование

core/src/test: проверки SafeLink, RouteResolver, PairingCode и настоящих TLS sockets на Linux. Синтетический test-only.p12 — исключительно test fixture; пароль fixture-only публичный и не является production credential. Файл не входит в core main JAR/Android APK. Секреты пользователя в тестах запрещены.

Проверять правильный pin и token, неверный pin, неверный token, запрет TLS 1.2 fallback, закрытую сессию, URL limits/authority, неизменность query и неоднозначность маршрутов. Результаты нового прогона — VALIDATION.md.

Задачи: :core:test, :app:assembleDebug, :app:lintDebug, :app:assembleRelease, :app:lintRelease. Не считать Android UI/instrumentation настроенными: этого пока нет. Linux TLS не доказывает работу Samsung KeyStore/профилей.

На устройстве: оба контейнера отдельно и одновременно; открыто/заблокировано; экран выключен; процесс убит; force-stop; перезагрузка; VPN и battery restrictions. Банки — только согласованный безопасный сценарий без настоящего платежа.
