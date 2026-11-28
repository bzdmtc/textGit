// 模块 7 院校与看板 API 冒烟：院校/专业/分数线/报录比 CRUD → 前台检索 → 看板统计 → 用户活跃 → 审计
const BASE = 'http://localhost:8080/api/v1'
const stamp = Date.now().toString().slice(-8)
const NAME = `SMOKE院校${stamp}`
const MAJOR = `SMOKE专业${stamp}`

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
    throw new Error(`${method} ${path} -> HTTP ${response.status}: ${text.slice(0, 500)}`)
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

  // ---------- 公开院校库 ----------
  const seedList = (await api('GET', '/public/universities?size=50')).data.data
  ok('公开院校列表包含演示数据', seedList.total >= 10 && seedList.items.some((item) => item.name === '北京大学'),
    { total: seedList.total })

  const options = (await api('GET', '/public/universities/options')).data.data
  ok('院校筛选选项来自数据库', options.provinces.includes('北京') && options.levels.length > 0,
    { provinces: options.provinces.length, levels: options.levels })

  const featured = (await api('GET', '/public/universities/featured')).data.data
  ok('首页精选院校', featured.length > 0 && featured.every((item) => item.featured), { count: featured.length })

  const seed = seedList.items.find((item) => item.name === '北京大学')
  const seedDetail = (await api('GET', `/public/universities/${seed.id}`)).data.data
  ok('院校详情含专业/分数线/报录比',
    seedDetail.majors.length > 0 && seedDetail.scoreLines.length > 0 && seedDetail.admissionStats.length > 0,
    {
      majors: seedDetail.majors.length,
      scoreLines: seedDetail.scoreLines.length,
      stats: seedDetail.admissionStats.length,
    })

  // ---------- 权限隔离 ----------
  const teacherDenied = await api('GET', '/admin/universities', undefined, teacher, false)
  const studentDenied = await api('GET', '/admin/universities', undefined, student, false)
  ok('讲师/学员不能访问院校管理', teacherDenied.status === 403 && studentDenied.status === 403,
    { teacher: teacherDenied.status, student: studentDenied.status })

  // ---------- 后台院校 CRUD ----------
  const created = (await api('POST', '/admin/universities', {
    name: NAME,
    province: '上海',
    city: '上海',
    level: '211',
    type: '理工',
    website: 'https://smoke.example.com',
    introduction: 'SMOKE 自动化测试院校',
    featured: false,
    sort: 0,
  }, admin)).data.data
  ok('新增院校', created.id > 0 && created.name === NAME, created.id)

  const duplicate = await api('POST', '/admin/universities', {
    name: NAME,
    province: '上海',
  }, admin, false)
  ok('院校名称重复被拒绝', duplicate.status === 400 && duplicate.data.code === 10003, duplicate.data.message)

  const major = (await api('POST', `/admin/universities/${created.id}/majors`, {
    name: MAJOR,
    code: '999999',
    degreeType: '学硕',
    duration: '3年',
    tuition: 8000,
    examSubjects: '政治、英语一、数学一、专业课',
    sort: 10,
  }, admin)).data.data
  ok('新增专业', major.id > 0 && major.universityId === created.id)

  const majorDuplicate = await api('POST', `/admin/universities/${created.id}/majors`, {
    name: MAJOR,
    degreeType: '学硕',
  }, admin, false)
  ok('同院校同专业同类型重复被拒绝', majorDuplicate.status === 400 && majorDuplicate.data.code === 10003)

  const line = (await api('POST', `/admin/universities/${created.id}/score-lines`, {
    majorId: major.id,
    year: 2025,
    political: 60,
    english: 60,
    math: 90,
    majorScore: 90,
    total: 300,
  }, admin)).data.data
  ok('新增分数线', line.id > 0 && line.total === 300)

  const lineDuplicate = await api('POST', `/admin/universities/${created.id}/score-lines`, {
    majorId: major.id,
    year: 2025,
    total: 310,
  }, admin, false)
  ok('同专业同年分数线重复被拒绝', lineDuplicate.status === 400 && lineDuplicate.data.code === 10003)

  const lineUpdated = (await api('PUT', `/admin/universities/${created.id}/score-lines/${line.id}`, {
    majorId: major.id,
    year: 2025,
    political: 60,
    english: 60,
    math: 95,
    majorScore: 95,
    total: 315,
  }, admin)).data.data
  ok('编辑分数线', lineUpdated.total === 315 && lineUpdated.math === 95)

  const stat = (await api('POST', `/admin/universities/${created.id}/admission-stats`, {
    majorId: major.id,
    year: 2025,
    applicants: 100,
    admitted: 10,
    minScore: 320,
  }, admin)).data.data
  ok('新增报录比且服务端计算', Number(stat.ratio) === 10, stat.ratio)

  const statUpdated = (await api('PUT', `/admin/universities/${created.id}/admission-stats/${stat.id}`, {
    majorId: major.id,
    year: 2025,
    applicants: 120,
    admitted: 10,
    minScore: 325,
  }, admin)).data.data
  ok('编辑报录比后重算', Number(statUpdated.ratio) === 12, statUpdated.ratio)

  const adminDetail = (await api('GET', `/admin/universities/${created.id}`, undefined, admin)).data.data
  ok('后台详情聚合专业/分数线/报录比',
    adminDetail.majors.some((item) => item.id === major.id)
    && adminDetail.scoreLines.some((item) => item.id === line.id)
    && adminDetail.admissionStats.some((item) => item.id === stat.id))

  // ---------- 前台可见性与状态 ----------
  const publicDetail = (await api('GET', `/public/universities/${created.id}`)).data.data
  ok('启用院校前台可见', publicDetail.university.id === created.id && publicDetail.majors.length === 1)

  const keywordPage = (await api('GET', `/public/universities?keyword=${encodeURIComponent(NAME)}&size=20`)).data.data
  ok('前台关键词检索命中', keywordPage.total === 1 && keywordPage.items[0].id === created.id)

  await api('PUT', `/admin/universities/${created.id}`, {
    name: NAME,
    province: '上海',
    city: '上海',
    level: '211',
    type: '理工',
    introduction: 'SMOKE 自动化测试院校（已更新）',
    featured: true,
    sort: 0,
  }, admin)
  const featuredAfter = (await api('GET', '/public/universities/featured')).data.data
  ok('设为精选后进入首页推荐', featuredAfter.some((item) => item.id === created.id))

  await api('PUT', `/admin/universities/${created.id}/status`, { status: 'DISABLED' }, admin)
  const hiddenDetail = await api('GET', `/public/universities/${created.id}`, undefined, undefined, false)
  const hiddenList = (await api('GET', `/public/universities?keyword=${encodeURIComponent(NAME)}`)).data.data
  ok('停用院校前台不可见',
    hiddenDetail.status === 400 && hiddenDetail.data.code === 10002 && hiddenList.total === 0,
    { detailStatus: hiddenDetail.status, code: hiddenDetail.data.code, total: hiddenList.total })

  await api('PUT', `/admin/universities/${created.id}/status`, { status: 'ACTIVE' }, admin)

  // ---------- 数据看板 ----------
  const dashboard = (await api('GET', '/admin/stats/dashboard?days=7', undefined, admin)).data.data
  const lastPoint = dashboard.trend[dashboard.trend.length - 1]
  ok('看板汇总与趋势',
    dashboard.days === 7 && dashboard.trend.length === 7
    && typeof dashboard.summary.totalUsers === 'number'
    && dashboard.summary.paidOrders <= dashboard.summary.totalOrders
    && lastPoint.date === dashboard.today,
    { today: dashboard.today, trend: dashboard.trend.length, paidOrders: dashboard.summary.paidOrders })

  const ordersPage = (await api('GET', '/admin/orders?page=1&size=1', undefined, admin)).data.data
  ok('看板订单总数与订单管理一致', dashboard.summary.totalOrders === ordersPage.total,
    { dashboard: dashboard.summary.totalOrders, orders: ordersPage.total })

  ok('看板含热销课程与状态分布',
    Array.isArray(dashboard.hotCourses) && Array.isArray(dashboard.orderStatus),
    { hotCourses: dashboard.hotCourses.length, statuses: dashboard.orderStatus.length })

  // ---------- 用户管理完善 ----------
  const activeUsers = (await api('GET', '/admin/users?status=ACTIVE&size=100', undefined, admin)).data.data
  ok('用户状态筛选', activeUsers.items.every((item) => item.status === 'ACTIVE'),
    { total: activeUsers.total })

  const me = (await api('GET', '/auth/me', undefined, admin)).data.data
  const activity = (await api('GET', `/admin/users/${me.user.id}/activity`, undefined, admin)).data.data
  ok('用户活跃数据', activity.userId === me.user.id
    && typeof activity.paidOrderCount === 'number'
    && typeof activity.courseCount === 'number'
    && typeof activity.practiceCount === 'number',
  activity)

  // ---------- 审计 ----------
  const targetTypes = (await api('GET', '/admin/audit-logs/target-types', undefined, admin)).data.data
  const audits = (await api('GET', `/admin/audit-logs?targetType=UNIVERSITY&action=UNIVERSITY&size=50`, undefined, admin))
    .data.data
  ok('审计日志记录院校变更',
    targetTypes.includes('UNIVERSITY')
    && audits.items.some((item) => item.targetId === String(created.id)),
    { types: targetTypes, hits: audits.items.length })

  const filtered = (await api('GET', `/admin/audit-logs?targetType=UNIVERSITY_MAJOR&size=50`, undefined, admin))
    .data.data
  ok('审计日志按目标类型筛选', filtered.items.every((item) => item.targetType === 'UNIVERSITY_MAJOR'))

  console.log('\n提示：SMOKE 院校已停用为 ACTIVE 状态，可运行 scripts/reset-university-data.sql 清理。')
  if (failures > 0) {
    console.error(`\n${failures} 项检查失败`)
    process.exit(1)
  }
  console.log('\n全部检查通过')
}

main().catch((error) => {
  console.error(error)
  process.exit(1)
})
