package com.example.notes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.notes.dto.NoteResponse;
import com.example.notes.exception.NoteNotFoundException;
import com.example.notes.service.NoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NoteController.class)
class NoteControllerTest {

  private static final Instant TS = Instant.parse("2026-01-01T10:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private NoteService noteService;

  @Test
  void create_returns201_withLocationHeader() throws Exception {
    when(noteService.create(any()))
        .thenReturn(new NoteResponse(1L, "Title", "Body", false, TS, TS));

    mockMvc
        .perform(
            post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("title", "Title", "content", "Body", "done", false))))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/notes/1"))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.title").value("Title"));
  }

  @Test
  void create_returns400_whenTitleBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("title", "   ", "content", "Body", "done", false))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.fieldErrors.title").exists());
  }

  @Test
  void getById_returns200() throws Exception {
    when(noteService.getById(1L)).thenReturn(new NoteResponse(1L, "A", "B", true, TS, TS));

    mockMvc
        .perform(get("/api/notes/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("A"));
  }

  @Test
  void getById_returns404_whenMissing() throws Exception {
    when(noteService.getById(99L)).thenThrow(new NoteNotFoundException(99L));

    mockMvc
        .perform(get("/api/notes/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Note not found: id=99"));
  }

  @Test
  void findAll_returns200_withList() throws Exception {
    when(noteService.findAll(null))
        .thenReturn(
            List.of(
                new NoteResponse(1L, "A", null, false, TS, TS),
                new NoteResponse(2L, "B", null, true, TS, TS)));

    mockMvc
        .perform(get("/api/notes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].title").value("A"));
  }

  @Test
  void update_returns200() throws Exception {
    when(noteService.update(eq(1L), any()))
        .thenReturn(new NoteResponse(1L, "New", "New body", true, TS, TS));

    mockMvc
        .perform(
            put("/api/notes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of("title", "New", "content", "New body", "done", true))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("New"));
  }

  @Test
  void delete_returns204() throws Exception {
    mockMvc.perform(delete("/api/notes/1")).andExpect(status().isNoContent());
  }

  @Test
  void delete_returns404_whenMissing() throws Exception {
    doThrow(new NoteNotFoundException(5L)).when(noteService).delete(5L);

    mockMvc.perform(delete("/api/notes/5")).andExpect(status().isNotFound());
  }
}
