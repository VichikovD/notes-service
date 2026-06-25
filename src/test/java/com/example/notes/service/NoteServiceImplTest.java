package com.example.notes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notes.domain.Note;
import com.example.notes.dto.CreateNoteRequest;
import com.example.notes.dto.NoteResponse;
import com.example.notes.dto.UpdateNoteRequest;
import com.example.notes.exception.NoteNotFoundException;
import com.example.notes.mapper.NoteMapper;
import com.example.notes.repository.NoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoteServiceImplTest {

  private static final Instant FIXED = Instant.parse("2026-01-01T10:00:00Z");

  @Mock private NoteRepository repository;

  private NoteServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new NoteServiceImpl(repository, new NoteMapper(), Clock.fixed(FIXED, ZoneOffset.UTC));
  }

  @Test
  void create_persistsNote_andStampsTimestamps() {
    var request = new CreateNoteRequest("Buy milk", "2 liters", false);
    when(repository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

    NoteResponse response = service.create(request);

    assertThat(response.title()).isEqualTo("Buy milk");
    assertThat(response.content()).isEqualTo("2 liters");
    assertThat(response.done()).isFalse();
    assertThat(response.createdAt()).isEqualTo(FIXED);
    assertThat(response.updatedAt()).isEqualTo(FIXED);
  }

  @Test
  void create_savesEntityBuiltFromRequest() {
    var request = new CreateNoteRequest("Title", "Body", true);
    when(repository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

    service.create(request);

    ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().isDone()).isTrue();
    assertThat(captor.getValue().getCreatedAt()).isEqualTo(FIXED);
  }

  @Test
  void getById_returnsMappedResponse() {
    Note note = persistedNote(1L, "A", "B", false);
    when(repository.findById(1L)).thenReturn(Optional.of(note));

    NoteResponse response = service.getById(1L);

    assertThat(response.id()).isEqualTo(1L);
    assertThat(response.title()).isEqualTo("A");
  }

  @Test
  void getById_throwsNotFound_whenMissing() {
    when(repository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(99L))
        .isInstanceOf(NoteNotFoundException.class)
        .hasMessageContaining("99");
  }

  @Test
  void findAll_returnsAllNotes() {
    when(repository.findAll())
        .thenReturn(
            List.of(persistedNote(1L, "A", null, false), persistedNote(2L, "B", null, true)));

    List<NoteResponse> result = service.findAll(null);

    assertThat(result).hasSize(2).extracting(NoteResponse::title).containsExactly("A", "B");
  }

  @Test
  void findAll_withDoneFilter_delegatesToDerivedQuery() {
    when(repository.findAllByDone(true)).thenReturn(List.of(persistedNote(2L, "B", null, true)));

    List<NoteResponse> result = service.findAll(true);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).done()).isTrue();
    verify(repository, never()).findAll();
  }

  @Test
  void update_changesFields() {
    Note note = persistedNote(1L, "Old", "Old body", false);
    when(repository.findById(1L)).thenReturn(Optional.of(note));
    when(repository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));
    var request = new UpdateNoteRequest("New", "New body", true);

    NoteResponse response = service.update(1L, request);

    assertThat(response.title()).isEqualTo("New");
    assertThat(response.content()).isEqualTo("New body");
    assertThat(response.done()).isTrue();
  }

  @Test
  void update_refreshesUpdatedAt() {
    Note note = new Note("Old", "Old body", false);
    note.markCreated(Instant.parse("2025-12-01T00:00:00Z"));
    setId(note, 1L);
    when(repository.findById(1L)).thenReturn(Optional.of(note));
    when(repository.save(any(Note.class))).thenAnswer(inv -> inv.getArgument(0));

    NoteResponse response = service.update(1L, new UpdateNoteRequest("New", null, true));

    assertThat(response.updatedAt()).isEqualTo(FIXED);
    assertThat(response.updatedAt()).isNotEqualTo(response.createdAt());
  }

  @Test
  void update_throwsNotFound_whenMissing() {
    when(repository.findById(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(7L, new UpdateNoteRequest("x", null, false)))
        .isInstanceOf(NoteNotFoundException.class);
  }

  @Test
  void delete_removesEntity() {
    Note note = persistedNote(5L, "A", null, false);
    when(repository.findById(5L)).thenReturn(Optional.of(note));

    service.delete(5L);

    verify(repository).delete(note);
  }

  @Test
  void delete_throwsNotFound_whenMissing() {
    when(repository.findById(123L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(123L)).isInstanceOf(NoteNotFoundException.class);
    verify(repository, never()).delete(any());
  }

  private Note persistedNote(Long id, String title, String content, boolean done) {
    Note note = new Note(title, content, done);
    note.markCreated(FIXED);
    setId(note, id);
    return note;
  }

  private static void setId(Note note, Long id) {
    try {
      var field = Note.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(note, id);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
