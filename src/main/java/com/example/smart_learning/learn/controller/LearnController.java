package com.example.smart_learning.learn.controller;

import com.example.smart_learning.common.api.ApiResponse;
import com.example.smart_learning.learn.dto.FavoriteDto;
import com.example.smart_learning.learn.dto.FavoriteStatusDto;
import com.example.smart_learning.learn.dto.LearnCourseDto;
import com.example.smart_learning.learn.dto.NoteDto;
import com.example.smart_learning.learn.dto.NoteRequest;
import com.example.smart_learning.learn.dto.ProgressDto;
import com.example.smart_learning.learn.dto.ProgressRequest;
import com.example.smart_learning.learn.dto.ReviewDto;
import com.example.smart_learning.learn.dto.ReviewRequest;
import com.example.smart_learning.learn.service.FavoriteService;
import com.example.smart_learning.learn.service.LearnService;
import com.example.smart_learning.learn.service.NoteService;
import com.example.smart_learning.learn.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learn")
public class LearnController {

    private final LearnService learnService;
    private final NoteService noteService;
    private final FavoriteService favoriteService;
    private final ReviewService reviewService;

    public LearnController(LearnService learnService,
                           NoteService noteService,
                           FavoriteService favoriteService,
                           ReviewService reviewService) {
        this.learnService = learnService;
        this.noteService = noteService;
        this.favoriteService = favoriteService;
        this.reviewService = reviewService;
    }

    @GetMapping("/courses/{courseId}")
    public ApiResponse<LearnCourseDto> course(@PathVariable Long courseId) {
        return ApiResponse.success(learnService.courseDetail(courseId));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/progress")
    public ApiResponse<ProgressDto> progress(@PathVariable Long courseId,
                                             @PathVariable Long lessonId,
                                             @Valid @RequestBody ProgressRequest request) {
        return ApiResponse.success(learnService.reportProgress(courseId, lessonId, request));
    }

    @GetMapping("/courses/{courseId}/lessons/{lessonId}/notes")
    public ApiResponse<List<NoteDto>> notes(@PathVariable Long courseId, @PathVariable Long lessonId) {
        return ApiResponse.success(noteService.list(courseId, lessonId));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/notes")
    public ApiResponse<NoteDto> createNote(@PathVariable Long courseId,
                                           @PathVariable Long lessonId,
                                           @Valid @RequestBody NoteRequest request) {
        return ApiResponse.success(noteService.create(courseId, lessonId, request));
    }

    @PutMapping("/notes/{noteId}")
    public ApiResponse<NoteDto> updateNote(@PathVariable Long noteId,
                                           @Valid @RequestBody NoteRequest request) {
        return ApiResponse.success(noteService.update(noteId, request));
    }

    @DeleteMapping("/notes/{noteId}")
    public ApiResponse<Void> deleteNote(@PathVariable Long noteId) {
        noteService.delete(noteId);
        return ApiResponse.success(null);
    }

    @GetMapping("/favorites")
    public ApiResponse<List<FavoriteDto>> favorites() {
        return ApiResponse.success(favoriteService.list());
    }

    @GetMapping("/favorites/{courseId}/status")
    public ApiResponse<FavoriteStatusDto> favoriteStatus(@PathVariable Long courseId) {
        return ApiResponse.success(favoriteService.status(courseId));
    }

    @PostMapping("/favorites/{courseId}")
    public ApiResponse<FavoriteStatusDto> addFavorite(@PathVariable Long courseId) {
        return ApiResponse.success(favoriteService.add(courseId));
    }

    @DeleteMapping("/favorites/{courseId}")
    public ApiResponse<FavoriteStatusDto> removeFavorite(@PathVariable Long courseId) {
        return ApiResponse.success(favoriteService.remove(courseId));
    }

    @PostMapping("/courses/{courseId}/reviews")
    public ApiResponse<ReviewDto> review(@PathVariable Long courseId,
                                         @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success(reviewService.create(courseId, request));
    }
}
