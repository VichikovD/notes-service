# Архитектура

## Слои

```
HTTP
 │
 ▼
┌────────────────────────────────────────────┐
│ web (controller)                            │
│  NoteController          @RestController     │
│  GlobalExceptionHandler  @RestControllerAdvice│
│  ApiError (DTO ошибки)                       │
└───────────────┬────────────────────────────┘
                │  зависит от интерфейса NoteService
                ▼
┌────────────────────────────────────────────┐
│ service (бизнес-логика)                      │
│  NoteService (interface)                     │
│  NoteServiceImpl  @Service @Transactional    │
│   - проверка дублей, проброс NotFound/Conflict│
│   - Clock для детерминированных времён        │
└───────────────┬────────────────────────────┘
                │  зависит от NoteRepository + NoteMapper
                ▼
┌────────────────────────────────────────────┐
│ repository (доступ к данным)                 │
│  NoteRepository extends JpaRepository        │
│   findAllByDoneOrderByCreatedAtDesc          │
│   existsByTitleIgnoreCase                     │
└───────────────┬────────────────────────────┘
                ▼
┌────────────────────────────────────────────┐
│ domain + БД                                  │
│  Note  @Entity → таблица notes               │
└────────────────────────────────────────────┘
```

Принципы:
- Контроллер не знает о JPA; общение со слоем сервиса — только через интерфейс `NoteService`.
- Внешний контракт (DTO `CreateNoteRequest` / `UpdateNoteRequest` / `NoteResponse`) отделён от сущности `Note`;
  преобразование — в `NoteMapper`.
- Время инкапсулировано в бин `Clock` — это делает временные метки тестируемыми (фиксированный Clock в unit-тестах).

## Схема БД

Таблица `notes`:

| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | bigint | PK, identity |
| `title` | varchar(120) | not null |
| `content` | text | nullable |
| `done` | boolean | not null |
| `created_at` | timestamp | not null, immutable |
| `updated_at` | timestamp | not null |

DDL генерируется Hibernate (`ddl-auto=update` локально, `create-drop` в тестах). В промышленном варианте
сюда добавилась бы Flyway-миграция `V1__create_notes.sql`.

## Обработка ошибок

| Ситуация | Исключение | HTTP |
|---|---|---|
| Заметка не найдена | `NoteNotFoundException` | 404 Not Found |
| Дубль заголовка | `DuplicateNoteTitleException` | 409 Conflict |
| Невалидный ввод | `MethodArgumentNotValidException` | 400 Bad Request (+ `fieldErrors`) |
