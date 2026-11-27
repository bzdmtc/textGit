package com.example.smart_learning.learn.repository;

import com.example.smart_learning.learn.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByUserIdAndLessonIdOrderByIdDesc(Long userId, Long lessonId);

    Optional<Note> findByIdAndUserId(Long id, Long userId);
}
