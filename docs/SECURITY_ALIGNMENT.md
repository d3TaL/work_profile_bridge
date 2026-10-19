# Best Practices and Security Alignment Update

## 0.1.6: межпрофильное разрешение и минимальная передача

Приоритет высокий: передача непроверенного payload или использование устаревшего
разрешения. Skills: android-intent-security и android-permissions-security,
Google android/skills commit 5bda39f154fb859a08d45e2392f3568e049f83fb.
Версии зависимостей не менялись.

Изменены AndroidManifest.xml, ProfileDiagnostics.kt, MainActivity.kt,
BridgeScreen.kt, strings.xml, app/build.gradle.kts. Разрешение специально
межпрофильное, не INTERACT_ACROSS_USERS; выдача только через штатный Settings.
Повторные проверки прав и target выполняются непосредственно перед send;
SecurityException обработан без exception.message. Получатель остаётся
публичным share entrypoint и не считает разрешение отправителя подтверждением
доверия к ссылке. Открытие локального приложения только после жеста пользователя.

Ключевые diff-фрагменты:

```diff
+    <uses-permission android:name="android.permission.INTERACT_ACROSS_PROFILES" />
```

```diff
+            if (target !in api.targetUserProfiles) return ProfileSendResult.NO_TARGET
+            if (!api.canInteractAcrossProfiles()) return ProfileSendResult.ACCESS_DENIED
+            val outgoing = Intent(Intent.ACTION_SEND).apply {
+                component = ComponentName(activity, MainActivity::class.java)
+                type = "text/plain"
+                putExtra(Intent.EXTRA_TEXT, parsed.link.original)
+            }
+            api.startActivity(outgoing, target, activity)
+            ProfileSendResult.SUBMITTED_UNKNOWN_OUTCOME
```

```diff
-        if (value != null) edit(value)
+        if (value != null) {
+            edit("")
+            if (SafeLink.parse(value) is LinkParseResult.Accepted) {
+                edit(value)
+                message = R.string.link_received
+            } else linkError = R.string.invalid_url
+        }
```

Проверка: core:test, lintDebug/Release, assembleDebug/Release в strict mode.
Device test отдельно: отказ без consent, возврат из настроек без автоотправки,
получение публичного URL другой копией только при реальном доступе.
Результаты — VALIDATION.md; исходный diff целиком доступен в git-коммите выпуска.

Приоритет высокий: TLS peer authentication, входящие Intent и секреты. Применены официальные android-intent-security и android-permissions-security. Сам факт применения skills не является аудитом.

| Область | Риск | Изменения |
| --- | --- | --- |
| TLS | Ложный local listener, downgrade | core/tls: TLS 1.3, SHA-256 pin leaf, certificate validity до отправки token, без fallback |
| Ключи | Экспорт signing key | AndroidTlsIdentity: EC P-256 в AndroidKeyStore, off-main generation, отдельный diagnostic alias |
| Код сессии | Подмена/утечка credentials | PairingCode: pin+random256 token, canonical parsing, bounded size, redacted toString; доверие к перенесённому коду обязательно |
| Приёмник | Команды и бесконечное чтение | TlsProbe: только fixed ping, 127.0.0.1, lifetime 120 секунд, 32 connections, timeouts, active socket close |
| Intent | URI grants, caller spoofing | IncomingLinkReader/MainActivity: allowlist sanitize, одинаково cold/warm, очищенный Intent, не доверяем caller extras |
| Запуск | Неявный пакет/фон | LocalLinkLauncher: новый package-scoped ACTION_VIEW, проверка выбора и RESUMED после async query |
| Privacy | Логи и backup | Redacted types, нет Log/analytics, URL только в памяти, onStop cleanup, backup/transfer disabled, FLAG_SECURE |
| Supply chain | Изменение зависимостей | Fixed versions, wrapper checksum, regenerated lockfiles/verification, immutable Action SHA |

Изменённые области: core/src/main/kotlin/dev/profilebridge/core/{links,tls}, app/src/main/kotlin/dev/profilebridge/app/{platform,ui}, MainActivity, AndroidManifest, res/xml, Gradle files и CI. Это восстановление новых файлов, не diff с найденным старым архивом.

Полный unified diff текущих текстовых исходников относительно пустой базы: [security-recovery.patch](security-recovery.patch). Проверки — [VALIDATION.md](VALIDATION.md).

Риски, которые остаются: подмена out-of-band кода; отсутствие долгосрочного сопряжения/отзыва и mTLS; не проверенный Samsung AndroidKeyStore/Conscrypt путь; отсутствие URL delivery state machine и политики получателя. Локальная диагностика не запускает банк через сеть. Перед настоящими банковскими ссылками требуется отдельный review.
# 0.2.0 — opt-in автоматизация

Прочитан android-intent-security SKILL.md из android/skills commit
5bda39f154fb859a08d45e2392f3568e049f83fb (metadata 2026-09-25).
Область: безопасная маршрутизация Intent, priority high. Новых permissions,
services, providers, nested Intent и зависимостей нет. MainActivity cold/warm
применяет один IncomingLinkReader/IntentSanitizer; исходный Intent не запускается.
ProfileDiagnostics строит новый explicit SEND своей signature-protected копии,
LocalLinkLauncher — новый package-scoped VIEW после проверки RESUMED/обработчика.
Автопередача только opt-in, в сохранённый serial профиля, разово; защищённый
получатель не делает обратный forward. TTL/replay проверяются до auto-open.
URL не пишется в preferences/saved state/report. Доменный реестр точный,
не wildcard; непрозрачные ссылки не выдаются за проверенные страницы товаров.

Scope: MainActivity, BridgeScreen, ProfileDiagnostics, StoreLinks/StoreLinksTest,
AndroidManifest, themes/strings, app version, DOMAIN_REGISTRY и release docs.
Точный unified diff выпуска: `git diff b13d65d..HEAD -- app core`.
Проверки: CI core:test, lintDebug/Release, assembleDebug/Release; device cold/warm,
пауза/исчезновение выбранного профиля, отсутствие магазина, rotation и циклы
ожидают владельца. Компактная Activity не является обходом BAL.
# 0.2.1 — роль и транзитное окно

Те же android-intent-security инструкции commit 5bda39f применены к MainActivity,
ProfileRole, BridgeScreen, manifest/themes. Публичные UserManager API, без Knox API
или новых разрешений. Signature receiver/sanitizer не изменены. Автонастройка
основной/managed копии запрошена владельцем; compact остаётся opt-in. Никакого
service/FGS, incoming Intent forwarding или обхода lifecycle. Полный diff:
`git diff 466a663..HEAD -- app`. Не обещается невидимый запуск: translucent Activity
может влиять на BAL и требует теста на Samsung; при наблюдаемом отказе UI виден.
# 0.2.2 — public share opt-in

android-intent-security commit 5bda39f: расширение public SEND документировано
как явно разрешённый локальный запуск из любого отправителя, не как доверенная
доставка. Приоритет high: разделить границы доверия. Scope MainActivity,
BridgeScreen, strings/version, StoreEntryPolicy и тест. Sanitizer/manifest
permissions неизменны, protected TTL/replay не обходится новым opt-in.
Core policy тестирует invalid protected ticket, opt-out, VIEW и manual route.
Diff: `git diff 1a1d28c..HEAD -- app core`. Реальное Knox автооткрытие ожидает
проверки владельца; AppOps не меняются.
