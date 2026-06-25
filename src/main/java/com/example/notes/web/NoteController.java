package com.example.notes.web;

import com.example.notes.dto.CreateNoteRequest;
import com.example.notes.dto.NoteResponse;
import com.example.notes.dto.UpdateNoteRequest;
import com.example.notes.service.NoteService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

  private final NoteService noteService;

  public NoteController(NoteService noteService) {
    this.noteService = noteService;
  }

  @PostMapping
  public ResponseEntity<NoteResponse> create(@Valid @RequestBody CreateNoteRequest request) {
    NoteResponse created = noteService.create(request);
    return ResponseEntity.created(URI.create("/api/notes/" + created.id())).body(created);
  }

  @GetMapping("/{id}")
  public NoteResponse getById(@PathVariable Long id) {
    return noteService.getById(id);
  }

  @GetMapping
  public List<NoteResponse> findAll(@RequestParam(required = false) Boolean done) {
    return noteService.findAll(done);
  }

  @GetMapping("/search")
  public List<NoteResponse> search(@RequestParam String q) {
    return noteService.search(q);
  }

  @GetMapping("/count")
  public long count(@RequestParam(required = false) Boolean done) {
    return noteService.count(done);
  }

  @PutMapping("/{id}")
  public NoteResponse update(@PathVariable Long id, @Valid @RequestBody UpdateNoteRequest request) {
    return noteService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    noteService.delete(id);
  }
}
