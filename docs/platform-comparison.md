# Исследование: GitHub Actions vs GitLab CI/CD на одном сценарии

Один и тот же проект `notes-service` и один и тот же конвейер
`lint → test → build → security → release` реализованы на обеих платформах
(`.github/workflows/ci.yml` и `.gitlab-ci.yml`). Ниже — наблюдения по пяти осям.

## 1. Настройка CI/CD

| Критерий | GitHub Actions | GitLab CI/CD |
|---|---|---|
| Файлов конфигурации | 1 (`.github/workflows/ci.yml`) | 1 (`.gitlab-ci.yml`) |
| Модель | jobs + steps, переиспользуемые actions из Marketplace | stages + jobs, всё в YAML/скриптах |
| Установка JDK 21 | `actions/setup-java@v4` (`distribution: temurin`, `java-version: 21`) | базовый образ `maven:3.9-eclipse-temurin-21` — JDK уже внутри |
| Кэш `.m2` | `cache: maven` — одна строка, ключ по `pom.xml` автоматически | `cache: { key, paths: [.m2/repository] }` + `-Dmaven.repo.local` вручную |
| Testcontainers / Docker | Docker предустановлен на `ubuntu-latest` — работает «из коробки» | нужен `services: docker:dind` + `DOCKER_HOST`/`TESTCONTAINERS_HOST_OVERRIDE` |

**Вывод оси:** на GitHub поднять JDK + кэш + Testcontainers заметно короче (готовые actions, Docker в раннере).
На GitLab JDK берётся из образа «бесплатно», но Testcontainers требует ручной настройки Docker-in-Docker.

## 2. Code review

| Критерий | GitHub (Pull Request) | GitLab (Merge Request) |
|---|---|---|
| Комментарии к строкам | да, threaded, «Resolve conversation» | да, threaded, «Resolve thread» |
| Предложение правки прямо в ревью | **Suggested changes** (применяется в один клик и коммитится) | suggestions тоже есть (\`\`\`suggestion) |
| Требование «все треды закрыты» | через branch protection / rulesets | встроенная опция «All threads resolved» |
| Привязка к задаче | `Closes #N` в описании PR | `Closes #N` / автоссылки на issue |
| Подтверждения (approvals) | required reviewers / CODEOWNERS | Approval rules (число, роли, CODEOWNERS) |

**Вывод оси:** функционально паритет. GitLab чуть гибче в политике обязательных аппрувов
(approval rules из коробки), GitHub удобнее для быстрых правок (Suggested changes).

## 3. Pipeline: скорость, логи, артефакты

| Критерий | GitHub Actions | GitLab CI/CD |
|---|---|---|
| Параллелизм | jobs параллельно, зависимости через `needs:` | stages последовательны, jobs внутри стадии параллельны |
| Логи | складные группы по шагам, аннотации в diff | потоковые логи на job, секции `collapsible` |
| Артефакты | `actions/upload-artifact` (jar, JaCoCo, отчёты) | `artifacts: paths` + `expire_in` |
| Отчёт о тестах | через сторонние actions или сводку | нативно `artifacts:reports:junit` (видно в MR) |
| Покрытие | бейдж/коммент через сторонние actions | нативный парсинг `coverage:` regex + бейдж |

**Вывод оси:** GitLab сильнее «из коробки» по тест-репортам и покрытию (нативные `reports:junit`,
`coverage:`). GitHub гибче по графу зависимостей задач (`needs`) и богаче экосистемой готовых actions.

## 4. Права и безопасность

| Критерий | GitHub | GitLab |
|---|---|---|
| Защита веток | Branch protection rules / Rulesets | Protected branches + Push rules |
| Обязательный CI перед merge | Required status checks | Pipeline must succeed |
| Обязательный review | Required approvals, CODEOWNERS | Approval rules, CODEOWNERS |
| SAST | **CodeQL** (нативно, бесплатно для публичных репо) + сторонние | GitLab SAST (Semgrep-based) в Ultimate; для любого тарифа — SpotBugs/find-sec-bugs в пайплайне |
| Secret scanning | GitHub Secret Scanning (Advanced Security) | Secret Detection (встроено) |
| SCA / зависимости | Dependabot + OWASP Dependency-Check (наш job) | Dependency Scanning (Ultimate) + OWASP в нашем job |

**Вывод оси:** CodeQL — заметное преимущество GitHub для Java-SAST без доплат на публичных репозиториях.
GitLab выносит часть security-фич (SAST, Dependency/Secret Scanning) в платные тарифы, но базовую защиту
веток и push rules даёт на всех тарифах; в нашем проекте независимость обеспечена тем, что SpotBugs +
find-sec-bugs + OWASP Dependency-Check работают одинаково на обеих платформах.

## 5. Применимость

| Контекст | Рекомендация |
|---|---|
| Учебный проект | **GitHub** — ниже порог входа, Marketplace, бесплатный CodeQL, Docker в раннере |
| Исследовательский | **GitHub** — публичность, бесплатные минуты для open-source, простое подключение |
| Промышленный (self-hosted, регуляторика) | **GitLab** — единая платформа (repo+CI+registry+security), self-managed, гранулярные права |

## Итоговая таблица

| Ось | Победитель | Кратко |
|---|---|---|
| Настройка CI/CD | GitHub (немного) | готовые actions, Docker в раннере; у GitLab JDK из образа |
| Code review | паритет | GitHub: Suggested changes; GitLab: approval rules |
| Pipeline | паритет/GitLab | GitLab: нативные junit-reports и coverage; GitHub: `needs`-граф |
| Безопасность | GitHub | бесплатный CodeQL для Java; у GitLab многое в Ultimate |
| Применимость | по контексту | учеба/исследование → GitHub; enterprise self-hosted → GitLab |

## Вывод

Для **данного** учебно-исследовательского Java-проекта рациональнее **GitHub Actions**: минимальная настройка
JDK/кэша/Testcontainers, бесплатный CodeQL для SAST по Java, обширный Marketplace и предустановленный Docker
в раннере. **GitLab CI/CD** остаётся предпочтительным для промышленного self-hosted сценария с требованиями к
единой платформе (репозиторий + CI + реестр образов + security) и тонкой ролевой модели. Поскольку ключевые
проверки качества и безопасности (spotless, checkstyle, SpotBugs + find-sec-bugs, JaCoCo 70%, OWASP
Dependency-Check) реализованы в Maven, конвейер переносится между платформами почти 1:1 — отличаются лишь
обвязка (установка JDK, кэш, поднятие Docker) и нативные security-фичи.

## Реальные наблюдения из живого эксперимента

Сценарий был выполнен не на бумаге, а фактически: проект целиком поднят на **GitHub** и
зеркалирован на **GitLab**.

### GitHub — полный живой прогон
- Репозиторий `VichikovD/notes-service`: 13 issues, 7 смерженных PR (включая 2 разрешённых конфликта
  и 2 итерации ревью), теги `v1.0.0`/`v1.1.0`, 30+ коммитов.
- CI (GitHub Actions) на публичном репозитории — **бесплатно, без верификации и без карты**.
- Раннеры `ubuntu-latest` идут с предустановленным Docker, поэтому Testcontainers работает «из коробки».
- Время одного PR-пайплайна (lint + test с Testcontainers + build + CodeQL + SpotBugs): ориентировочно 3–5 минут.
- Покрытие JaCoCo на чистом Linux-пути раннера — около 90%.

### GitLab — зеркало
- Проект `VichikovD/notes-service` создан через `glab`/API, код и теги запушены.
- Настроены **Protected Branch** (`push = No one`, merge только Maintainer через MR) и правила MR
  (merge только при зелёном pipeline и разрешённых тредах) — это бесплатный эквивалент branch protection.
- **Push Rules** (regex на сообщения коммитов, запрет секретов и т.п.) на gitlab.com — функция **Premium**;
  на free-тарифе доступны только Protected Branches.
- Локальный `gitlab-runner` (Docker executor, privileged, под dind/Testcontainers) был установлен,
  зарегистрирован как project runner и запущен.

### Критическая находка по применимости
При попытке запустить любой pipeline gitlab.com вернул:

```
Identity verification is required in order to run CI jobs
```

То есть на бесплатном аккаунте gitlab.com **запуск ЛЮБОГО CI требует identity verification, в общем случае
с привязкой банковской карты** — причём это касается даже **собственного self-hosted runner'а**, а не только
облачных shared-runners. Без верификации картой живой прогон GitLab CI на gitlab.com недоступен.

Для сравнения: **GitHub Actions на публичном репозитории запускается сразу, без верификации и карты.**

### Уточнённый вывод по применимости
- **Учебный / исследовательский публичный проект → GitHub**: порог входа ощутимо ниже — CI стартует
  немедленно, без карты, раннеры с Docker готовы.
- **GitLab на gitlab.com (free)**: ставит барьер верификации картой для запуска CI; обходится только
  self-managed GitLab (свой сервер) либо верифицированным/платным аккаунтом. При этом репозиторий,
  protected branches и сам процесс MR доступны без оплаты.
- **Настройка self-hosted runner под GitLab** (скачать бинарь, зарегистрировать, выбрать executor,
  настроить Docker) — заметно больше ручной работы, чем готовые hosted-раннеры GitHub.
