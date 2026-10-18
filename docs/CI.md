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

Срок хранения — 14 дней. Для скачивания через сайт нужно войти в GitHub
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
