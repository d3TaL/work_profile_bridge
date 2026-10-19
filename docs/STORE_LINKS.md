# Ссылки магазинов: 0.1.7

## Сценарий

Основной профиль: Profile Bridge получает ссылку через «Поделиться»,
«Открыть с помощью» или явный выбор обработчика Android. Пользователь выбирает
«Передать ссылку в копию …». Магазины в основном профиле не нужны.

Рабочий профиль: установлен тот же APK моста и нужный магазин. Включить
«Автооткрытие магазинов в этой копии». Для supported product/root links
отправитель вызывает signature-protected StoreReceiveActivity своей копии
через CrossProfileApps; получатель независимо проверяет текст и выбирает только
фиксированный пакет. Доступ CrossProfileApps должен быть предоставлен через
Shelter allowlist и системное согласие пользователя.

Публичный вход MainActivity сам по себе не автооткрывает магазин. Недоверенный
extra не включает trusted mode; StoreReceiveActivity защищена собственной
signature permission. Состояние preference хранит только boolean, URL в памяти.
Входящие Intent не пересылаются. Новая локальная ACTION_VIEW создаётся заново
без чужих flags, extras, ClipData/grants.

## Разрешённые маршруты автооткрытия

- `ozon.ru`, `www.ozon.ru`, `m.ozon.ru`: `/product/…`, либо корень без
  query/fragment → `ru.ozon.app.android`.
- `wildberries.ru`, `www.wildberries.ru`: `/catalog/<digits>/detail.aspx`,
  либо корень без query/fragment → `com.wildberries.ru`.

Только HTTPS; raw URL сохраняется без преобразования. Shortlinks, arbitrary
subdomains, account/payment paths и другие адреса — ручное открытие.
Редиректы не запрашиваются и не разворачиваются мостом.

Ozon package/hosts подтверждены resolver установленного APK при исследовании;
Wildberries package проверен по официальной карточке Google Play:
https://play.google.com/store/apps/details?id=com.wildberries.ru .
Поддержка конкретной страницы магазином всё равно проверяется на устройстве.

## Жизненный цикл и результат

Открытие только на RESUMED Activity, после согласия получателя и жеста отправки
в другой копии. Pending intent consumed до suspend; при onStop отменяется.
Не воспроизводить входящий Intent при rotation/process restore.
Несекретный message ID и same-device elapsedRealtime timestamp дают TTL 30 с,
bounded replay guard 64 IDs в памяти процесса. После process death память
очищается — это не durable exactly-once протокол, не разрешение банковских действий.
Запуск Activity не подтверждает загрузку товара/покупку. Нет автоматического
повтора при отказе или неясном результате; текст доступен для ручного выбора
если магазин не открылся.

## Ограничения «перехвата»

Intent filters не перехватывают внутреннюю навигацию чужого APK. На Android 12+
подтверждённые App Links и выбор браузера определяются Android/пользователем.
Мост не владеет доменами магазинов и не заявляет autoVerify/Digital Asset Links.
Поэтому система может открыть браузер/магазин без показа моста. Использовать
«Поделиться → Profile Bridge», доступное «Открыть с помощью» или пользовательскую
настройку обработчика. Право владения доменом/verification через ADB не подделывается.

Secure Folder — только если CrossProfileApps действительно предоставляет target
и право передачи. Ранее успешное меню Knox не доказывает такой API-доступ.

## Проверка владельцем

1. Установить одинаковый 0.1.7 APK в основной профиль и Shelter, включить
   allowlist в Shelter и системный переключатель. Магазины — только в Shelter.
2. В рабочем мосте включить автооткрытие магазинов.
3. Из основного приложения поделиться публичной product-ссылкой → Profile Bridge
   → «Передать ссылку в копию …». Убедиться, что открылся магазин с work badge.
4. Повторить со вторым магазином. Отдельно проверить выключенный toggle,
   отсутствие магазина, неподдерживаемый адрес и возврат из фона: повторного
   автоматического открытия быть не должно.

## Security alignment

Применены ранее прочитанные android-intent-security / android-permissions-security,
Google android/skills 5bda39f154fb859a08d45e2392f3568e049f83fb.
Приоритет высокий: ограничить автооткрытие недоверенными Intent и URI grants.
Области: AndroidManifest, StoreReceiveActivity, MainActivity, IncomingLinkReader,
StoreDelivery, ProfileDiagnostics, LocalLinkLauncher и core links/tests.

Ключевые diff:
```diff
+ <permission android:name="${applicationId}.permission.STORE_TRANSFER" android:protectionLevel="signature" />
+ <activity android:name=".StoreReceiveActivity" android:exported="true"
+     android:permission="${applicationId}.permission.STORE_TRANSFER" android:launchMode="singleTop" />
```
```diff
-        val candidates = activity.packageManager.queryIntentActivities(view(link), PackageManager.MATCH_DEFAULT_ONLY)
+        val candidates = activity.packageManager.queryIntentActivities(view(link).setPackage(targetPackage), PackageManager.MATCH_DEFAULT_ONLY)
```
Полный unified diff — git коммит 0.1.7; результаты фактических проверок в VALIDATION.md.
# Короткие ссылки Ozon (0.1.9)

Поддержан точный host `ozon.ru`, raw path `/t/[A-Za-z0-9]+`, без query/fragment.
Исходный адрес передаётся неизменным в `ru.ozon.app.android`; мост не делает
сетевых запросов и не раскрывает редиректы. Это осознанный запуск короткого
адреса в магазине, не проверка конечного назначения как страницы товара:
его содержание неизвестно мосту. Остальные ограничения защищённого входа,
TTL, replay gate, согласия и RESUMED сохраняются.

В 0.1.8 внутренний sender выбирал PUBLIC_ENTRY для неподдержанного `/t/`.
Поэтому PUBLIC_ENTRY в отчёте не доказывает внешний Sharesheet: это также
штатный fallback внутреннего sender. Владелец подтвердил доставку ссылки
в рабочую копию через внутреннюю кнопку, Report.txt показал unsupported route.
# Актуально для 0.2.0

См. [реестр и контракт автоматики](DOMAIN_REGISTRY.md). Он заменяет описанные
ниже ограничения Ozon на /product/ и /t/. Все три режима по умолчанию выключены.
