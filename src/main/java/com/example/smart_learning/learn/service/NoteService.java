package com.example.smart_learning.learn.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.course.entity.CourseLesson;
import com.example.smart_learning.learn.dto.NoteDto;
import com.example.smart_learning.learn.dto.NoteRequest;
import com.example.smart_learning.learn.entity.Note;
import com.example.smart_learning.learn.repository.NoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Master
public class NoteService {

    private final NoteRepository noteRepository;
    private final LearnAccessService accessService;

    public NoteService(NoteRepository noteRepository, LearnAccessService accessService) {
        this.noteRepository = noteRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public List<NoteDto> list(Long courseId, Long lessonId) {
        Long userId = accessService.requireUserId();
        accessService.requireEnrolled(userId, courseId);
        CourseLesson lesson = accessService.requireLesson(courseId, lessonId);
        return noteRepository.findByUserIdAndLessonIdOrderByIdDesc(userId, lessonId).stream()
                .map(note -> NoteDto.of(note, lesson.getTitle()))
                .toList();
    }

    @Transactional
    public NoteDto create(Long courseId, Long lessonId, NoteRequest request) {
        Long userId = accessService.requireUserId();
        accessService.requireEnrolled(userId, courseId);
        CourseLesson lesson = accessService.requireLesson(courseId, lessonId);
        Note note = new Note();
        note.setUserId(userId);
        note.setLessonId(lessonId);
        note.setContent(request.content().trim());
        note = noteRepository.save(note);
        return NoteDto.of(note, lesson.getTitle());
    }

    @Transactional
    public NoteDto update(Long noteId, NoteRequest request) {
        Long userId = accessService.requireUserId();
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在"));
        note.setContent(request.content().trim());
        note = noteRepository.save(note);
        return NoteDto.of(note, null);
    }

    @Transactional
    public void delete(Long noteId) {
        Long userId = accessService.requireUserId();
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "笔记不存在"));
        noteRepository.delete(note);
    }
}
