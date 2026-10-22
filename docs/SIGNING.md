# Постоянная подпись Profile Bridge

Release alias: `profile-bridge-release`, PKCS12 RSA-3072, validity 10000 days.
Certificate SHA-256:
`11a778c8292ce4404420938c6aaaf4100af042441071a431c1597a5ebfe8fca7`.
Автор/владелец d3TaL. Ключ отдельный от Shelter и TLS AndroidKeyStore.

GitHub Secrets: PLB_RELEASE_KEYSTORE_B64, PLB_RELEASE_PASSWORD.
Созданы с отказом перезаписи существующих secrets. Сам ключ/пароли не в git.
CI восстанавливает ключ только в d3TaL/work_profile_bridge, refs/heads/master,
push/workflow_dispatch, никогда pull_request. Ключ временно в RUNNER_TEMP,
mode 600, удаляется always step; артефакты содержат APK/публичный fingerprint,
не signing material. Пароль передаётся только build step environment.

Release остаётся R8/minify/shrink, apksigner verify + exact pinned signer
проверяется перед upload. PR/другие ветки получают unsigned release и обычный
временный debug; использовать для установки владельца только signed release.
Gradle signing config берёт путь и пароль из env; без ключа release unsigned.
Не заменять постоянный ключ вновь сгенерированным при сбое secrets.

Переход с прежних debug APK требует однократной переустановки только Bridge
во всех копиях по решению владельца, с повторной настройкой. Shelter/профили
не удалять. Далее одинаковый release signer, applicationId и растущий versionCode
позволяют Android обновлять поверх с сохранением данных; установки в каждом
контейнере всё равно выполняются штатно, политики Knox не обходятся.

Закрытая резервная копия создаётся вне репозитория и вне папки APK artifacts.
Содержит p12, пароль и публичный сертификат: хранить офлайн в защищённом месте,
не публиковать/не прикладывать к отчётам. Потеря ключа лишит возможности обычных
обновлений. GitHub secret нельзя скачать обратно через API.
