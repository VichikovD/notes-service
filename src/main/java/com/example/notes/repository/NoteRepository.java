package com.example.notes.repository;

import com.example.notes.domain.Note;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {

  List<Note> findAllByDone(boolean done);
}
