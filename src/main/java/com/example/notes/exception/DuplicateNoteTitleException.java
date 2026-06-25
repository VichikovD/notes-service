package com.example.notes.exception;

public class DuplicateNoteTitleException extends RuntimeException {

  public DuplicateNoteTitleException(String title) {
    super("Note with this title already exists: " + title);
  }
}
