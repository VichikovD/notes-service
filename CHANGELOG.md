# Changelog

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
проект следует [Semantic Versioning](https://semver.org/lang/ru/).

## [Unreleased]

## [1.1.0] - 2026-06-20
### Added
- Фильтрация списка заметок по статусу: `GET /api/notes?done=true|false`.
- Защита от дублей: `409 Conflict` при попытке создать заметку с существующим заголовком.
- Поле `updatedAt` и его обновление при `PUT`.
- CodeQL SAST и OWASP dependency-check в CI.
### Changed
- `GlobalExceptionHandler` возвращает структурированный `ApiError` с `fieldErrors`.

## [1.0.0] - 2026-06-10
### Added
- CRUD заметок: `POST/GET/PUT/DELETE /api/notes`.
- Валидация входных DTO (`@NotBlank`, `@Size`).
- Слои controller/service/repository, PostgreSQL через Spring Data JPA.
- Unit-тесты (JUnit 5 + Mockito + AssertJ) и интеграционные на Testcontainers.
- CI на GitHub Actions и GitLab CI/CD: lint → test → build → security → release.
- JaCoCo с порогом покрытия 70%.

[Unreleased]: https://example.com/notes-service/compare/v1.1.0...HEAD
[1.1.0]: https://example.com/notes-service/compare/v1.0.0...v1.1.0
[1.0.0]: https://example.com/notes-service/releases/tag/v1.0.0
