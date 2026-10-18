# Совместимость

Ни один Android/Samsung не подключён. Со слов владельца основное устройство Android 17 / One UI 9, парк Android 11–17 и One UI 8.5. Это целевые конфигурации, не доказанная совместимость.

| Пространство | Loopback/TLS | AndroidKeyStore TLS | Локальный запуск | Блокировка/фон |
| --- | --- | --- | --- | --- |
| Work profile | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED |
| Secure Folder | NOT_TESTED | NOT_TESTED | NOT_TESTED | NOT_TESTED |

Карточка теста: model, release/API, build fingerprint, One UI, security patch, app version, targetSdk, контейнер/DPC, VPN, battery restrictions, notifications, состояние блокировки, ожидаемый/фактический результат и дата. Без URL/секретов. PASS только с наблюдениями.

Android 17: межпрофильный loopback запрещён по умолчанию для всех targetSdk; соединения внутри одного профиля не затронуты. Поэтому портовый мост не является основным транспортом для Android 17. ACCESS_LOCAL_NETWORK не следует считать разрешением на пересечение границы профилей. TLS не снимает системное ограничение. Источник: [Android 17 behavior changes](https://developer.android.com/about/versions/17/behavior-changes-all#block-cross-profile-loopback-traffic), проверен 2026-10-17.

Для work profile исследовать разрешённые администратором системные cross-profile API. Для Secure Folder доступный публичный путь ещё не подтверждён; блокировку контейнера не обходить. Диагностическая проба фиксирует результат конкретной прошивки, но её успех не доказывает поддержку постоянной доставки.

Оставшиеся гипотезы: поведение OEM loopback на старых версиях, работа AndroidKeyStore ключа с Conscrypt TLS, доступность процесса в фоне, отображение уведомлений, Shizuku cross-user и совместимость банковских callback. Проверять отдельно.
