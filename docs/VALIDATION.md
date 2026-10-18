# Проверки восстановленной версии

## Текущая итерация 2026-10-18 — 0.1.2-diagnostics

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
