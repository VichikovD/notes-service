package com.example.notes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNoteRequest(
    @NotBlank(message = "title must not be blank")
        @Size(max = 120, message = "title must be at most 120 characters")
        String title,
    @Size(max = 10_000, message = "content must be at most 10000 characters") String content,
    boolean done) {}
