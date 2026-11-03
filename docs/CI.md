# Сборка и скачивание через GitHub Actions

Workflow `.github/workflows/android.yml` запускается при push, pull request
и вручную через **Actions → Android checks → Run workflow**.
Он выполняет JVM-тесты, lint Debug/Release, собирает Debug и Release с R8
со строгой проверкой зависимостей.

После успешного запуска в разделе **Artifacts** доступны:

- `profile-bridge-apks-<номер>`: устанавливаемый Debug APK, неподписанный
  Release APK, `SHA256SUMS` и `BUILD.txt` с commit/run ID;
- `profile-bridge-reports-<номер>`: отчёты тестов и lint, в том числе доступные
  отчёты при неудачном запуске.

Срок хранения — 7 дней, но обычно запуск удаляется раньше (см. ниже). Для скачивания через сайт нужно войти в GitHub
и иметь доступ к репозиторию. В архивы не включаются ключи подписи.
Debug-ключ на чистом runner создаётся заново: APK разных запусков может
быть несовместим для обновления поверх прежней установки. Постоянная
подпись требует отдельной настройки ключа в GitHub Secrets.
Неподписанный Release APK напрямую не устанавливается.

## GitHub CLI

После размещения проекта в выбранном репозитории и `gh auth login`:

```bash
gh workflow run android.yml --repo OWNER/REPO --ref BRANCH
gh run list --repo OWNER/REPO --workflow android.yml --limit 5
gh run watch RUN_ID --repo OWNER/REPO --exit-status
gh run download RUN_ID --repo OWNER/REPO --pattern 'profile-bridge-apks-*' --dir downloaded-apks
```

При распаковке через сайт проверить `sha256sum -c SHA256SUMS` в папке APK.
Настройка workflow сама по себе не подтверждает успешную удалённую сборку.

## Очистка Actions

`.github/workflows/actions-cleanup.yml` запускается после каждого завершения
`Android checks` или `Dependency metadata`, ежедневно и вручную (`dry_run`
только печатает план). Скрипт `.github/scripts/actions-cleanup.sh` удаляет
завершённые запуски вместе с их логами и артефактами, кроме:

- последнего завершённого запуска каждого workflow в каждой ветке;
- последнего успешного `Android checks` в `master` (текущий APK).

Запуски в очереди и выполняющиеся не трогаются. Поэтому APK, отчёты и
скриншоты нужно скачать до завершения следующего запуска той же ветки.
Ссылки на удалённые run ID в VALIDATION.md перестают открываться; commit и
SHA-256 в VALIDATION.md остаются доказательством. Из Gradle-кэша `master`
сохраняется самая новая запись каждого вида, кэши других веток удаляются:
ветки и pull request только читают кэш.

## Обновление verification metadata

Локальные сборки не выполняются, поэтому для новой или обновлённой
зависимости вручную запускается `Dependency metadata` в нужной ветке:

```bash
gh workflow run dependency-metadata.yml --repo d3TaL/work_profile_bridge --ref BRANCH
```

Артефакт `dependency-metadata-<номер>` содержит patch для
`gradle/verification-metadata.xml` и lockfile-ов. Новые компоненты
проверяются по списку групп/версий перед применением; автоматического
коммита нет. Метаданные создаются по первому скачиванию (SHA-256), как и
исходный файл.
