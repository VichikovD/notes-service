package com.example.notes.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.notes.dto.CreateNoteRequest;
import com.example.notes.dto.NoteResponse;
import com.example.notes.repository.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoteControllerIT extends AbstractPostgresIT {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private NoteRepository repository;

  @BeforeEach
  void cleanUp() {
    repository.deleteAll();
  }

  @Test
  void createThenFetch_roundTripsThroughRealDatabase() {
    var create = new CreateNoteRequest("Integration note", "Body", false);

    ResponseEntity<NoteResponse> created =
        restTemplate.postForEntity("/api/notes", create, NoteResponse.class);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Long id = created.getBody().id();
    assertThat(id).isNotNull();

    ResponseEntity<NoteResponse> fetched =
        restTemplate.getForEntity("/api/notes/" + id, NoteResponse.class);

    assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(fetched.getBody().title()).isEqualTo("Integration note");
  }

  @Test
  void create_returns409_onDuplicateTitle() {
    restTemplate.postForEntity(
        "/api/notes", new CreateNoteRequest("Dup", null, false), NoteResponse.class);

    ResponseEntity<String> conflict =
        restTemplate.postForEntity(
            "/api/notes", new CreateNoteRequest("dup", null, false), String.class);

    assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void getById_returns404_whenMissing() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/notes/999999", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void list_returnsCreatedNotes() {
    restTemplate.postForEntity(
        "/api/notes", new CreateNoteRequest("A", null, false), NoteResponse.class);
    restTemplate.postForEntity(
        "/api/notes", new CreateNoteRequest("B", null, true), NoteResponse.class);

    ResponseEntity<NoteResponse[]> response =
        restTemplate.getForEntity("/api/notes", NoteResponse[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
  }

  @Test
  void search_findsNotesByTitleSubstring() {
    restTemplate.postForEntity(
        "/api/notes", new CreateNoteRequest("Buy milk", null, false), NoteResponse.class);
    restTemplate.postForEntity(
        "/api/notes", new CreateNoteRequest("Call doctor", null, false), NoteResponse.class);

    ResponseEntity<NoteResponse[]> response =
        restTemplate.getForEntity("/api/notes/search?q=milk", NoteResponse[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody()[0].title()).isEqualTo("Buy milk");
  }

  @Test
  void count_reflectsPersistedNotes() {
    restTemplate.postForEntity(
        "/api/notes", new CreateNoteRequest("A", null, false), NoteResponse.class);
    restTemplate.postForEntity(
        "/api/notes", new CreateNoteRequest("B", null, true), NoteResponse.class);

    ResponseEntity<Long> response = restTemplate.getForEntity("/api/notes/count", Long.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(2L);
  }
}
