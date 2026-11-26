package com.example.smart_learning.course.entity;

import com.example.smart_learning.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "course_lesson")
public class CourseLesson extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "duration_sec", nullable = false)
    private Integer durationSec = 0;

    @Column(name = "is_trial", nullable = false)
    private Boolean isTrial = false;

    @Column(nullable = false)
    private Integer sort = 0;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";
}
