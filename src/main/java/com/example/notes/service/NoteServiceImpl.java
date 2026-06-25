package com.example.notes.service;

import com.example.notes.domain.Note;
import com.example.notes.dto.CreateNoteRequest;
import com.example.notes.dto.NoteResponse;
import com.example.notes.dto.UpdateNoteRequest;
import com.example.notes.exception.DuplicateNoteTitleException;
import com.example.notes.exception.NoteNotFoundException;
import com.example.notes.mapper.NoteMapper;
import com.example.notes.repository.NoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NoteServiceImpl implements NoteService {

  private final NoteRepository repository;
  private final NoteMapper mapper;
  private final Clock clock;

  public NoteServiceImpl(NoteRepository repository, NoteMapper mapper, Clock clock) {
    this.repository = repository;
    this.mapper = mapper;
    this.clock = clock;
  }

  @Override
  public NoteResponse create(CreateNoteRequest request) {
    if (repository.existsByTitleIgnoreCase(request.title())) {
      throw new DuplicateNoteTitleException(request.title());
    }
    Note note = mapper.toEntity(request);
    note.markCreated(Instant.now(clock));
    return mapper.toResponse(repository.save(note));
  }

  @Override
  @Transactional(readOnly = true)
  public NoteResponse getById(Long id) {
    return mapper.toResponse(findOrThrow(id));
  }

  @Override
  @Transactional(readOnly = true)
  public List<NoteResponse> findAll(Boolean done) {
    List<Note> notes =
        done == null ? repository.findAll() : repository.findAllByDoneOrderByCreatedAtDesc(done);
    return notes.stream().map(mapper::toResponse).toList();
  }

  @Override
  public NoteResponse update(Long id, UpdateNoteRequest request) {
    Note note = findOrThrow(id);
    note.setTitle(request.title());
    note.setContent(request.content());
    note.setDone(request.done());
    note.touch(Instant.now(clock));
    return mapper.toResponse(repository.save(note));
  }

  @Override
  public void delete(Long id) {
    Note note = findOrThrow(id);
    repository.delete(note);
  }

  @Override
  @Transactional(readOnly = true)
  public java.util.List<NoteResponse> search(String query) {
    return repository.findByTitleContainingIgnoreCase(query).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long count(Boolean done) {
    return done == null ? repository.count() : repository.countByDone(done);
  }

  private Note findOrThrow(Long id) {
    return repository.findById(id).orElseThrow(() -> new NoteNotFoundException(id));
  }
}
