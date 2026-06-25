package com.example.notes.dto;

public record CreateNoteRequest(String title, String content, boolean done) {}
