# CI/CD

Пайплайн запускается на **push в `main`**, на **PR/MR** и на **теги `v*`**. Стадии одинаковы на обеих
платформах: `lint → test → build → security → release`.

| Стадия | Что делает | Команда |
|---|---|---|
| lint | формат + линтер | `./mvnw spotless:check checkstyle:check` |
| test | unit (surefire) + integration (failsafe, Testcontainers) + JaCoCo 70% | `./mvnw verify` |
| build | исполняемый jar | `./mvnw -DskipTests package` |
| security | SAST + CVE в зависимостях | `spotbugs:check`, `-Psecurity verify`, CodeQL (GitHub) |
| release | публикация jar на теге | `package` + публикация артефакта |

## GitHub Actions — `.github/workflows/ci.yml`

- `actions/setup-java@v4` с `cache: maven` — JDK 21 (Temurin) и кэш `.m2`.
- Testcontainers работает «из коробки»: на `ubuntu-latest` Docker предустановлен.
- Артефакты (`actions/upload-artifact@v4`): `jacoco-report`, `test-reports`, `notes-service-jar`,
  `dependency-check-report`.
- Отдельные job: `security-spotbugs`, `security-owasp`, `codeql`.
- `release` — только при `refs/tags/v*` (`softprops/action-gh-release`).

## GitLab CI/CD — `.gitlab-ci.yml`

- Образ `maven:3.9-eclipse-temurin-21` (JDK 21 + Maven сразу).
- Кэш каталога `.m2/repository` по ключу `$CI_COMMIT_REF_SLUG`.
- Для Testcontainers — сервис `docker:24-dind` (Docker-in-Docker) + `TESTCONTAINERS_HOST_OVERRIDE=docker`.
- `coverage` через regex по выводу JaCoCo; `artifacts:reports:junit` для отчётов о тестах.
- Стадия `release` — `rules: $CI_COMMIT_TAG =~ /^v/`.

## Артефакты

| Артефакт | Путь |
|---|---|
| Отчёт покрытия | `target/site/jacoco/` |
| Отчёты тестов | `target/surefire-reports/`, `target/failsafe-reports/` |
| Исполняемый jar | `target/notes-service.jar` |
| OWASP-отчёт | `target/dependency-check-report.html|json` |
