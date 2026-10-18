# ADR 004 — исследование системного канала Shelter

Статус: ручной переход в рабочий Chrome подтверждён; доставка в мост не проверена. Дата: 2026-10-18.

## Дополнение: пользовательский выбор рабочего Chrome

После эксперимента владелец явно подтвердил: «Страница открылась и загрузилась».
Вместе с ADB-наблюдением Chrome в user 10 это подтверждает ручной маршрут
публичной ссылки из основного пространства в браузер Shelter в этой попытке.
Автоматическая проверка DOM/адресной строки не выполнена. Следующий текст
фиксирует состояние доказательств до получения подтверждения владельца.

После установки Chrome владельцем в Shelter отправлен из shell user 0 новый
ACTION_SEND text/plain с публичным `https://ozon.ru/`. Затем dumpsys activity
зафиксировал topResumedActivity: **user 10, com.android.chrome,
ChromeTabbedActivity**. Это подтверждает наличие рабочего Chrome на переднем
плане после эксперимента, но не доказывает автоматически, что он получил и
загрузил именно переданный адрес. Точные действия выбора получателя не
зафиксированы автоматизацией: к моменту UI dump resolver уже не был доступен.
UI dump не предоставил читаемых узлов Chrome; получение текста/загрузка сайта
и запуск Ozon не подтверждены. Банковские ссылки не использовались.

Отдельная более ранняя попытка ACTION_VIEW BROWSABLE `https://ozon.ru/`
из user 0 открыла Samsung Internet в user 0. Следовательно, обычное открытие
HTTPS и пользовательский маршрут выбора рабочего браузера различаются.

Следующие необходимые доказательства: такой же SEND из обычного приложения (не shell), а
для моста — получение bounded text в рабочей MainActivity без автоматического
запуска внешнего обработчика. Доступность обратного направления не установлена.

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

## Отдельный эксперимент Secure Folder — подтверждение владельца

Владелец сообщил, что полноценное меню «Поделиться» браузера показывает
Secure Folder, в отличие от окна resolver, открывавшегося простым shell
ACTION_SEND. Предложен пользовательский выбор Secure Folder и браузера внутри
для публичного `https://ozon.ru/`. ADB затем зафиксировал Chrome в user 150,
launchedFromPackage=com.samsung.knox.securefolder, с недавней видимостью окна.
На момент проверки Chrome уже STOPPED, на переднем плане браузер user 0.
Владелец явно подтвердил, что именно страница ozon.ru внутри Knox открылась
и загрузилась. DOM и адресная строка автоматизированно не проверены.

Итог: ручной маршрут через пользовательское меню в браузер Secure Folder
подтверждён на этом устройстве сочетанием ADB и наблюдения владельца.
Не подтверждены: такое же меню из UID моста через Intent.createChooser,
доставка в экземпляр моста внутри Secure Folder (он не был установлен при
инвентаризации), запуск приложения Ozon, обратное направление, фон и
автоматический выбор профиля/приложения. Shell resolver не эквивалентен
полноценному Sharesheet, отсутствие Knox в первом не означает запрет второго.
Доказательства Shelter и Secure Folder не объединяются в единый API-контракт.
