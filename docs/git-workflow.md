# Git workflow — GitHub Flow

## Почему GitHub Flow

Проект маленький, изменения частые и небольшие, релизы линейные. GitHub Flow даёт минимум накладных
расходов: одна стабильная ветка `main` + короткоживущие feature-ветки. Git Flow с его `develop`/`release`/
`hotfix` здесь избыточен (нет нескольких поддерживаемых релизных линий), а trunk-based без PR не даёт места
для обязательного code review, что важно в учебном контексте.

## Ветки

| Ветка | Назначение | Жизнь |
|---|---|---|
| `main` | стабильная, всегда зелёная, релизы по тегам | постоянная |
| `feature/<issue>-<slug>` | новая функциональность | до merge |
| `fix/<issue>-<slug>` | исправление | до merge |
| `docs/<issue>-<slug>`, `ci/<issue>-<slug>` | документация / пайплайны | до merge |

## Жизненный цикл изменения

```
Issue #N  ──►  ветка feature/N-...  ──►  коммиты (Conventional Commits)
   ──►  Pull/Merge Request (Closes #N)  ──►  CI (lint/test/build/security)
   ──►  Code review (≥1 approve)  ──►  Merge в main  ──►  (по вехе) тег vX.Y.Z + Release
```

## Намеренные конфликты в истории (учебная демонстрация)

1. **`pom.xml`** — две ветки одновременно добавляют зависимость в один и тот же блок `<dependencies>`
   (`feature/4-validation` и `feature/3-integration-tests`). Разрешение: оставить обе зависимости.
2. **`NoteServiceImpl.create`** — `feature/7-duplicate-check` добавляет проверку дубля, а
   `feature/8-clock` меняет установку времени в том же методе. Разрешение: объединить — сначала проверка
   дубля, затем установка времени через `Clock`.

## Защита main (branch protection)

См. [ci-cd.md](ci-cd.md) и раздел отчёта. Кратко: прямой push в `main` запрещён, обязательны
прохождение CI и минимум одно одобрение PR.

## Релизы

- Релиз — аннотированный тег: `git tag -a v1.0.0 -m "Release 1.0.0"`.
- Тег синхронизирован с разделом `CHANGELOG.md`.
- Пуш тега запускает стадию `release` в CI (сборка и публикация jar).
