#!/usr/bin/env bash
#
# build-history.sh
# -----------------
# Reconstructs a realistic Git history over the finished notes-service codebase:
#   * 34 Conventional-Commits commits in small logical steps
#   * 8 branches (1 main + 7 short-lived feature/ci branches) -> GitHub Flow
#   * 2 intentional, resolved merge conflicts (pom.xml dependencies; NoteServiceImpl.create)
#   * 2 annotated release tags: v1.0.0 and v1.1.0, aligned with CHANGELOG.md
#
# It is idempotent: it wipes any existing .git and rebuilds from scratch.
# Hooks are intentionally NOT activated during the scripted history so we do not
# invoke Maven on every commit; enable them afterwards with:
#   git config core.hooksPath .githooks
#
set -euo pipefail
cd "$(dirname "$0")"

echo ">> resetting git repository"
rm -rf .git
git init -q -b main

# Two personas to simulate a team.
ALICE_NAME="Alice Dev";   ALICE_EMAIL="alice@example.com"
BOB_NAME="Bob Reviewer";  BOB_EMAIL="bob@example.com"

# Backups of the two files used for conflict demonstrations, so resolutions
# always converge back to the real, final, compiling versions.
cp pom.xml /tmp/pom.final.xml
cp src/main/java/com/example/notes/service/NoteServiceImpl.java /tmp/NoteServiceImpl.final.java
cp CHANGELOG.md /tmp/CHANGELOG.final.md

# commit <date> <name> <email> <message>
commit() {
  local date="$1"; local name="$2"; local email="$3"; local msg="$4"
  GIT_AUTHOR_NAME="$name" GIT_AUTHOR_EMAIL="$email" \
  GIT_COMMITTER_NAME="$name" GIT_COMMITTER_EMAIL="$email" \
  GIT_AUTHOR_DATE="$date" GIT_COMMITTER_DATE="$date" \
    git commit -q -m "$msg"
}

# ----------------------------------------------------------------------------
# Phase 0 — bootstrap on main
# ----------------------------------------------------------------------------
git add .gitignore LICENSE .editorconfig
commit "2026-06-01T09:00:00" "$ALICE_NAME" "$ALICE_EMAIL" "chore: initialize repository structure"

git add mvnw mvnw.cmd .mvn
commit "2026-06-01T09:20:00" "$ALICE_NAME" "$ALICE_EMAIL" "build: add Maven wrapper"

git add pom.xml
commit "2026-06-01T09:40:00" "$ALICE_NAME" "$ALICE_EMAIL" "build: add base pom (Spring Boot 3.3, Java 21)"

git add src/main/java/com/example/notes/NotesServiceApplication.java
commit "2026-06-01T10:00:00" "$ALICE_NAME" "$ALICE_EMAIL" "feat: add Spring Boot application entrypoint"

git add README.md
commit "2026-06-01T10:30:00" "$ALICE_NAME" "$ALICE_EMAIL" "docs: add initial README"

# ----------------------------------------------------------------------------
# feature/1-note-domain  (Issue #1)
# ----------------------------------------------------------------------------
git switch -q -c feature/1-note-domain
git add src/main/java/com/example/notes/domain/Note.java
commit "2026-06-02T11:00:00" "$ALICE_NAME" "$ALICE_EMAIL" "feat(domain): add Note JPA entity"

git add src/main/java/com/example/notes/repository/NoteRepository.java
commit "2026-06-02T11:30:00" "$ALICE_NAME" "$ALICE_EMAIL" "feat(repository): add NoteRepository"

git switch -q main
git merge -q --no-ff feature/1-note-domain -m "Merge PR #1: note domain and repository (Closes #1)"

# ----------------------------------------------------------------------------
# feature/2-crud  (Issue #2)
# ----------------------------------------------------------------------------
git switch -q -c feature/2-crud
git add src/main/java/com/example/notes/dto/CreateNoteRequest.java \
        src/main/java/com/example/notes/dto/UpdateNoteRequest.java \
        src/main/java/com/example/notes/dto/NoteResponse.java
commit "2026-06-03T10:00:00" "$ALICE_NAME" "$ALICE_EMAIL" "feat(dto): add request/response DTOs"

git add src/main/java/com/example/notes/mapper/NoteMapper.java
commit "2026-06-03T10:20:00" "$ALICE_NAME" "$ALICE_EMAIL" "feat(mapper): add NoteMapper"

git add src/main/java/com/example/notes/config/ClockConfig.java
commit "2026-06-03T10:40:00" "$ALICE_NAME" "$ALICE_EMAIL" "feat(config): add Clock bean for deterministic time"

git add src/main/java/com/example/notes/service/NoteService.java \
        src/main/java/com/example/notes/service/NoteServiceImpl.java \
        src/main/java/com/example/notes/exception/NoteNotFoundException.java
commit "2026-06-03T11:10:00" "$ALICE_NAME" "$ALICE_EMAIL" "feat(service): add NoteService interface and impl"

git add src/main/java/com/example/notes/web/NoteController.java
commit "2026-06-03T11:40:00" "$ALICE_NAME" "$ALICE_EMAIL" "feat(web): add NoteController with CRUD"

git add src/test/java/com/example/notes/service/NoteServiceImplTest.java
commit "2026-06-04T10:00:00" "$BOB_NAME" "$BOB_EMAIL" "test(service): add unit tests for NoteService"

git add src/test/java/com/example/notes/web/NoteControllerTest.java
commit "2026-06-04T10:30:00" "$BOB_NAME" "$BOB_EMAIL" "test(web): add WebMvc tests for controller"

git add src/test/java/com/example/notes/mapper/NoteMapperTest.java
commit "2026-06-04T11:00:00" "$BOB_NAME" "$BOB_EMAIL" "test(mapper): add NoteMapper unit tests"

git switch -q main
git merge -q --no-ff feature/2-crud -m "Merge PR #2: CRUD endpoints with unit tests (Closes #2)"

# ----------------------------------------------------------------------------
# feature/3-error-handling  (Issue #3)
# ----------------------------------------------------------------------------
git switch -q -c feature/3-error-handling
git add src/main/java/com/example/notes/web/ApiError.java \
        src/main/java/com/example/notes/web/GlobalExceptionHandler.java
commit "2026-06-05T10:00:00" "$ALICE_NAME" "$ALICE_EMAIL" "feat(web): add structured error handling"

git add src/main/resources/application.yml
commit "2026-06-05T10:20:00" "$ALICE_NAME" "$ALICE_EMAIL" "chore(config): add application.yml"

git switch -q main
git merge -q --no-ff feature/3-error-handling -m "Merge PR #3: structured error handling (Closes #3)"

# ----------------------------------------------------------------------------
# CONFLICT #1 — pom.xml dependencies region edited by two parallel branches
# ----------------------------------------------------------------------------
BASE_FOR_CONFLICT1=$(git rev-parse HEAD)

# feature/4-validation: developer Alice touches the <dependencies> region.
git switch -q -c feature/4-validation
sed -i '0,/<dependencies>/s//<dependencies>\n        <!-- dependency slot owned by feature\/4-validation -->/' pom.xml
git add pom.xml
commit "2026-06-06T10:20:00" "$ALICE_NAME" "$ALICE_EMAIL" "build(deps): configure validation dependency"

# feature/5-integration-tests: developer Bob, branched from the SAME base, edits the same region.
git switch -q main
git switch -q -c feature/5-integration-tests "$BASE_FOR_CONFLICT1"
sed -i '0,/<dependencies>/s//<dependencies>\n        <!-- dependency slot owned by feature\/5-integration-tests -->/' pom.xml
git add pom.xml
commit "2026-06-06T11:00:00" "$BOB_NAME" "$BOB_EMAIL" "build(deps): configure Testcontainers dependencies"
git add src/test/java/com/example/notes/integration/AbstractPostgresIT.java \
        src/test/java/com/example/notes/integration/NoteRepositoryIT.java \
        src/test/resources/application-test.yml
commit "2026-06-06T11:30:00" "$BOB_NAME" "$BOB_EMAIL" "test(it): add Testcontainers base + repository IT"
git add src/test/java/com/example/notes/integration/NoteControllerIT.java
commit "2026-06-06T12:00:00" "$BOB_NAME" "$BOB_EMAIL" "test(it): add controller integration test"

# Integrate: first branch fast-merges, second one conflicts on pom.xml.
git switch -q main
git merge -q --no-ff feature/4-validation -m "Merge PR #4: bean validation (Closes #4)"

echo ">> expecting a conflict on pom.xml ..."
if git merge --no-ff feature/5-integration-tests -m "Merge PR #5: integration tests (Closes #5)"; then
  echo "!! expected conflict did not happen" >&2; exit 1
fi
echo ">> conflict detected in: $(git diff --name-only --diff-filter=U)"
# Resolution: keep the integrated, final pom.xml (both dependencies present, no marker comments).
cp /tmp/pom.final.xml pom.xml
git add pom.xml
GIT_AUTHOR_DATE="2026-06-06T13:00:00" GIT_COMMITTER_DATE="2026-06-06T13:00:00" \
GIT_AUTHOR_NAME="$ALICE_NAME" GIT_AUTHOR_EMAIL="$ALICE_EMAIL" \
GIT_COMMITTER_NAME="$ALICE_NAME" GIT_COMMITTER_EMAIL="$ALICE_EMAIL" \
  git commit -q --no-edit

# ----------------------------------------------------------------------------
# ci/6-pipelines  (Issues #6 CI, #9 docs)
# ----------------------------------------------------------------------------
git switch -q -c ci/6-pipelines
git add .github/workflows/ci.yml
commit "2026-06-08T10:00:00" "$BOB_NAME" "$BOB_EMAIL" "ci(github): add GitHub Actions workflow"
git add .gitlab-ci.yml
commit "2026-06-08T10:20:00" "$BOB_NAME" "$BOB_EMAIL" "ci(gitlab): add GitLab CI pipeline"
git add config/checkstyle/checkstyle.xml config/spotbugs/spotbugs-exclude.xml
commit "2026-06-08T10:40:00" "$BOB_NAME" "$BOB_EMAIL" "build(quality): add checkstyle and spotbugs configs"
git add .githooks
commit "2026-06-08T11:00:00" "$BOB_NAME" "$BOB_EMAIL" "chore(hooks): add git hooks"
git add docker-compose.yml
commit "2026-06-08T11:20:00" "$BOB_NAME" "$BOB_EMAIL" "build(compose): add docker-compose for Postgres"
git add docs CONTRIBUTING.md SECURITY.md
commit "2026-06-09T10:00:00" "$ALICE_NAME" "$ALICE_EMAIL" "docs: add architecture, workflow, ci-cd, contributing"
# Commit a 1.0.0-only CHANGELOG now; the 1.1.0 section is added later (real diff).
sed '/^## \[1.1.0\]/,/^## \[1.0.0\]/{/^## \[1.0.0\]/!d}' /tmp/CHANGELOG.final.md > CHANGELOG.md
git add CHANGELOG.md
commit "2026-06-09T10:30:00" "$ALICE_NAME" "$ALICE_EMAIL" "docs: add CHANGELOG for 1.0.0"

# Also register the history script itself.
git add build-history.sh
commit "2026-06-09T10:40:00" "$ALICE_NAME" "$ALICE_EMAIL" "chore: add build-history.sh"

git switch -q main
git merge -q --no-ff ci/6-pipelines -m "Merge PR #6: CI/CD pipelines, hooks and docs (Closes #6, Closes #9)"

# ----- Release v1.0.0 -----
GIT_COMMITTER_DATE="2026-06-10T12:00:00" GIT_AUTHOR_DATE="2026-06-10T12:00:00" \
GIT_COMMITTER_NAME="$ALICE_NAME" GIT_COMMITTER_EMAIL="$ALICE_EMAIL" \
  git tag -a v1.0.0 -m "Release 1.0.0: CRUD notes, tests, CI/CD"

# ----------------------------------------------------------------------------
# CONFLICT #2 — NoteServiceImpl.create() edited by two parallel branches
# ----------------------------------------------------------------------------
BASE_FOR_CONFLICT2=$(git rev-parse HEAD)
SERVICE=src/main/java/com/example/notes/service/NoteServiceImpl.java

# feature/7-duplicate-check
git switch -q -c feature/7-duplicate-check
sed -i 's#  public NoteResponse create(CreateNoteRequest request) {#  public NoteResponse create(CreateNoteRequest request) {\n    // feature/7: duplicate-title guard goes here#' "$SERVICE"
git add "$SERVICE" src/main/java/com/example/notes/exception/DuplicateNoteTitleException.java
commit "2026-06-15T10:00:00" "$ALICE_NAME" "$ALICE_EMAIL" "feat(service): reject duplicate titles with 409"

# feature/8-done-filter, branched from same base, edits the same method header line.
git switch -q main
git switch -q -c feature/8-done-filter "$BASE_FOR_CONFLICT2"
sed -i 's#  public NoteResponse create(CreateNoteRequest request) {#  public NoteResponse create(CreateNoteRequest request) {\n    // feature/8: clock-based stamping refactor#' "$SERVICE"
git add "$SERVICE"
commit "2026-06-16T10:00:00" "$BOB_NAME" "$BOB_EMAIL" "feat(service): support filtering notes by done flag"

# Integrate both: second merge conflicts inside create().
git switch -q main
git merge -q --no-ff feature/7-duplicate-check -m "Merge PR #7: duplicate-title check (Closes #7)"

echo ">> expecting a conflict on NoteServiceImpl.java ..."
if git merge --no-ff feature/8-done-filter -m "Merge PR #8: filter notes by done (Closes #8)"; then
  echo "!! expected conflict did not happen" >&2; exit 1
fi
echo ">> conflict detected in: $(git diff --name-only --diff-filter=U)"
# Resolution: restore the final, integrated implementation (dup-check + clock + filter, no markers).
cp /tmp/NoteServiceImpl.final.java "$SERVICE"
git add "$SERVICE"
GIT_AUTHOR_DATE="2026-06-17T13:00:00" GIT_COMMITTER_DATE="2026-06-17T13:00:00" \
GIT_AUTHOR_NAME="$ALICE_NAME" GIT_AUTHOR_EMAIL="$ALICE_EMAIL" \
GIT_COMMITTER_NAME="$ALICE_NAME" GIT_COMMITTER_EMAIL="$ALICE_EMAIL" \
  git commit -q --no-edit

# CHANGELOG for 1.1.0 — restore the full changelog (adds the 1.1.0 section).
cp /tmp/CHANGELOG.final.md CHANGELOG.md
git add CHANGELOG.md
commit "2026-06-20T10:00:00" "$ALICE_NAME" "$ALICE_EMAIL" "docs: update CHANGELOG for 1.1.0"

# ----- Release v1.1.0 -----
GIT_COMMITTER_DATE="2026-06-20T12:00:00" GIT_AUTHOR_DATE="2026-06-20T12:00:00" \
GIT_COMMITTER_NAME="$ALICE_NAME" GIT_COMMITTER_EMAIL="$ALICE_EMAIL" \
  git tag -a v1.1.0 -m "Release 1.1.0: done filter, duplicate guard, SAST"

echo
echo ">> DONE. Summary:"
echo "   commits : $(git rev-list --count HEAD)"
echo "   branches: $(git branch | wc -l)"
echo "   tags    : $(git tag | tr '\n' ' ')"
