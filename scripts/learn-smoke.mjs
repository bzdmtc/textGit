// 模块 5 学习链路 API 冒烟：购课 → 学习页 → 进度 → 笔记 → 收藏 → 评价 → 审核
const BASE = 'http://localhost:8080/api/v1'

let failures = 0
function ok(label, condition, detail) {
  if (condition) {
    console.log(`OK   ${label}${detail === undefined ? '' : ' :: ' + JSON.stringify(detail)}`)
  } else {
    failures++
    console.error(`FAIL ${label}${detail === undefined ? '' : ' :: ' + JSON.stringify(detail)}`)
  }
}

async function api(method, path, body, token, expectOk = true) {
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await response.text()
  let data
  try {
    data = JSON.parse(text)
  } catch {
    data = text
  }
  if (expectOk && !response.ok) {
    throw new Error(`${method} ${path} -> HTTP ${response.status}: ${text.slice(0, 400)}`)
  }
  return { status: response.status, data }
}

async function login(account, password) {
  const response = await api('POST', '/auth/login', { account, password })
  return response.data.data.accessToken
}

async function main() {
  const admin = await login('admin', 'Admin@123')
  const student = await login('student', 'Student@123')
  const teacher = await login('teacher', 'Teacher@123')

  // 购课开通
  const order = (await api('POST', '/orders', { courseIds: [1] }, student)).data.data
  const paid = (await api('POST', `/payments/${order.orderNo}/pay`, { channel: 'MOCK_ALIPAY' }, student)).data.data
  ok('购课后订单已支付', paid.order.status === 'PAID', paid.order.orderNo)

  // 学习页与访问控制
  const learn = (await api('GET', '/learn/courses/1', null, student)).data.data
  const lesson = learn.chapters[0]?.lessons[0]
  ok('已购学员可进入学习页', Boolean(lesson), { chapters: learn.chapters.length, lesson: lesson?.title })
  const denied = await api('GET', '/learn/courses/1', null, teacher, false)
  ok('未购用户被拒绝学习', denied.status === 400 && denied.data.code === 30009,
    { status: denied.status, code: denied.data.code })

  // 进度上报
  const half = Math.floor((lesson.durationSec || 0) / 2)
  const partial = (await api('POST', `/learn/courses/1/lessons/${lesson.id}/progress`,
    { watchedSec: half, completed: false }, student)).data.data
  ok('进度上报保存观看时长', partial.watchedSec === half && partial.completed === false, partial)
  const finished = (await api('POST', `/learn/courses/1/lessons/${lesson.id}/progress`,
    { watchedSec: lesson.durationSec, completed: true }, student)).data.data
  ok('标记完成并重算课程进度', finished.completed === true && Number(finished.courseProgressPercent) > 0,
    finished)
  const learnAfter = (await api('GET', '/learn/courses/1', null, student)).data.data
  ok('学习页进度已同步', Number(learnAfter.progressPercent) === Number(finished.courseProgressPercent)
    && learnAfter.chapters[0].lessons[0].completed === true,
  { progress: learnAfter.progressPercent })

  const secondLesson = learnAfter.chapters[0]?.lessons[1]
  if (secondLesson) {
    const auto = (await api('POST', `/learn/courses/1/lessons/${secondLesson.id}/progress`,
      { watchedSec: secondLesson.durationSec, completed: false }, student)).data.data
    ok('观看达 95% 自动完成', auto.completed === true && Number(auto.courseProgressPercent) > Number(finished.courseProgressPercent),
      auto)
  }

  // 笔记
  const note = (await api('POST', `/learn/courses/1/lessons/${lesson.id}/notes`,
    { content: '马原框架：物质—意识—实践' }, student)).data.data
  ok('新增笔记', note.id >= 0 && note.content.includes('马原'), note.id)
  const noteList = (await api('GET', `/learn/courses/1/lessons/${lesson.id}/notes`, null, student)).data.data
  ok('笔记列表可见', noteList.some((item) => item.id === note.id), noteList.length)
  const noteUpdated = (await api('PUT', `/learn/notes/${note.id}`,
    { content: '马原框架：物质、意识、实践（已更新）' }, student)).data.data
  ok('更新笔记', noteUpdated.content.includes('已更新'), noteUpdated.content)
  await api('DELETE', `/learn/notes/${note.id}`, null, student)
  const noteAfter = (await api('GET', `/learn/courses/1/lessons/${lesson.id}/notes`, null, student)).data.data
  ok('删除笔记', !noteAfter.some((item) => item.id === note.id), noteAfter.length)

  // 收藏
  await api('POST', '/learn/favorites/2', null, student)
  const favoriteStatus = (await api('GET', '/learn/favorites/2/status', null, student)).data.data
  ok('收藏课程', favoriteStatus.favorited === true, favoriteStatus)
  const favoriteList = (await api('GET', '/learn/favorites', null, student)).data.data
  ok('收藏列表包含课程', favoriteList.some((item) => item.courseId === 2), favoriteList.map((item) => item.courseId))
  const favoriteOther = await api('GET', '/learn/favorites/2/status', null, teacher)
  ok('收藏按用户隔离', favoriteOther.data.data.favorited === false)
  await api('DELETE', '/learn/favorites/2', null, student)
  ok('取消收藏', (await api('GET', '/learn/favorites/2/status', null, student)).data.data.favorited === false)

  // 评价与审核
  const review = (await api('POST', '/learn/courses/1/reviews',
    { rating: 5, content: '课程结构清晰，讲义配套很实用。' }, student)).data.data
  ok('提交评价待审核', review.status === 'PENDING', { id: review.id, status: review.status })
  const publicBefore = (await api('GET', '/public/courses/1/reviews')).data.data
  ok('待审核评价不公开展示', publicBefore.items.length === 0, publicBefore.reviewCount)

  const adminPage = (await api('GET', '/admin/reviews?status=PENDING&page=1&size=20', null, admin)).data.data
  ok('后台待审核列表包含评价', adminPage.items.some((item) => item.id === review.id), adminPage.total)
  const approved = (await api('PUT', `/admin/reviews/${review.id}/status`,
    { status: 'APPROVED' }, admin)).data.data
  ok('管理员通过评价', approved.status === 'APPROVED', approved.id)
  const publicAfter = (await api('GET', '/public/courses/1/reviews')).data.data
  ok('通过后公开可见', publicAfter.items.some((item) => item.id === review.id)
    && publicAfter.reviewCount === 1 && Number(publicAfter.averageRating) === 5,
  { count: publicAfter.reviewCount, average: publicAfter.averageRating })
  // 课程详情走只读从库，评分校验改读主库的后台详情
  const adminCourse = (await api('GET', '/admin/courses/1', null, admin)).data.data
  ok('课程评分按审核评价重算', Number(adminCourse.course.rating) === 5, adminCourse.course.rating)

  // 修改评价后回到待审核
  const edited = (await api('POST', '/learn/courses/1/reviews',
    { rating: 4, content: '补充：习题解析还可以更详细。' }, student)).data.data
  ok('更新评价重新进入待审核', edited.id === review.id && edited.status === 'PENDING', edited.status)
  const publicReset = (await api('GET', '/public/courses/1/reviews')).data.data
  ok('更新后不再公开旧评价', publicReset.items.length === 0, publicReset.reviewCount)

  // 我的课程进度
  const myCourses = (await api('GET', '/my/courses', null, student)).data.data
  const enrolledCourse = myCourses.find((item) => item.courseId === 1)
  ok('我的课程展示学习进度', Number(enrolledCourse.progressPercent) > 0,
    enrolledCourse && enrolledCourse.progressPercent)

  // 后台菜单权限
  const me = (await api('GET', '/auth/me', null, admin)).data.data
  ok('管理员拥有评价审核菜单', me.menus.some((menu) => menu.path === '/admin/reviews')
    && Boolean(me.rights.review), me.rights.review)

  console.log(JSON.stringify({ orderNo: order.orderNo, reviewId: review.id }))
  if (failures > 0) {
    console.error(`${failures} failure(s)`)
    process.exit(1)
  }
  console.log('ALL OK')
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
