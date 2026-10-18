# Проверки восстановленной версии

## 2026-10-18 — 0.1.3-key-diagnostics

- APK commit: `fa66fe0`; автор/коммитер d3TaL.
- CI https://github.com/d3TaL/work_profile_bridge/actions/runs/39131660837 — SUCCESS.
- Локально `:core:test --dependency-verification strict` — PASS (24 теста).
- CI: core:test, assembleDebug/Release, lintDebug/Release — PASS, strict verification.
- Debug APK: `/storage/emulated/0/Download/artifacts/profile-bridge-0.1.3-key-diagnostics-fa66fe0.apk`.
- SHA-256: `d10ca52694c2add1bbd59530734e4e9062d1e0c7c3f1004cbe7438180417438f`.
- Оригинальный комплект: `Download/artifacts/run-39131660837/profile-bridge-apks-5/`.
  Обе контрольные суммы оригинальных APK проверены: OK.

Получен отчёт владельца `report-4703863896238174962.txt` из внутренней копии
0.1.2. Тип внутреннего пространства в самом отчёте не указан. Пять локальных
self-check попыток: identity/context PASS, bind/listen/connect/accept PASS,
handshake FAIL на обеих сторонах (ANDROID_OPENSSL); PEER_VALIDATE отсутствует.
Это наблюдение локального сбоя, не доказательство причины в Knox или сетевой
межпрофильной границе. Подробная причина в версии 0.1.2 не установлена.

Версия 0.1.3 добавляет проверку sign/verify того же AndroidKeyStore ключа и
allowlisted категории цепочки исключений, события выбора серверного ключа,
доступа к private key и certificate chain. Ни имя alias, ни сертификат, ни
challenge/signature, ни exception.message в экспорт не попадают.

Исследован upstream Conscrypt `NativeSsl.configureServerCertificate`,
`setCertificate` и `SSLParametersImpl` (google/conscrypt, master на дату работы).
Это описание upstream пути, не доказательство идентичности Samsung provider.
Ссылки: https://github.com/google/conscrypt/blob/master/common/src/main/java/org/conscrypt/NativeSsl.java
и https://github.com/google/conscrypt/blob/master/common/src/main/java/org/conscrypt/SSLParametersImpl.java .

Следующий эксперимент: только одна локальная самопроверка во внутренней копии,
затем экспорт нового отчёта. Межпрофильный тест до устранения локального отказа
не нужен. Физический результат 0.1.3 пока неизвестен.

## Текущая итерация 2026-10-18 — 0.1.2-diagnostics

### Подтверждённая CI-сборка

- Commit APK: `95171bf` (автор и коммитер d3TaL).
- GitHub Actions: https://github.com/d3TaL/work_profile_bridge/actions/runs/39130603916 — SUCCESS.
- Полный набор `:core:test :app:assembleDebug :app:lintDebug :app:assembleRelease
  :app:lintRelease --dependency-verification strict` — PASS на GitHub runner.
- Исправлена явная установка SDK и исключён удалённый legacy package `tools`.
  Дополнена отсутствовавшая SHA-256 запись `groovy-bom-4.0.29.pom` после
  отдельной загрузки с HTTPS Maven Central; прежние checksum сохранены.
- Debug APK скачан в `/storage/emulated/0/Download/artifacts/profile-bridge-0.1.2-diagnostics-95171bf.apk`.
- Debug SHA-256: `9560e6b53c6eb00f5995c1bde1a586bd7388f040540717ae9ea02901c3460b0b`.
- Release unsigned SHA-256: `3ccac29e4a10f391c2fd940cd38733eb9ecec2ce4e2e3da937ddd6c46c4b6220`.
- Оригинальные артефакты и `BUILD.txt`, `SHA256SUMS` сохранены в
  `/storage/emulated/0/Download/artifacts/run-39130603916/profile-bridge-apks-4/`.
  `sha256sum -c SHA256SUMS` — OK для обоих APK.
- Debug подписан ключом чистого CI runner; совместимость обновления со старой
  установленной версией не подтверждена. Физические проверки ещё не выполнены.

### Первоначальные локальные проверки (до подключения CI)

- JDK: Temurin 17.0.20.1, Linux aarch64; Gradle 9.8.1 (wrapper checksum включён).
- `:core:test --dependency-verification strict`: PASS, 23 теста. Включены неверный
  pin до отправки токена, локальный self-check, отмена listener во время handshake,
  ограничение журнала и исключение текста исключений из отчёта.
- Полный набор `:core:test :app:assembleDebug :app:lintDebug :app:assembleRelease
  :app:lintRelease --dependency-verification strict`: BLOCKED — Android SDK отсутствует.
  Компиляция Android UI, lint и APK этой итерации не подтверждены.
- При первой конфигурации обнаружены четыре отсутствующие записи checksum для
  транзитивных Maven metadata. Добавлены SHA-256 после отдельной загрузки с
  `https://repo.maven.apache.org/maven2/`; существующие checksum не заменены,
  strict verification сохранён. Это фиксация доверия к HTTPS Maven Central,
  не независимый аудит upstream.
- GitHub Actions дополнен APK/checksum/build ID и отчётами, retention 14 дней.
  Удалённый запуск не выполнен: git remote отсутствует. См. [CI.md](CI.md).
- Прочитан `android-intent-security` (Google android/skills,
  commit `5bda39f154fb859a08d45e2392f3568e049f83fb`, last-updated 2026-09-25).
  Применён к экспорту через non-exported FileProvider с узким cache-path и
  временным read grant после предварительного просмотра. Входящие Intent
  по-прежнему проходят общий обработчик cold/warm launch; не перенаправляются.
- Физический AndroidKeyStore/Conscrypt, work profile и Secure Folder не проверены.
- Новый APK и его SHA-256 пока отсутствуют. Файлы в `artifacts/` относятся к
  восстановленной версии; не являются результатом текущей итерации.
- Git checkout пока без коммитов. Текущие изменения сохранены в рабочем дереве;
  публикация и коммит этой итерации не выполнялись.

### Следующий эксперимент после успешной сборки

1. Запустить «Проверить TLS внутри этого профиля» в основной и рабочей копии.
2. Создать свежую сессию B, проверить её из A; затем свежую сессию A из B.
   Держать обе копии видимыми, если ОС позволяет; иначе записать фактическое
   состояние окон. Срок listener выводится из монотонного deadline.
3. В обеих копиях открыть «Диагностический отчёт», просмотреть и поделиться.
   Указать направление и тип пространства отдельно; attempt IDs локальные.
   Банковские URL, коды сессий и полный logcat не нужны.

## Исторические данные из импортированного проекта

Следующие сведения перенесены из архива и не являются проверками текущего checkout.

Дата: 2026-10-17. Версия 0.1.1-recovered-tls. Проверки выполнены заново после восстановления исходников; старые результаты не использованы.

| Проверка | Результат |
| --- | --- |
| core:test | 18 тестов, 0 failures, 0 errors |
| HTTPS validation | 5 тестов |
| Route resolver | 4 теста |
| Session code | 4 теста |
| TLS probe | 5 тестов: успешный TLS, неверный pin, неверный token, отказ TLS 1.2, закрытая сессия |
| assembleDebug / assembleRelease | PASS; Release с R8, без производственной подписи |
| lintDebug / lintRelease | PASS, warningsAsErrors, без baseline |
| apksigner verify Debug | PASS, APK Signature Scheme v2 |
| zipalign -c -P 16 4 Debug | PASS |
| ELF PT_LOAD alignment | Все четыре ABI graphics.path имеют alignment >= 16384 |
| Test fixture exclusion | test-only.p12 отсутствует в APK |
| Dependency verification | PASS: полный набор задач в strict mode, offline; 558 артефактов с SHA-256 |

Окружение: полный Temurin JDK 17; Gradle 9.8.1; SDK Platform 37.0; Build Tools 36.0.0. Wrapper JAR извлечён из проверенного дистрибутива Gradle. SHA-256 дистрибутива указан в wrapper properties. Зависимости закреплены lockfiles; SHA-256 metadata создаётся из полученных по HTTPS артефактов и затем проверяется в strict mode. Это начальная фиксация доверия, не независимый аудит всех зависимостей.

Gradle сообщает об устаревающих API до Gradle 10; сообщение не подавлено. CI подготовлен, но удалённо не запускался. Android instrumentation, Samsung KeyStore/Conscrypt TLS, расход батареи, фон и переходы между пространствами на физических устройствах НЕ проверены. JVM TLS-тесты не заменяют Android-тест.

Android 17 запрещает межпрофильный loopback по умолчанию независимо от targetSdk. Нельзя интерпретировать успешный JVM ping как доказательство работы портового моста на Android 17. См. [COMPATIBILITY.md](COMPATIBILITY.md).

APK — диагностический, с новым debug signing key. Если старая установленная версия имеет другую подпись, обновление поверх неё невозможно. Ключ из прежней среды не восстановлен; новая сборка не является побайтовой копией старой. До реализации полного протокола не передавать реальные банковские URL через сеть.
