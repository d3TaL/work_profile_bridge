# ADR 004 — исследование системного канала Shelter

Статус: кандидат, доставка не подтверждена. Дата: 2026-10-18.

## Фактические наблюдения ADB

- Samsung SM-S938B, Android 17, API 37, One UI 9 (`90000`), patch 2026-09-05.
- Основной user 0; managed profile user 10, parent 0, owner Shelter;
  Secure Folder user 150, отдельный Samsung owner. Оба профиля running.
- Мост 0.1.5 (versionCode 6) установлен и не suspended в users 0 и 10;
  в user 150 не установлен. INTERNET granted.
- INTERACT_ACROSS_PROFILES не запрошен manifest и не предоставлен, app-op
  в default. DPC `mCrossProfilePackages` пуст; мост отсутствует в выведенных
  OEM cross-profile allowlists. Это не запрещает специальный launch-only
  `CrossProfileApps.startMainActivity`, но не даёт прямой передачи Intent.
- Shelter устанавливает `no_debugging_features=true` для user 10. Запрос
  `cmd package query-activities --user 10 ...` отклонён: shell не имеет доступа
  к user 10. Политика не изменялась. Автоматизация рабочего профиля ограничена.
- Запрос из shell user 0 для нового ACTION_SEND / text/plain возвращает
  `android/com.android.internal.app.ForwardIntentToManagedProfile` и локальную
  MainActivity моста. Это наблюдение resolver, не подтверждение доставки
  в рабочую копию и не доказательство app-UID package visibility.
- Тот же запрос с `-p dev.profilebridge.app` возвращает только локальную
  MainActivity: package-scoped SEND нельзя считать доказанным маршрутом в work.
- Launcher старт основной копии через shell не является проверкой
  CrossProfileApps из UID приложения. UI-наблюдение не выполнено: при проверке
  активен браузер, экран пользователя не перехватывался повторно.

ADB endpoint, pairing code, device serial и необработанные dumpsys/logcat
в репозиторий не включаются. Не выполнялись изменение app-ops/DPC, выдача
межпрофильных разрешений через shell, установка/удаление приложений, сброс данных,
переходы в банковские приложения или обход Secure Folder.

## Рассмотренные публичные пути

1. `CrossProfileApps.startMainActivity`: открывает launcher Activity своего
   пакета, без передачи payload. Сам по себе не транспорт URL.
2. `CrossProfileApps.startActivity(Intent, ...)`: нужен соответствующий доступ,
   allowlist администратора/OEM и согласие пользователя; целевой компонент
   только своего пакета. Мост не может самостоятельно выдать себе эти права.
3. Системный Sharesheet через новый ACTION_SEND / text/plain: кандидат на
   ручную доставку в рабочую копию через уже разрешённые DPC filters.
   Выбор пространства/получателя делает пользователь. Сначала проверить
   синтетический несекретный HTTPS адрес в обе стороны; не реальные банк-токены.

Для реализации третьего пути исходный входящий Intent не пересылается.
Получатель извлекает только bounded text через текущий IntentSanitizer,
проверяет URL независимо и открывает выбранный локальный пакет только по жесту.
Не добавлять chooser initial-intents/скрытые ForwardIntent компоненты для
принудительного переключения профиля. Пока нет подтверждённой доставки,
не объявлять Sharesheet готовым каналом и не добавлять автоматические повторы.

## Источники

- AOSP CrossProfileApps (публичные методы и их Javadoc):
  https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/content/pm/CrossProfileApps.java
- DevicePolicyManager, cross-profile Intent filters:
  https://developer.android.com/reference/android/app/admin/DevicePolicyManager#addCrossProfileIntentFilter(android.content.ComponentName,android.content.IntentFilter,int)
- Shelter upstream `Utility.enforceWorkProfilePolicies`:
  https://github.com/PeterCxy/Shelter/blob/master/app/src/main/java/net/typeblog/shelter/util/Utility.java

Исходники upstream main/master просмотрены на дату исследования, не сверены
побайтово с установленным Shelter или Samsung framework. В комментариях Shelter
есть риск путаницы направления; источником фактического направления считать
resolver и проверку доставки, а не комментарий.

Secure Folder остаётся отдельным неподтверждённым сценарием. Наличие owner или
профиля running не доказывает доступный канал обычного APK в это пространство.
