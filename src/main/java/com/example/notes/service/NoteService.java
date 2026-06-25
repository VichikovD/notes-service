package com.example.notes.service;

import com.example.notes.dto.CreateNoteRequest;
import com.example.notes.dto.NoteResponse;
import com.example.notes.dto.UpdateNoteRequest;
import java.util.List;

public interface NoteService {

  NoteResponse create(CreateNoteRequest request);

  NoteResponse getById(Long id);

  List<NoteResponse> findAll(Boolean done);

  java.util.List<NoteResponse> search(String query);

  NoteResponse update(Long id, UpdateNoteRequest request);

  void delete(Long id);
}
