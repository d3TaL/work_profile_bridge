# Проверки восстановленной версии

## 0.3.0-setup-intro — 2026-10-19

- Первый UX-этап: SetupScreen на первом launcher запуске, акцент текущей роли,
  число видимых копий (без утверждения об их типе/праве передачи), инструкции
  клонирования и Samsung Secure Folder, штатный запрос межпрофильного доступа.
  Повторное открытие из главного экрана. Полный roadmap: UX_ROADMAP.md.
- APK commit `c26af98`; CI https://github.com/d3TaL/work_profile_bridge/actions/runs/39238861113 — SUCCESS.
- Скачанные XML: 31 tests, 0 failures/errors/skipped; Debug/Release lint 0 issues.
  Assemble Debug/Release PASS, strict verification, Release R8.
- APK `/storage/emulated/0/Download/artifacts/profile-bridge-0.3.0-setup-intro-c26af98.apk`.
- SHA-256 `92421c775dce55bc526061a858881f800fe74891351ddbe4470b6e53ff8116e8`.
  Оригиналы `run-39238861113/profile-bridge-apks-21/`, оба SHA256SUMS OK.
- apksig 9.4.1 verified=true errors=[]; signer SHA-256
  `d97db84fe63ef9732e82896fcb832f59ab3e12bf8a57903659b155ed1aa48c3a`.
  Подпись временная отличается от прежней. Установка/удаление агентом не выполнялись.
- Device проверки ожидаются: первый launcher в каждой копии, правильная карточка
  роли, обновление после Android settings, повторное открытие и rotation мастера,
  входящая SEND/VIEW без блокировки введением. После завершения вводный экран
  не показывается автоматически; это не подтверждает готовность маршрута.
- Следующая порция: экран передачи приложения/назначения под системным меню.
  Банковская доставка в этом выпуске не добавлена и не проверялась.

## 0.2.3-share-route — 2026-10-19

- Владелец подтвердил на 0.2.2: Share → Bridge в Knox → Ozon открывается.
- Добавлен заранее выбираемый share-route вместо прямой автопередачи,
  ручная share кнопка и untrusted SHARE_ONCE для подавления повторного chooser.
- APK commit `8b65065`; CI https://github.com/d3TaL/work_profile_bridge/actions/runs/39211343257 — SUCCESS.
- Скачанные XML: 31 JVM tests, 0 failures/errors/skipped; lint Debug/Release
  0 issues. Assemble Debug/Release PASS, strict verification, Release R8.
- APK `/storage/emulated/0/Download/artifacts/profile-bridge-0.2.3-share-route-8b65065.apk`.
- SHA-256 `a93f58b22b545c188614a31c8f10030d5ceb43a92c9c67d0b73b10518befa699`.
  Оригиналы `run-39211343257/profile-bridge-apks-20/`, оба SHA256SUMS OK.
- apksig 9.4.1 verified=true errors=[]; signer SHA-256
  `68c94ed9fecc14ee791418a6f6dd390f7a191be85a76ef9b6e3c40403b05fd20`.
  Подпись отличается от предыдущей, установку/удаление агент не выполнял.
- Device сценарий ожидается: основной auto-forward ON + share-route ON,
  public share auto-open OFF; Knox public share auto-open ON. Входящая
  ссылка в основном мосте открывает chooser; пользователь выбирает Knox Bridge,
  затем Ozon. Compact по желанию. Отмена/возврат не должны запускать chooser
  повторно. Выбор внешнего Bridge должен остаться ручным при сохранённом marker.
- Android/Samsung может удалять extras при межпрофильном share; сохранение
  SHARE_ONCE и реальное подавление re-share требуют проверки. Выбор целевой
  копии автоматически не выполняется; сам chooser не является ACK доставки.

## 0.2.2-knox-share — 2026-10-19

- Владелец подтвердил системный Sharesheet → Bridge внутри Secure Folder:
  ссылка доходит на 0.2.1, но public entry автоматически магазин не открывает.
- Добавлен independent auto-open-share opt-in (default false). Только public
  SEND, local package-scoped shop VIEW, без автоforward; protected entry
  по-прежнему требует ticket и protected opt-in. Подробнее DOMAIN_REGISTRY.md.
- APK commit `8af1447`; CI https://github.com/d3TaL/work_profile_bridge/actions/runs/39210109410 — SUCCESS.
- Скачанные XML: 31 JVM tests, 0 failures/errors/skipped; lint Debug/Release
  0 issues. Assemble Debug/Release PASS, strict verification, Release R8.
- APK `/storage/emulated/0/Download/artifacts/profile-bridge-0.2.2-knox-share-8af1447.apk`.
- SHA-256 `4aa21327c90eba6c5cb1a47c6aa4026fb29df53380b4d4a0dcee63cdc12aa4e7`.
  Оригиналы `run-39210109410/profile-bridge-apks-19/`, оба SHA256SUMS OK.
- apksig 9.4.1 verified=true errors=[]; signer SHA-256
  `02981f4b85bff545f43627da22b61f171b5938bff6b0fb3748b964216815b5b1`.
  Подпись отличается от 0.2.1, требуется решение владельца о переустановке
  только Bridge. Установка агентом не выполнялась, политики не менялись.
- Device test ожидается: внутри Knox включить auto-open-share, по желанию
  compact и debug, выйти Назад, снаружи Share → Secure Folder Bridge.
  Ожидается PUBLIC_SHARE_AUTO_OPEN_PENDING → AUTO_OPEN_ATTEMPT → результат.
  Проверить OFF/manual, повтор cold/warm, отсутствие магазина, rotation.
  Фактическое открытие Ozon этой версией пока не подтверждено.

## Knox: проверка разрешений через ADB — 2026-10-19

Владелец подтвердил работоспособность 0.2.1, сообщил: копии видны из Secure
Folder, но передача отклоняется; тумблер межпрофильного доступа включён.
ADB сопряжён, выполнены только read-only pm/appops/dumpsys запросы.
Адрес подключения и pairing credentials в репозиторий не записываются.

- Установлена 0.2.1 (versionCode 12) у users 0, 11 (Shelter), 150 (Secure Folder).
- AppOps INTERACT_ACROSS_PROFILES: users 0 и 11 — uid mode allow;
  user 150 — No operations, default mode default (не explicit allow).
- Profile owner user 11 — net.typeblog.shelter, owner user 150 —
  com.samsung.knox.securefolder. У администратора Shelter наблюдается
  mCrossProfilePackages=[dev.profilebridge.app]; у Secure Folder — [].
- Это подтверждает различие политик/эффективного AppOp между контейнерами.
  Тумблер в настройках нельзя считать доказательством разрешения для uid 150.
  Точное место отказа внутри APK (precheck vs startActivity exception) этим
  не установлено; требуется разделение диагностических кодов или новый отчёт.
- Права через shell не выдавались, DPC/Knox/установки не менялись. Прямой
  CrossProfileApps путь из Knox не подтверждён. Следующий эксперимент —
  штатный Sharesheet в Bridge внутри папки с публичной ссылкой и ручным открытием.

## 0.2.1-profile-roles — 2026-10-19

- APK commit `1a1d28c`; CI https://github.com/d3TaL/work_profile_bridge/actions/runs/39199333153 — SUCCESS.
- Скачанные XML: 30 tests, failures/errors/skipped = 0; Debug/Release lint 0 issues.
  Assemble Debug/Release PASS, strict verification, Release R8.
- APK `/storage/emulated/0/Download/artifacts/profile-bridge-0.2.1-profile-roles-1a1d28c.apk`.
- SHA-256 `dc60436b67642124af1759ddc990ff387830ed2a7f76d1ef5b08d8b5999b34af`.
  Оригиналы `run-39199333153/profile-bridge-apks-18/`, оба SHA256SUMS OK.
- apksig 9.4.1 verified=true errors=[]; signer SHA-256
  `c1714d7e4809d573262fd07688b93dbc55478014c7b7bb26ca2bc4e3d551e0be`.
  Подпись отличается от 0.2.0; переустановку Bridge решает владелец, агент
  ничего не устанавливал/удалял. Один APK для обеих копий, доступ повторно настроить.
- MAIN/MANAGED role setup и translucent transit с сохранением UI при отказе
  скомпилированы, но физически пока не испытаны. Владелец сообщил о мерцании
  прежней версии, где меню рисовалось до открытия Ozon.
- Device сценарий: launcher обеих копий показывает роль; main auto-forward ON,
  managed auto-open ON; единственный доступный target выбран. Выключение вручную
  должно сохраняться после relaunch. OTHER/UNKNOWN настроить вручную, это не
  точное определение Secure Folder. Compact включить в обеих, выйти Назад,
  передать ссылку, оценить мерцание/открытие Ozon. При отказе — Report.txt.
- Подавление preview, прозрачное окно и отсутствие Compose на автоматическом
  пути уменьшают собственную отрисовку, но не гарантируют отсутствие системного
  перехода/анимации или успешный background Activity launch на Samsung.

## 0.2.0-auto-forward — 2026-10-19

- Commit APK `466a663`; CI https://github.com/d3TaL/work_profile_bridge/actions/runs/39198098939 — SUCCESS.
- Скачанные XML: 30 JVM tests, 0 failures/errors/skipped.
- Debug/Release lint PASS: 0 errors/warnings, по 1 Hint AutoboxingStateCreation
  (mutableStateOf<Long> для serial; не подавлен). Debug/Release assemble PASS,
  strict dependency verification, Release R8.
- APK `/storage/emulated/0/Download/artifacts/profile-bridge-0.2.0-auto-forward-466a663.apk`.
- SHA-256 `55ec03abe22089212ec6b4b7f042bf4d7789f86c05d21d770b5cdfbe594bb90a`.
- Оригиналы `run-39198098939/profile-bridge-apks-17/`, оба SHA256SUMS OK.
  apksig 9.4.1 verified=true errors=[]; signer SHA-256
  `e1189627c277b23655b6f8e854e79fb57ee41d860eb780431e4cbd8982a6e928`.
  Подпись временная, отличается от 0.1.9; обновление поверх прежней установки
  невозможно без подходящего старого ключа. Установку/удаление агент не выполнял.
- Auto-forward, auto-open, compact по умолчанию false. Реестр и контракт:
  [DOMAIN_REGISTRY.md](DOMAIN_REGISTRY.md). 21 exact домен Ozon из скриншотов;
  checkout/test — manual, пути коротких ссылок не привязаны к /t/.
- Новый device test ещё не выполнен: тот же APK в обеих копиях, доступ Android,
  основной: явный выбор копии + auto-forward; рабочий: auto-open; compact в обеих
  по желанию. Share ссылки в основной Bridge должен один раз передать её,
  рабочая копия — открыть Ozon. Compact испытывать после нового cold launch.
- Отдельно ожидаются: тумблеры OFF (ручной режим), paused target, отсутствующий
  магазин, rotation, warm launch, обратное направление с auto-forward в обеих
  копиях (protected receiver не пересылает обратно). Полноценный невидимый
  background service не реализован: обычная dialog Activity, без FGS.

## 0.1.9-ozon-short-links — 2026-10-19

- Владелец подтвердил: Report.txt от 0.1.8 получен в рабочей копии после
  внутренней передачи; доставка работает. PUBLIC_ENTRY здесь выбран самим
  sender из-за неподдержанного короткого /t/ адреса, а не внешним share.
  Прежний вывод о внешнем источнике по одному PUBLIC_ENTRY был неверным.
- Добавлена поддержка exact ozon.ru/t/[A-Za-z0-9]+ без query/fragment.
  Конечное назначение непрозрачно, редиректы мостом не запрашиваются.
- APK commit `b13d65d`; CI https://github.com/d3TaL/work_profile_bridge/actions/runs/39196913209 — SUCCESS.
- По скачанным XML: 29 tests, 0 failures/errors/skipped; Debug/Release lint — 0 issues.
  Debug/Release assemble PASS, strict verification, Release R8.
- APK `/storage/emulated/0/Download/artifacts/profile-bridge-0.1.9-ozon-short-links-b13d65d.apk`.
- SHA-256 `36d940f33c695316bf38500a0f4887a1d8c155e4a08e044c0139a09d12f20331`.
  Оригиналы `run-39196913209/profile-bridge-apks-16/`, оба SHA256SUMS — OK.
- apksig 9.4.1 verified=true errors=[]; signer SHA-256
  `2b4d21786fba7b1316781d62674b97590811f90b4910a8325e234213d29d3ff1`.
  Отличается от 0.1.8: требуется согласованная владельцем переустановка Bridge,
  не Shelter или профиля; все копии одним APK. Установка агентом не выполнялась.
- Проверка телефона ожидается: debug обеих копий, auto-open рабочей,
  внутренняя передача короткого адреса; ожидаемые события PROTECTED_ENTRY,
  AUTO_OPEN_PENDING, SHOP_ROUTE_SUPPORTED, AUTO_OPEN_ATTEMPT и результат запуска.
  ACTIVITY_REQUEST_SUBMITTED не доказывает отображение страницы внутри Ozon.

## 0.1.8-routing-debug

### Результаты CI 2026-10-19

- APK commit `c6107a1`; CI https://github.com/d3TaL/work_profile_bridge/actions/runs/39195461107 — SUCCESS.
- Скачанные XML: 28 JVM tests, failures/errors/skipped = 0; lint Debug/Release — 0 issues.
- Assemble Debug/Release PASS, strict dependency verification; release с R8.
- Первый run `39194941509` остановился на UnusedResources после удаления TLS UI;
  неиспользуемые строки удалены без подавления lint.
- APK: `/storage/emulated/0/Download/artifacts/profile-bridge-0.1.8-routing-debug-c6107a1.apk`.
- SHA-256: `ef3634ba9b70dcc2f47c6fa2ee38930c4a851231430902e0496a09807704fed4`.
- Оригиналы в `run-39195461107/profile-bridge-apks-15/`; оба SHA256SUMS — OK.
- apksig 9.4.1: verified=true, errors=[]; signer SHA-256
  `8463447978e39887b6f1b1a79788b02e81b33052f716f34c131749eca739f019`.
  Он отличается от 0.1.7: обычное обновление поверх неё не поддерживается.
  Переустановка требует отдельного решения владельца; профиль/DPC не удалять.
- Автооткрытие Ozon на устройстве ещё не проверено. Включить debug в обеих
  копиях и auto-open у получателя, отправить обычную /product/ ссылку.
  Если не открылось — открыть отчёт рабочей копии, посмотреть и поделиться.

TLS меню удалено из BridgeScreen. Добавлен пользовательский toggle отладки
и bounded 64-event in-memory журнал только фиксированных кодов: вход,
допуск delivery, магазинный маршрут, попытка открытия, результат startActivity.
URL, token, ID доставки и произвольные сообщения исключений не экспортируются.
Boolean debug сохраняется отдельно, журнал очищается при выключении/process death.

Найден дефект запуска 0.1.7: onPostResume может вызываться до перевода
LifecycleRegistry в RESUMED; pending auto-open оставался без следующего вызова.
Теперь открытие планируется через lifecycle.withResumed для cold/warm запуска.
Это исправление наблюдаемой возможности отказа, не доказательство причины
конкретного устройства без нового отчёта. Пакет Ozon ru.ozon.app.android сохранён.

MCP обсуждён, но не реализован: пока не определены транспорт, адрес сервера,
проверка TLS и авторизация. Встроенный debug toggle не является MCP-коннектором.
Нельзя считать localhost доступным между профилями или обещать remote debug
без канала. Новый APK требует CI и device-проверки.

## 0.1.7 — автооткрытие магазинов у получателя

### Подтверждённая сборка 2026-10-19

- APK commit `6db8859` (автор/коммитер d3TaL).
- CI https://github.com/d3TaL/work_profile_bridge/actions/runs/39193546642 — SUCCESS.
- 28 JVM tests, 0 failures/errors по скачанным XML отчётам.
- Debug/Release lint и assemble PASS, dependency verification strict, Release R8.
- Предыдущий CI `39170825191` остановился на AppLinkWarning. Исправлено явным
  autoVerify=false: мост не заявляет подтверждённое владение доменами магазинов.
- APK `/storage/emulated/0/Download/artifacts/profile-bridge-0.1.7-store-links-6db8859.apk`.
- SHA-256 `2e606b774630f6ecee85dcbba7f5d56b0ad9286f76d660940454a965d685883a`.
- Оригинальные артефакты: `run-39193546642/profile-bridge-apks-12/`;
  SHA256SUMS обоих APK проверены: OK. apksig 9.4.1: verified=true, errors=[].
- CI debug сертификат `f3bdee6233846344d085d7c6c43a90a41f0f0ca291e61e967e13acea13d7aef2`.
  Debug signing по-прежнему временный: обновление поверх прежних версий может
  быть отклонено. Все копии должны использовать один и тот же APK этой сборки.
- ADB 2026-10-19: подключённых устройств нет. Установка и автоматическое
  открытие реальных магазинов на телефоне этой версией не проверены.

Владелец сообщил, что доступ включён в стандартном Shelter без форка, и что
магазины находятся только в рабочем профиле. Это целевой сценарий реализации;
расположение не выдаётся за полную ADB-инвентаризацию текущего момента.
При подготовке ADB ранее видел Ozon в user 150, а Wildberries по фиксированному
пакету не находил; владелец затем уточнил текущую конфигурацию. Установка
магазинов и маршруты должны быть проверены в свежем device test.

Добавлены: public VIEW shop filters, share parser для одного product URL в тексте,
отдельный signature-protected endpoint между копиями, default-off auto-open
у получателя, TTL 30 с и bounded in-memory дедупликация. Пакеты магазинных
обработчиков проверяются только локально у получателя, не у отправителя.
Публичный вход не включает auto-open, входящий Intent никогда не ретранслируется.
При rotation/process restore payload не восстанавливается и не запускается вновь.

Новые JVM проверки: lookalike domains/account/payment paths, share ambiguity,
сохранение raw query/fragment, TTL/future/replay. Android сборка/тесты/lint
запускаются только GitHub Actions, результат и APK будут записаны после CI.
Device IPC signature enforcement, cold/warm/rotation и фактическое открытие
магазинов новой версией пока не проверены. См. STORE_LINKS.md.

## Коррекция исследования Shelter и отдельный форк

При полном изучении upstream Shelter обнаружен существующий пункт контекстного
меню рабочей копии приложения **«Разрешить работу между профилями»**
(AppListFragment, MENU_ITEM_ALLOW_CROSS_PROFILE_INTERACTION). Он вызывает
setCrossProfilePackages. Прежнее утверждение об отсутствии такого UI ошибочно.
Сначала проверить его в установленном Shelter для рабочей копии Profile Bridge,
затем отдельно выдать user consent в системных настройках моста.

По поручению владельца подготовлен отдельный форк
https://github.com/d3TaL/shelter-profile-bridge в
`/root/projects/shelter-profile-bridge`, commit APK 95b591e. Он добавляет
понятный переключатель fixed-package allowlist с readback и сохранением
остальных записей, отзывом, проверкой Binder caller package/signer.
CI 39169534585 PASS: тесты, lint 0 ошибок/77 warnings upstream, Debug/Release.
APK `Download/artifacts/shelter-profile-bridge-95b591e-test.apk`;
SHA-256 7d8522f09f13dfaf6290155e7ed939be71227fd2a49593c54395f735f0d25617.
Подпись тестового APK проверена, отличается от установленного Shelter.
Установленный DPC и рабочий профиль не удалялись, форк не устанавливался.
Дальнейшие сборки по указанию владельца — только GitHub Actions.

## 0.1.6 — передача ссылки CrossProfileApps

### Сборка и установка

- APK commit `e79eeb2` (код функции `08f5945`), автор/коммитер d3TaL.
- CI https://github.com/d3TaL/work_profile_bridge/actions/runs/39167404563 — SUCCESS:
  core:test, assembleDebug/Release, lintDebug/Release, strict verification PASS.
  Локальный core:test UP-TO-DATE (24 существующих теста); Android IPC device
  tests не выполнялись.
- APK `/storage/emulated/0/Download/artifacts/profile-bridge-0.1.6-cross-profile-link-e79eeb2.apk`.
- SHA-256 `ccbc5d4ab3d4213c0dbdabad770b41468de93c3fb89e73650aace34c13f6b591`.
- Оригиналы `run-39167404563/profile-bridge-apks-9/`; checksum Debug/Release OK.
- По явному поручению владельца удалены старые установки пакета через
  `adb uninstall dev.profilebridge.app` (SUCCESS), данные/identity удаляются.
  До удаления installed=true только user 0, users 10/150 уже installed=false.
- `adb install --user 0` — SUCCESS. Холодный старт MainActivity — PASS,
  dumpsys подтверждает 0.1.6/versionCode 7 в основном профиле.
- `install-existing --user 10` и `--user 150` — SecurityException: shell
  не имеет доступа к этим users. Политики не менялись. На последней проверке
  installed=false в users 10/150. Для завершения установки владелец должен
  клонировать новую основную копию через Shelter и «Добавить приложения» Knox.
- Межпрофильная передача 0.1.6 не проверена: копии ещё не установлены,
  право взаимодействия не выдано. После клонирования проверить настройки
  доступа; отказ allowlist является допустимым результатом эксперимента.

Первая CI `39167086778`: Kotlin/assembleDebug PASS; lint ProtectedPermissions
отклонил manifest permission. Проверено на телефоне: protection=signature|appop,
что согласуется с public CrossProfileApps Settings consent flow. Добавлено
точечное tools:ignore только на эту uses-permission с пояснением. Общего
baseline/отключения lint нет; фактические access checks сохранены.

Владелец подтвердил работающую кнопку открытия копии и обратное ручное share
из браузера Knox в основное пространство. Для открытия копии точный target
не зафиксирован автоматизацией, не выдавать за подтверждение Secure Folder.
Повторная ADB проверка 0.1.5: INTERACT_ACROSS_PROFILES denied в users 0/10,
app-op default. Это исходное состояние до добавления permission в manifest.

0.1.6 добавляет новый явный ACTION_SEND text/plain в MainActivity своего
пакета через CrossProfileApps.startActivity(Intent, UserHandle, Activity).
Проверка canInteractAcrossProfiles и membership target выполняется перед
каждой операцией; настройки открываются отдельно по явному действию через
createRequestInteractAcrossProfilesIntent. canRequest не означает DPC allowlist
или гарантированную выдачу доступа. Без разрешения наблюдаемый ACCESS_DENIED,
без ADB grants или изменения политик Shelter/Knox.

IncomingLinkReader/IntentSanitizer общий для onCreate/onNewIntent; полученный
bounded text независимо проходит SafeLink.parse до показа. Автозапуска нет.
На отправке новый Intent содержит только component, type и EXTRA_TEXT;
исходные флаги/ClipData/URI grants не пересылаются. Успех framework вызова
не является ACK; результат помечен UNKNOWN_OUTCOME, retries отсутствуют.
Тестировать только публичные синтетические HTTPS адреса, не банковские токены.

На дату подготовки изменения физическая доставка именно через API не
подтверждена. Для Shelter требуется DPC/OEM allowlist; смена DPC/его политик
не выполняется. Secure Folder не обещается: нужны targetUserProfiles и доступ.

Прочитан android-permissions-security, Google android/skills commit
5bda39f154fb859a08d45e2392f3568e049f83fb (last-updated 2026-09-25),
применён к динамической проверке прав и обработке SecurityException.
android-intent-security того же commit прочитан ранее в этой сессии и
применён к новому explicit Intent без пересылки исходного и общему приёмнику.
См. SECURITY_ALIGNMENT.md, фактические проверки сборки фиксируются после CI.

## Подтверждённый ручной переход в браузер Secure Folder

После пользовательского выбора Secure Folder для публичного `https://ozon.ru/`
ADB зафиксировал Chrome в user 150, запущенный com.samsung.knox.securefolder,
с недавней видимостью. Владелец подтвердил открытие и загрузку этой страницы
внутри Knox. Это подтверждение ручного браузерного маршрута на данном устройстве,
не доставка в мост, не запуск приложения Ozon и не автоматическая доставка.
Полноценное меню браузера отличается от resolver простого shell ACTION_SEND.
Следующий шаг: новый ACTION_SEND через Intent.createChooser из обычного UID
Profile Bridge, затем пользовательский выбор пространства/получателя.
Подробности и ограничения: ADR 004. Изменены только документы, APK не менялся.

## Проверка рабочего Chrome через системный share

Владелец после эксперимента подтвердил открытие и загрузку страницы.
Итог: ручной переход публичной ссылки в Chrome Shelter подтверждён сочетанием
ADB (user 10) и наблюдения владельца. Это не тест localhost/TLS, не доставка
в рабочую копию моста, не запуск Ozon и не проверка обратного направления.
Отправка исходила из shell; app-UID путь ещё требует проверки.

По запросу владельца отправлен новый ACTION_SEND text/plain с публичным
`https://ozon.ru/` из shell user 0. После этого фактически наблюдался
topResumedActivity рабочего Chrome в user 10. Чтение содержимого Chrome через
UI dump не удалось; получение/загрузка конкретного URL не подтверждены.
Исходный отправитель — shell, поэтому это ещё не проверка возможностей
обычного UID моста. Подробнее: ADR 004. Код и APK не менялись.

## ADB-исследование Shelter, 2026-10-18

Беспроводное ADB сопряжение по действию владельца успешно, доступ к телефону
подтверждён. Android 17/API 37, One UI 9, SM-S938B фактически проверены.
Зафиксированы owner Shelter для work profile 10 и отдельный owner Secure Folder
150. Мост 0.1.5 установлен в users 0/10, не в 150.

У ACTION_SEND text/plain из user 0 найден системный forwarder в managed profile.
Доставка в приложение не проверена; shell query в user 10 заблокирован политикой
Shelter no_debugging_features. Прямой INTERACT_ACROSS_PROFILES не предоставлен.
Политики и разрешения не менялись, исходные данные устройства не публиковались.
Подробные факты и ограничения: [ADR 004](adr/004-shelter-system-share-investigation.md).

Изменены только документы исследования. APK остаётся 0.1.5/`291c396`, прежняя
успешная CI и checksum сохраняют силу для того APK; новая сборка не требовалась.
Следующий шаг: пользовательский эксперимент Sharesheet с синтетической ссылкой,
отдельно проверка вызова CrossProfileApps из обычного UID приложения.

## Отчёты устройства 0.1.4 и выпуск 0.1.5 clipboard

Владелец предоставил `Out.txt` (внешняя копия) и `Profile.txt` (внутренняя),
оба Version 0.1.4-keystore-tls. Локальные SELF_CHECK в обеих копиях содержат
PEER_VALIDATE SUCCESS, TLS_HANDSHAKE SUCCESS, TLS_1_3, PROBE_READ/WRITE SUCCESS,
COMPLETE SUCCESS. Серверная сторона self-check также COMPLETE SUCCESS.
Это физическое подтверждение локального pinned TLS ping/ACK в данных копиях,
после добавления DIGEST_NONE; не подтверждение межпрофильного транспорта.

Внешняя CLIENT попытка: TCP_CONNECT STARTED 8ms, FAILURE TIMEOUT 2014ms.
Внутренний ручной listener: LISTEN SUCCESS, TCP_ACCEPT STARTED, без ACCEPT SUCCESS,
остановлен пользователем около 39 секунд. Попытки не имеют общего run ID;
сопоставление по действиям владельца. Причина межпрофильной недостижимости
точно не установлена, TLS handshake для этой CLIENT попытки не начинался.
Сведения совместимы с ограничениями межпрофильного loopback, но сами по себе
не доказывают конкретную ОС-политику, firewall, VPN или состояние контейнера.
Дальнейший этап — разрешённые публичные транспорты (P1), отдельно work profile
и Secure Folder; повторять прежний TLS self-check без новых изменений не нужно.

0.1.5-clipboard: commit `291c396`, CI
https://github.com/d3TaL/work_profile_bridge/actions/runs/39136578854 — SUCCESS.
core:test, Debug/Release build, lintDebug/lintRelease в strict mode — PASS.
Поля TLS теперь обычные текстовые, явные кнопки copy/paste; clipboard sensitive,
чтение только текста по действию, без URI/Intent coercion. Реальный UI clipboard
и межпрофильная политика clipboard пока не проверены.

APK: `/storage/emulated/0/Download/artifacts/profile-bridge-0.1.5-clipboard-291c396.apk`.
SHA-256: `b219735012aec3f6e9cdf471a6455b9c8dc4fe92d5bb32897a12dea56dd76edb`.
Оригиналы: `run-39136578854/profile-bridge-apks-7/`; обе checksum APK — OK.

## 2026-10-18 — 0.1.4-keystore-tls

- APK commit `b8f3251` (d3TaL); CI https://github.com/d3TaL/work_profile_bridge/actions/runs/39132513756 — SUCCESS.
- Локально core:test: 24 теста PASS, strict verification.
- CI core:test, assembleDebug/Release, lintDebug/Release — PASS, strict verification.
- APK `/storage/emulated/0/Download/artifacts/profile-bridge-0.1.4-keystore-tls-b8f3251.apk`.
- SHA-256 `214628719a6d7d81def62e9f0fe47bdedf9a83bbdf1e621ca446b8068f4e2365`.
- Оригиналы: `Download/artifacts/run-39132513756/profile-bridge-apks-6/`;
  checksum Debug/Release проверены: OK. Подпись CI Debug временная.

Получены `Reportout.txt` (внешняя копия) и `Reportprofile.txt` (внутренняя).
В обеих копиях 0.1.3: KEY_SIGN_CHECK PASS, EC выбран, private key и цепочка
запрошены, затем сервер HANDSHAKE_FAILED с causes=[HANDSHAKE,TLS]. Клиенты
self-check: HANDSHAKE_FAILED с causes=[HANDSHAKE,IO]; peer validation отсутствует.
Отдельная внешняя CLIENT попытка: TCP_CONNECT/CONNECTION_FAILED; внутренний
listener не содержит TCP_ACCEPT SUCCESS для межпрофильной попытки. Автоматическая
корреляция двух отчётов отсутствует; точный errno и причина сети неизвестны.

В upstream Conscrypt `CryptoUpcalls.ecSignDigestWithPrivateKey` используется
`NONEwithECDSA`, тогда как прежний AndroidKeyStore spec разрешал SHA256 only.
Источник: https://github.com/google/conscrypt/blob/master/common/src/main/java/org/conscrypt/CryptoUpcalls.java .
Это объясняет, почему SHA256 sign-check не доказывает совместимость TLS, но
не является окончательным доказательством причины Samsung handshake failure.

Новый alias v2 разрешает SHA256 и NONE для EC signing, v1 остаётся нетронутым.
Проверка KEY_RAW_SIGN_CHECK использует NONEwithECDSA на 32 случайных байтах;
данные/подпись обнуляются и не экспортируются. Provider штатный, pin и TLS 1.3
сохранены. Ключ подписи APK не изменяется кодом приложения.

Следующий тест: по одному self-check в обеих копиях и экспорт отчётов. Не
повторять межпрофильный тест до подтверждения локального TLS. Физический
результат версии 0.1.4 пока не проверен.

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
