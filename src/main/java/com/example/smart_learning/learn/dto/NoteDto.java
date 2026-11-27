package com.example.smart_learning.learn.dto;

import com.example.smart_learning.learn.entity.Note;

import java.time.LocalDateTime;

public record NoteDto(
        Long id,
        Long lessonId,
        String lessonTitle,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static NoteDto of(Note note, String lessonTitle) {
        return new NoteDto(note.getId(), note.getLessonId(), lessonTitle, note.getContent(),
                note.getCreatedAt(), note.getUpdatedAt());
    }
}
