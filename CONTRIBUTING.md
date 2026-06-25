# Contributing

## Модель ветвления — GitHub Flow

1. `main` всегда стабильна (зелёный CI, готова к релизу).
2. На каждую задачу — короткоживущая ветка от `main`:
   `feature/<issue>-<slug>`, `fix/<issue>-<slug>`, `docs/<issue>-<slug>`.
3. Открываете Pull/Merge Request с описанием и ссылкой `Closes #N`.
4. Merge в `main` — только при зелёном CI и ≥1 одобрении ревьюера.
5. Релиз — аннотированный тег `vX.Y.Z` на `main`, синхронизированный с `CHANGELOG.md`.

## Conventional Commits

Формат сообщения: `<type>(<scope>): <subject>`.

Типы: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`.

Примеры:
```
feat(notes): add POST /api/notes endpoint
test(service): cover duplicate title path
ci(github): add CodeQL SAST job
```
Формат проверяется хуком `commit-msg`.

## Перед коммитом

```bash
git config core.hooksPath .githooks   # один раз
./mvnw spotless:apply                  # автоформат
./mvnw test                            # unit-тесты
```

## Definition of Done

- [ ] Код проходит `spotless:check` и `checkstyle:check`.
- [ ] Покрытие не ниже 70% (`jacoco:check`).
- [ ] Для каждой ручки есть unit-тест (SUCCESS) и, где применимо, NOT_FOUND/валидация.
- [ ] `spotbugs:check` без находок.
- [ ] PR связан с issue (`Closes #N`), CI зелёный, есть аппрув.
