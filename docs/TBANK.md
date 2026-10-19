# Первый банковский маршрут

## 0.3.7 — схема оплаты

Read-only ADB фиксировал ACTION_VIEW bank100000000004 из
by.advasoft.android.troika.app после выбора Т-Банка владельцем. Полные data
не выводились/не сохранялись. Это не сверка manifest установленной банковской
копии: runtime package-scoped query проверит обработку у получателя.

SafeLink.parse остаётся HTTPS-only. Явный parseRouting принимает дополнительно
только bank100000000004 hierarchical URI с host, без userinfo/port/fragment.
Path/query сохраняются raw, не трактуются как проверенные реквизиты платежа.
Размер/control/Unicode ограничения сохраняются. Сбер и intent:// не поддержаны.
Manifest отдельный VIEW filter. SEND только полная банковская ссылка.
Передача новым text/plain SEND, local запуск новым package-scoped VIEW после
жеста и RESUMED. BankLinks исключён из StoreLinks.target, автоматика не применяется.
URL не попадает в TLS/report/state/retry. Реальный платёж и возврат не проверены.

Т-Банк package `com.idamob.tinkoff.android`: карточка АО «ТБанк»
https://www.rustore.ru/catalog/app/com.idamob.tinkoff.android проверена 2026-10-19.
ADB отключён: пакет установленной копии и подпись банка на устройстве не сверены.
Hosts со скриншота владельца: www.ob-tb.ru, t-calls.ru, id.srv-hub.org,
ipa.srv-hub.org, b.tbao.su, www.tbank.ru, tbank.ru, tt.ru.
Это декларации установленного приложения, не доказательство платёжного назначения.

BankLinks отдельно от магазинной auto-open политики. Все hosts exact,
HTTPS/SafeLink, raw query/fragment сохраняются. CONFIRM по умолчанию;
AUTOMATIC из группового действия нормализуется в CONFIRM. DISABLED работает.
Защищённый SEND своей копии не открывает банк автоматически. Public share opt-in
тоже не открывает банк. Ручной «Найти обработчики» для известной банковской
ссылки ограничен пакетом Т-Банка; запуск новым package-scoped VIEW в RESUMED.
Это проверка установленного обработчика, не криптографическая аттестация банка.
Для Knox ручной chooser доступен, bank URL не сохраняется в retryLink.
URL не включается в отчёты, saved state, preferences или diagnostic TLS ping.

Проверка владельца: сначала публичный https://tbank.ru/ в Bridge, ручная
передача в нужную копию, «Найти обработчики» → открыть пакет банка.
Затем переход оплаты из магазина до подтверждения платежа: доступен ли Bridge,
доходит ли исходный адрес, показана ли ожидаемая форма, отмена и возврат.
Не присылать токены/реальные URL. Для настоящей оплаты отдельное решение владельца.
Открытие банка не равно оплате. Explicit SDK/custom schemes/Activity results
не поддержаны этим HTTPS маршрутом, callback между профилями не гарантируется.
