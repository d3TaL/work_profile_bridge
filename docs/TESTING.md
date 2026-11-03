# Тестирование

core/src/test: проверки SafeLink, RouteResolver, PairingCode и настоящих TLS sockets на Linux. Синтетический test-only.p12 — исключительно test fixture; пароль fixture-only публичный и не является production credential. Файл не входит в core main JAR/Android APK. Секреты пользователя в тестах запрещены.

Проверять правильный pin и token, неверный pin, неверный token, запрет TLS 1.2 fallback, закрытую сессию, URL limits/authority, неизменность query и неоднозначность маршрутов. Результаты нового прогона — VALIDATION.md.

Задачи CI: :core:test, :app:verifyRoborazziDebug (все app unit-тесты и сверка скриншотов), :app:assembleDebug, :app:lintDebug, :app:assembleRelease, :app:lintRelease. Instrumentation на устройстве/эмуляторе не настроен. Linux TLS не доказывает работу Samsung KeyStore/профилей.

## UI-тесты на JVM (app/src/test)

Сборка и тесты только в GitHub Actions. Применены Android skills из
github.com/android/skills, commit 6daa53ef27fa2aa87827d183a85c06cedceca833:
testing-setup (2026-10-28), edge-to-edge (2026-08-24), navigation-event (2026-10-07).

- Robolectric 4.17 (SDK 37) запускает Compose UI-тесты без устройства. Образ
  android-all-instrumented получает Gradle (lockfile + SHA-256 metadata),
  Robolectric работает offline. Для SDK 37 нужен JDK 21: только test JVM через
  toolchain, APK собирается JDK 17.
- Тестовая ComponentActivity регистрируется в Robolectric PackageManager из теста;
  ui-test-manifest не подключается, APK не получает лишних exported Activity.
- Поведение: согласие на банковский AUTOMATIC, выход из выделения по «назад»,
  шаги мастера и восстановление шага после saved state, кнопки передачи/Knox,
  «назад» на вкладках и экране передачи (через NavigationEvent dispatcher).
- Скриншоты Roborazzi 1.77.0 (Robolectric Native Graphics) в
  app/src/test/screenshots: светлая/тёмная тема всех экранов, шрифт 150 %,
  400×500 dp (половина разделённого экрана), альбомная, кадр жеста «назад».

Отступления от testing-setup: Compose Preview Screenshot Testing не используется —
для AGP 9.4 skill предлагает устаревший неподдерживаемый alpha-плагин, а test suites
требуют AGP 9.5-alpha; AGP не обновлялся. Roborazzi — вариант на Robolectric из того
же skill. Hilt/DI и JaCoCo не добавлены: AGENTS.md запрещает DI без необходимости,
тесты не пишутся ради покрытия. Девять размеров на экран сокращены до набора выше.
Dropshots/инструментальные тесты требуют устройства; системные панели и IME
проверяются на телефоне.

### Скриншоты

- Push: `verifyRoborazziDebug` сравнивает с эталонами; при расхождении артефакт
  `profile-bridge-screenshot-diff-*` содержит `_actual` и `_compare`.
- Намеренное изменение UI: запустить Android checks вручную с `screenshots=record`
  в ветке, скачать `profile-bridge-screenshots-*`, просмотреть и закоммитить в
  app/src/test/screenshots. Эталоны создаются только в CI (одинаковый рендер).
- Обычный `testDebugUnitTest` без режима Roborazzi пропускает захват.

Скриншоты JVM не заменяют проверку на телефоне: Robolectric не рисует системные
панели, клавиатуру, Samsung One UI и реальный жест «назад».

На устройстве: оба контейнера отдельно и одновременно; открыто/заблокировано; экран выключен; процесс убит; force-stop; перезагрузка; VPN и battery restrictions. Банки — только согласованный безопасный сценарий без настоящего платежа.
