package com.example.notes.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notes.domain.Note;
import com.example.notes.dto.CreateNoteRequest;
import com.example.notes.dto.NoteResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class NoteMapperTest {

  private static final Instant TS = Instant.parse("2026-01-01T10:00:00Z");

  private final NoteMapper mapper = new NoteMapper();

  @Test
  void toEntity_copiesAllFields() {
    var request = new CreateNoteRequest("Title", "Body", true);

    Note note = mapper.toEntity(request);

    assertThat(note.getTitle()).isEqualTo("Title");
    assertThat(note.getContent()).isEqualTo("Body");
    assertThat(note.isDone()).isTrue();
  }

  @Test
  void toResponse_copiesAllFields() {
    Note note = new Note("Title", "Body", false);
    note.markCreated(TS);

    NoteResponse response = mapper.toResponse(note);

    assertThat(response.title()).isEqualTo("Title");
    assertThat(response.content()).isEqualTo("Body");
    assertThat(response.done()).isFalse();
    assertThat(response.createdAt()).isEqualTo(TS);
    assertThat(response.updatedAt()).isEqualTo(TS);
  }

  @Test
  void toResponse_handlesNullContent() {
    Note note = new Note("Title", null, false);
    note.markCreated(TS);

    NoteResponse response = mapper.toResponse(note);

    assertThat(response.content()).isNull();
  }
}
