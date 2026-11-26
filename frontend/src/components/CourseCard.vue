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
  display: block;
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  border: 1px solid #eef0f4;
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.12);
}

.course-cover {
  height: 160px;
  background: linear-gradient(135deg, #e60012, #ff7a00);
  background-size: cover;
  background-position: center;
  position: relative;
}

.course-stage {
  position: absolute;
  right: 12px;
  top: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: #fff;
  background: rgba(17, 24, 39, 0.45);
}

.course-body {
  padding: 16px;
}

.course-subject {
  color: #e60012;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 6px;
}

h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #111827;
  line-height: 1.4;
  min-height: 44px;
}

p {
  margin: 0 0 10px;
  color: #6b7280;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-teacher {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 14px;
}

.course-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

.price strong {
  color: #e60012;
  font-size: 20px;
}

.price del {
  color: #9ca3af;
  font-size: 12px;
  margin-left: 6px;
}

.students {
  color: #9ca3af;
  font-size: 12px;
}
</style>
