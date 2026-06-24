package com.example.notes.mapper;

import com.example.notes.domain.Note;
import com.example.notes.dto.CreateNoteRequest;
import com.example.notes.dto.NoteResponse;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

  public Note toEntity(CreateNoteRequest request) {
    return new Note(request.title(), request.content(), request.done());
  }

  public NoteResponse toResponse(Note note) {
    return new NoteResponse(
        note.getId(),
        note.getTitle(),
        note.getContent(),
        note.isDone(),
        note.getCreatedAt(),
        note.getUpdatedAt());
  }
}
