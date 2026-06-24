# notes-service

Небольшой REST-сервис заметок на **Java 21 + Spring Boot 3.3**, созданный как учебно-исследовательский
проект для демонстрации полного инженерного процесса разработки: Issue → ветка → код → PR → CI → review →
merge → release, продублированного на **GitHub Actions** и **GitLab CI/CD**.

## Возможности

- CRUD заметок (`/api/notes`) с валидацией (`jakarta.validation`).
- Слои `controller → service → repository → PostgreSQL` (внедрение зависимостей через интерфейсы).
- Единый формат ошибок (`ApiError`): 400 (валидация, с `fieldErrors`), 404 (нет заметки), 409 (дубль заголовка).

## Технологии

| Область | Выбор |
|---|---|
| Язык / платформа | Java 21 (LTS), Spring Boot 3.3.4 |
| Сборка | Maven + Maven Wrapper (`./mvnw`) |
| БД | PostgreSQL 16 (Spring Data JPA) |
| Unit-тесты | JUnit 5 + Mockito + AssertJ (`@WebMvcTest`, моки) |
| Интеграционные | `@SpringBootTest` + Testcontainers PostgreSQL (`*IT.java`) |
| Качество | spotless (google-java-format), checkstyle, spotbugs + find-sec-bugs, pmd, jacoco (≥70%) |
| Безопасность | OWASP dependency-check, CodeQL (GitHub) |

## REST API

| Метод | Путь | Назначение | Коды |
|---|---|---|---|
| `POST` | `/api/notes` | создать заметку | 201, 400, 409 |
| `GET` | `/api/notes/{id}` | получить по id | 200, 404 |
| `GET` | `/api/notes?done=` | список (опц. фильтр) | 200 |
| `PUT` | `/api/notes/{id}` | обновить | 200, 400, 404 |
| `DELETE` | `/api/notes/{id}` | удалить | 204, 404 |

## Локальный запуск

```bash
# 1. Поднять PostgreSQL
docker compose up -d

# 2. Запустить приложение
./mvnw spring-boot:run

# 3. Проверить
curl -s localhost:8080/api/notes
```

## Сборка и проверки

```bash
./mvnw spotless:check checkstyle:check   # lint
./mvnw test                              # unit (surefire)
./mvnw verify                            # unit + integration (Testcontainers) + JaCoCo 70% gate
./mvnw -Psecurity verify                 # + OWASP dependency-check
./mvnw -DskipTests package               # исполняемый jar -> target/notes-service.jar
```

> Для `verify` нужен запущенный Docker (интеграционные тесты поднимают PostgreSQL через Testcontainers).
> Сборка требует **JDK 21** (`JAVA_HOME` должен указывать на JDK 21).

## Git-хуки

```bash
git config core.hooksPath .githooks
```
`pre-commit` — формат+линт, `commit-msg` — Conventional Commits, `pre-push` — unit-тесты.

## Документация

- [docs/architecture.md](docs/architecture.md) — слои и схема БД
- [docs/git-workflow.md](docs/git-workflow.md) — GitHub Flow, branch protection
- [docs/ci-cd.md](docs/ci-cd.md) — пайплайны GitHub/GitLab
- [docs/platform-comparison.md](docs/platform-comparison.md) — исследование GitHub vs GitLab
- [CONTRIBUTING.md](CONTRIBUTING.md), [CHANGELOG.md](CHANGELOG.md), [SECURITY.md](SECURITY.md)
