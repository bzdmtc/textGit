<script setup lang="ts">
import type { Course } from '@/types'

defineProps<{ course: Course }>()

function teacherNames(course: Course) {
  return course.teachers?.map((teacher) => teacher.name).join(' / ') || '智学名师'
}
</script>

<template>
  <router-link :to="`/courses/${course.id}`" class="course-card">
    <div
      class="course-cover"
      :style="course.coverUrl ? { backgroundImage: `url(${course.coverUrl})` } : undefined"
    >
      <span class="course-stage">{{ course.stage }}</span>
    </div>
    <div class="course-body">
      <div class="course-subject">{{ course.subject }}</div>
      <h3>{{ course.title }}</h3>
      <p>{{ course.subtitle || '系统课程 · 名师主讲' }}</p>
      <div class="course-teacher">{{ teacherNames(course) }}</div>
      <div class="course-footer">
        <div class="price">
          <strong>¥{{ Number(course.price).toFixed(0) }}</strong>
          <del v-if="course.originalPrice">¥{{ Number(course.originalPrice).toFixed(0) }}</del>
        </div>
        <span class="students">{{ course.studentCount }} 人学习</span>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.course-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--line);
  box-shadow: var(--shadow-xs);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.course-card:hover {
  transform: translateY(-4px);
  border-color: var(--brand-line);
  box-shadow: var(--shadow-md);
}

.course-cover {
  height: 160px;
  background: linear-gradient(135deg, var(--brand), var(--accent));
  background-size: cover;
  background-position: center;
  position: relative;
  overflow: hidden;
  transition: transform 0.4s ease;
}

.course-card:hover .course-cover {
  transform: scale(1.04);
}

.course-stage {
  position: absolute;
  right: 12px;
  top: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  background: rgba(17, 24, 39, 0.45);
  backdrop-filter: blur(6px);
}

.course-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 16px;
}

.course-subject {
  color: var(--brand);
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
}

h3 {
  margin: 0 0 8px;
  min-height: 44px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-strong);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

p {
  margin: 0 0 10px;
  color: var(--text-muted);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-teacher {
  font-size: 12px;
  color: var(--text-faint);
  margin-bottom: 14px;
}

.course-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
}

.price strong {
  color: var(--brand);
  font-size: 21px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.price del {
  color: var(--text-faint);
  font-size: 12px;
  margin-left: 6px;
}

.students {
  color: var(--text-faint);
  font-size: 12px;
  white-space: nowrap;
}
</style>
