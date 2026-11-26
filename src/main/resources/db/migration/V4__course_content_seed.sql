-- 模块 2：课程与内容权限 + 演示数据

INSERT INTO sys_priv (module, method, name, type, menu_path, icon, sort, status)
SELECT * FROM (VALUES
    ('course','browse',N'课程管理','VIEW','/admin/courses','Reading',210,'ACTIVE'),
    ('course','create',N'新增课程','ACTION',NULL,NULL,211,'ACTIVE'),
    ('course','edit',N'编辑课程/章节课时','ACTION',NULL,NULL,212,'ACTIVE'),
    ('course','delete',N'删除课程','ACTION',NULL,NULL,213,'ACTIVE'),
    ('course','publish',N'上下架课程','ACTION',NULL,NULL,214,'ACTIVE'),
    ('teacher','browse',N'讲师管理','VIEW','/admin/teachers','User',220,'ACTIVE'),
    ('teacher','create',N'新增讲师','ACTION',NULL,NULL,221,'ACTIVE'),
    ('teacher','edit',N'编辑讲师','ACTION',NULL,NULL,222,'ACTIVE'),
    ('teacher','delete',N'删除讲师','ACTION',NULL,NULL,223,'ACTIVE'),
    ('article','browse',N'文章管理','VIEW','/admin/articles','Document',230,'ACTIVE'),
    ('article','create',N'新增文章','ACTION',NULL,NULL,231,'ACTIVE'),
    ('article','edit',N'编辑文章','ACTION',NULL,NULL,232,'ACTIVE'),
    ('article','delete',N'删除文章','ACTION',NULL,NULL,233,'ACTIVE'),
    ('article','publish',N'发布文章','ACTION',NULL,NULL,234,'ACTIVE'),
    ('material','browse',N'资料管理','VIEW','/admin/materials','Folder',240,'ACTIVE'),
    ('material','create',N'新增资料','ACTION',NULL,NULL,241,'ACTIVE'),
    ('material','edit',N'编辑资料','ACTION',NULL,NULL,242,'ACTIVE'),
    ('material','delete',N'删除资料','ACTION',NULL,NULL,243,'ACTIVE'),
    ('banner','browse',N'Banner 管理','VIEW','/admin/banners','Picture',250,'ACTIVE'),
    ('banner','create',N'新增 Banner','ACTION',NULL,NULL,251,'ACTIVE'),
    ('banner','edit',N'编辑 Banner','ACTION',NULL,NULL,252,'ACTIVE'),
    ('banner','delete',N'删除 Banner','ACTION',NULL,NULL,253,'ACTIVE'),
    ('file','upload',N'上传文件','ACTION',NULL,NULL,260,'ACTIVE')
) AS v(module, method, name, type, menu_path, icon, sort, status)
WHERE NOT EXISTS (SELECT 1 FROM sys_priv p WHERE p.module = v.module AND p.method = v.method);

-- 管理员自动补齐新权限
INSERT INTO sys_group_priv (group_id, priv_id)
SELECT g.id, p.id FROM sys_group g CROSS JOIN sys_priv p
WHERE g.code = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM sys_group_priv gp WHERE gp.group_id = g.id AND gp.priv_id = p.id);

-- 讲师：课程/文章/资料自有内容 + 上传
INSERT INTO sys_group_priv (group_id, priv_id)
SELECT g.id, p.id FROM sys_group g CROSS JOIN sys_priv p
WHERE g.code = 'TEACHER'
  AND (
        (p.module IN ('course','article','material') AND p.method IN ('browse','create','edit','delete','publish'))
     OR (p.module = 'file' AND p.method = 'upload')
     OR (p.module = 'teacher' AND p.method = 'browse')
  )
  AND NOT EXISTS (SELECT 1 FROM sys_group_priv gp WHERE gp.group_id = g.id AND gp.priv_id = p.id);

INSERT INTO teacher (name, title, avatar, subject, introduction, sort, status, owner_user_id) VALUES
(N'张老师', N'考研政治主讲', '/seed/teacher-1.svg', N'政治', N'深耕考研政治教学 10 年，擅长框架式教学与主观题答题模板。', 10, 'ACTIVE', 2),
(N'王老师', N'考研数学主讲', '/seed/teacher-2.svg', N'数学', N'考研数学一/二/三全覆盖，注重基础题型与计算能力训练。', 20, 'ACTIVE', 4),
(N'李老师', N'考研英语主讲', '/seed/teacher-3.svg', N'英语', N'英语一/英语二阅读与写作主讲，长难句拆解方法清晰。', 30, 'ACTIVE', NULL);

INSERT INTO course (title, subtitle, subject, stage, cover_url, description, price, original_price, rating, student_count, status, owner_user_id) VALUES
(N'考研政治强化提升班', N'框架梳理 + 主观题模板', N'政治', N'强化', '/seed/course-politics.svg', N'覆盖马原、毛中特、史纲、思修法基，配套章节练习与主观题批改思路。', 599.00, 899.00, 4.90, 1280, 'PUBLISHED', 2),
(N'考研英语一基础精讲', N'词汇、长难句、阅读起步', N'英语一', N'基础', '/seed/course-english.svg', N'从核心词汇与语法长难句开始，建立阅读和写作的基础能力。', 499.00, 799.00, 4.80, 960, 'PUBLISHED', 2),
(N'考研数学一强化班', N'高数、线代、概率强化', N'数学一', N'强化', '/seed/course-math.svg', N'按题型模块拆解，配合阶段测评和错题复盘。', 699.00, 999.00, 4.90, 1560, 'PUBLISHED', 4),
(N'计算机 408 专业课冲刺', N'数据结构、组成原理、操作系统、网络', N'计算机408', N'冲刺', '/seed/course-cs.svg', N'408 高频考点串讲、真题精讲与模拟套卷训练。', 899.00, 1299.00, 4.70, 680, 'PUBLISHED', 4);

INSERT INTO course_teacher (course_id, teacher_id, sort)
SELECT c.id, t.id, 1 FROM course c JOIN teacher t ON
    (c.title = N'考研政治强化提升班' AND t.name = N'张老师')
 OR (c.title = N'考研英语一基础精讲' AND t.name = N'李老师')
 OR (c.title = N'考研数学一强化班' AND t.name = N'王老师')
 OR (c.title = N'计算机 408 专业课冲刺' AND t.name = N'王老师');

INSERT INTO course_chapter (course_id, title, sort)
SELECT id, N'马克思主义基本原理', 1 FROM course WHERE title = N'考研政治强化提升班';
INSERT INTO course_chapter (course_id, title, sort)
SELECT id, N'毛泽东思想和中国特色社会主义理论体系', 2 FROM course WHERE title = N'考研政治强化提升班';
INSERT INTO course_chapter (course_id, title, sort)
SELECT id, N'核心词汇与长难句', 1 FROM course WHERE title = N'考研英语一基础精讲';
INSERT INTO course_chapter (course_id, title, sort)
SELECT id, N'高等数学强化', 1 FROM course WHERE title = N'考研数学一强化班';
INSERT INTO course_chapter (course_id, title, sort)
SELECT id, N'408 高频考点串讲', 1 FROM course WHERE title = N'计算机 408 专业课冲刺';

INSERT INTO course_lesson (chapter_id, title, video_url, duration_sec, is_trial, sort, status)
SELECT ch.id, N'导学：马原框架与复习方法', 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4', 720, 1, 1, 'ACTIVE'
FROM course_chapter ch JOIN course c ON c.id = ch.course_id
WHERE c.title = N'考研政治强化提升班' AND ch.title = N'马克思主义基本原理';
INSERT INTO course_lesson (chapter_id, title, video_url, duration_sec, is_trial, sort, status)
SELECT ch.id, N'唯物论核心考点', 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4', 1800, 0, 2, 'ACTIVE'
FROM course_chapter ch JOIN course c ON c.id = ch.course_id
WHERE c.title = N'考研政治强化提升班' AND ch.title = N'马克思主义基本原理';
INSERT INTO course_lesson (chapter_id, title, video_url, duration_sec, is_trial, sort, status)
SELECT ch.id, N'辩证法高频题型', 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4', 2100, 0, 3, 'ACTIVE'
FROM course_chapter ch JOIN course c ON c.id = ch.course_id
WHERE c.title = N'考研政治强化提升班' AND ch.title = N'马克思主义基本原理';
INSERT INTO course_lesson (chapter_id, title, video_url, duration_sec, is_trial, sort, status)
SELECT ch.id, N'核心词汇速记方法', 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4', 1500, 1, 1, 'ACTIVE'
FROM course_chapter ch JOIN course c ON c.id = ch.course_id
WHERE c.title = N'考研英语一基础精讲' AND ch.title = N'核心词汇与长难句';
INSERT INTO course_lesson (chapter_id, title, video_url, duration_sec, is_trial, sort, status)
SELECT ch.id, N'长难句拆解示例', 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4', 1800, 0, 2, 'ACTIVE'
FROM course_chapter ch JOIN course c ON c.id = ch.course_id
WHERE c.title = N'考研英语一基础精讲' AND ch.title = N'核心词汇与长难句';
INSERT INTO course_lesson (chapter_id, title, video_url, duration_sec, is_trial, sort, status)
SELECT ch.id, N'极限与连续题型精讲', 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4', 2400, 1, 1, 'ACTIVE'
FROM course_chapter ch JOIN course c ON c.id = ch.course_id
WHERE c.title = N'考研数学一强化班' AND ch.title = N'高等数学强化';
INSERT INTO course_lesson (chapter_id, title, video_url, duration_sec, is_trial, sort, status)
SELECT ch.id, N'数据结构高频考点', 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4', 2700, 1, 1, 'ACTIVE'
FROM course_chapter ch JOIN course c ON c.id = ch.course_id
WHERE c.title = N'计算机 408 专业课冲刺' AND ch.title = N'408 高频考点串讲';

INSERT INTO article_category (name, sort) VALUES
(N'考研资讯', 10),
(N'复习经验', 20),
(N'政策解读', 30);

INSERT INTO article (category_id, title, summary, cover_url, content_html, author, status, published_at, view_count, owner_user_id)
SELECT c.id, N'2027 考研全年复习时间轴', N'从基础到冲刺的阶段目标与复习重点。', '/seed/article-1.svg',
       N'<p>基础阶段以理解概念和建立框架为主，强化阶段开始专题训练，冲刺阶段以真题和模拟卷为主。</p><p>建议每周固定复盘一次，把错题和模糊知识点集中整理。</p>',
       N'张老师', 'PUBLISHED', SYSUTCDATETIME(), 128, 2
FROM article_category c WHERE c.name = N'考研资讯';
INSERT INTO article (category_id, title, summary, cover_url, content_html, author, status, published_at, view_count, owner_user_id)
SELECT c.id, N'英语阅读提分的三个关键习惯', N'精读、错题归因、长难句拆解。', '/seed/article-2.svg',
       N'<p>阅读提分不是单纯刷题，而是每篇错题都要归因：是词汇、语法、逻辑还是选项陷阱。</p>',
       N'李老师', 'PUBLISHED', SYSUTCDATETIME(), 86, 2
FROM article_category c WHERE c.name = N'复习经验';
INSERT INTO article (category_id, title, summary, cover_url, content_html, author, status, published_at, view_count, owner_user_id)
SELECT c.id, N'考研报名与现场确认注意事项', N'报名材料、时间节点与常见问题。', '/seed/article-3.svg',
       N'<p>请提前准备身份证、学历证明和电子照片，关注目标院校研招网通知。</p>',
       N'张老师', 'PUBLISHED', SYSUTCDATETIME(), 64, 2
FROM article_category c WHERE c.name = N'政策解读';

INSERT INTO material (title, subject, file_url, file_size, download_count, status, owner_user_id) VALUES
(N'考研数学公式速查手册', N'数学', '/seed/kaoyan-notes.txt', 2048, 320, 'ACTIVE', 2),
(N'考研英语高频词汇表', N'英语', '/seed/kaoyan-notes.txt', 1536, 210, 'ACTIVE', 2);

INSERT INTO banner (title, image_url, link_url, sort, status, start_at, end_at) VALUES
(N'2027 考研全程班', '/seed/banner-1.svg', '/courses', 10, 'ACTIVE', NULL, NULL),
(N'名师课程限时优惠', '/seed/banner-2.svg', '/courses', 20, 'ACTIVE', NULL, NULL);
