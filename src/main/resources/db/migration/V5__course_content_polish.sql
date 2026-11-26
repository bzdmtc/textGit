-- 模块 2 内容补全：为第二个政治章节补充课时

INSERT INTO course_lesson (chapter_id, title, video_url, duration_sec, is_trial, sort, status)
SELECT ch.id, N'毛中特核心考点串讲', 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4', 2400, 0, 1, 'ACTIVE'
FROM course_chapter ch JOIN course c ON c.id = ch.course_id
WHERE c.title = N'考研政治强化提升班'
  AND ch.title = N'毛泽东思想和中国特色社会主义理论体系'
  AND NOT EXISTS (SELECT 1 FROM course_lesson l WHERE l.chapter_id = ch.id);
