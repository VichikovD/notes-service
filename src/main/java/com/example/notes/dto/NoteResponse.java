package com.example.notes.dto;

import java.time.Instant;

public record NoteResponse(
    Long id, String title, String content, boolean done, Instant createdAt, Instant updatedAt) {}
