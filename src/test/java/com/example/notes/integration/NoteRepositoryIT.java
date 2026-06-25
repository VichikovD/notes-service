package com.example.notes.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notes.domain.Note;
import com.example.notes.repository.NoteRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NoteRepositoryIT extends AbstractPostgresIT {

  @Autowired private NoteRepository repository;

  @Test
  void savesAndReadsBackNote() {
    Note note = new Note("Persisted", "Body", false);
    note.markCreated(Instant.parse("2026-01-01T10:00:00Z"));

    Note saved = repository.save(note);

    assertThat(saved.getId()).isNotNull();
    assertThat(repository.findById(saved.getId())).isPresent();
  }

  @Test
  void countsPersistedNotes() {
    repository.deleteAll();
    repository.save(stamped("a"));
    repository.save(stamped("b"));

    assertThat(repository.count()).isEqualTo(2);
  }

  @Test
  void findAllByDone_returnsOnlyMatching() {
    repository.deleteAll();
    repository.save(withDone("done one", true));
    repository.save(withDone("open one", false));

    assertThat(repository.findAllByDone(true))
        .extracting(Note::getTitle)
        .containsExactly("done one");
  }

  private static Note stamped(String title) {
    Note note = new Note(title, null, false);
    note.markCreated(Instant.parse("2026-01-01T10:00:00Z"));
    return note;
  }

  private static Note withDone(String title, boolean done) {
    Note note = new Note(title, null, done);
    note.markCreated(Instant.parse("2026-01-01T10:00:00Z"));
    return note;
  }
}
