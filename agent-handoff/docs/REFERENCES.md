# Официальные источники и skills

Проверка страниц Android 17 behavior changes и CrossProfileApps при подготовке комплекта: 2026-10-18. Остальные ссылки — отправные точки для обязательного изучения агентом перед реализацией соответствующего API. Версии документации, SDK и skills фиксировать в своём отчёте.

## Ограничения профилей

- [Android 17: behavior changes for all apps](https://developer.android.com/about/versions/17/behavior-changes-all) — раздел Block cross profile loopback traffic: запрет по умолчанию, все targetSdk, loopback внутри профиля не затронут.
- [CrossProfileApps](https://developer.android.com/reference/android/content/pm/CrossProfileApps) — проверять требования каждого overload, а не только существование класса.
- [DevicePolicyManager](https://developer.android.com/reference/android/app/admin/DevicePolicyManager) — cross-profile filters, policies и ограничения profile owner.
- [Samsung Knox documentation](https://docs.samsungknox.com/) — искать точный продукт и публичные API; доступность enterprise API не означает доступность обычному APK.

## Kotlin / Android

- [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Android coroutines](https://developer.android.com/kotlin/coroutines)
- [Compose state](https://developer.android.com/develop/ui/compose/state)
- [AndroidKeyStore](https://developer.android.com/privacy-and-security/keystore)
- [KeyGenParameterSpec.Builder](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder)
- [Network Security Configuration](https://developer.android.com/privacy-and-security/security-config) — не считать её автоматической защитой custom raw SSLSocket TrustManager.
- [Intent redirection](https://developer.android.com/privacy-and-security/risks/intent-redirection)
- [Background activity starts](https://developer.android.com/guide/components/activities/background-starts)
- [Foreground services](https://developer.android.com/develop/background-work/services/fgs)
- [App links](https://developer.android.com/training/app-links)
- [16 KiB page sizes](https://developer.android.com/guide/practices/page-sizes)
- [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html)

## Skills для агента

Пользователь просит применять skills при Kotlin-разработке. Найти доступные в реальной среде навыки Kotlin/Android, прочитать инструкции. Не устанавливать случайный skill с непроверенным install script. Если подходящего навыка нет — отметить это и использовать официальные руководства, а не объявлять несуществующий skill применённым.

В прежней работе ссылались на Google Android security skills с commit `5bda39f154fb859a08d45e2392f3568e049f83fb`:

- [android-intent-security SKILL.md](https://github.com/android/skills/blob/5bda39f154fb859a08d45e2392f3568e049f83fb/security/android-intent-security/SKILL.md)
- [android-permissions-security SKILL.md](https://github.com/android/skills/blob/5bda39f154fb859a08d45e2392f3568e049f83fb/security/android-permissions-security/SKILL.md)

Это исторические ссылки, не утверждение об их доступности или применении в новой среде. Если URL/commit недоступен, не выдумывать содержимое. До применения проверить источник и прочитать инструкции. Skill не заменяет аудит, разрешения платформы или physical-device test.
