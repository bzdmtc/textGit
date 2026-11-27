// 模块 6 题库与考试 API 冒烟：后台建题/组卷/发布 → 章节练习即时判题 → 错题本 → 错题重做 → 限时套卷判分 → 权限
const BASE = 'http://localhost:8080/api/v1'
const stamp = Date.now().toString().slice(-8)
const SUBJECT = `自动化测试-${stamp}`

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

function wrongLabel(question, correct) {
  const label = question.options.map((item) => item.label).find((item) => !correct.includes(item))
  return label || (correct === 'A' ? 'B' : 'A')
}

async function main() {
  const admin = await login('admin', 'Admin@123')
  const student = await login('student', 'Student@123')
  const teacher = await login('teacher', 'Teacher@123')

  // ---------- 后台建题 ----------
  const optionSets = {
    single1: [{ label: 'A', content: '2', isCorrect: true }, { label: 'B', content: '3' }, { label: 'C', content: '4' }, { label: 'D', content: '5' }],
    single2: [{ label: 'A', content: '1' }, { label: 'B', content: '2', isCorrect: true }, { label: 'C', content: '3' }, { label: 'D', content: '4' }],
    multiple: [{ label: 'A', content: '甲', isCorrect: true }, { label: 'B', content: '乙' }, { label: 'C', content: '丙', isCorrect: true }, { label: 'D', content: '丁' }],
    judge: [{ label: 'A', content: '正确', isCorrect: true }, { label: 'B', content: '错误' }],
  }
  const q1 = (await api('POST', '/admin/questions', {
    subject: SUBJECT, chapter: '第一章', type: 'SINGLE', stem: `SMOKE ${stamp} 单选题一：1+1=?`,
    analysis: '基础加法：1+1=2。', difficulty: 1, options: optionSets.single1,
  }, admin)).data.data
  const q2 = (await api('POST', '/admin/questions', {
    subject: SUBJECT, chapter: '第一章', type: 'SINGLE', stem: `SMOKE ${stamp} 单选题二：1+1 的正确答案是？`,
    analysis: '本题正确选项为 A。', difficulty: 2, options: optionSets.single2,
  }, admin)).data.data
  const q3 = (await api('POST', '/admin/questions', {
    subject: SUBJECT, chapter: '第一章', type: 'MULTIPLE', stem: `SMOKE ${stamp} 多选题：请选择甲和丙`,
    analysis: '甲、丙为正确选项。', difficulty: 2, options: optionSets.multiple,
  }, admin)).data.data
  const q4 = (await api('POST', '/admin/questions', {
    subject: SUBJECT, chapter: '第一章', type: 'JUDGE', stem: `SMOKE ${stamp} 判断题：地球是圆的。`,
    analysis: '正确。', difficulty: 1, options: optionSets.judge,
  }, admin)).data.data
  ok('后台创建单选/多选/判断题', [q1, q2, q3, q4].every((item) => item.id > 0)
    && q1.correctAnswer === 'A' && q2.correctAnswer === 'B'
    && q3.correctAnswer === 'AC' && q4.correctAnswer === 'A',
  { q1: q1.correctAnswer, q2: q2.correctAnswer, q3: q3.correctAnswer })

  const updated = (await api('PUT', `/admin/questions/${q1.id}`, {
    subject: SUBJECT, chapter: '第一章', type: 'SINGLE', stem: `SMOKE ${stamp} 单选题一（已更新）：1+1=?`,
    analysis: '基础加法：1+1=2。', difficulty: 1, options: optionSets.single1,
  }, admin)).data.data
  ok('后台编辑题目', updated.stem.includes('已更新') && updated.correctAnswer === 'A', updated.stem)

  // ---------- 题库首页 ----------
  const overview = (await api('GET', '/practice/overview', null, student)).data.data
  const subjectRow = overview.subjects.find((item) => item.subject === SUBJECT)
  ok('题库首页展示科目/章节统计', Boolean(subjectRow) && subjectRow.questionCount === 4
    && subjectRow.chapters[0].chapter === '第一章', subjectRow)
  ok('题库首页展示已发布试卷', overview.papers.length >= 4 && overview.papers.every((item) => item.status === 'PUBLISHED'),
    overview.papers.map((item) => item.title))

  // ---------- 章节练习：即时判题 ----------
  const practice = (await api('POST', '/practice/sessions',
    { subject: SUBJECT, chapter: '第一章', count: 10 }, student)).data.data
  ok('创建章节练习并生成题目', practice.status === 'IN_PROGRESS' && practice.mode === 'PRACTICE'
    && practice.questions.length === 4 && Number(practice.totalScore) === 20, {
    id: practice.recordId, total: practice.totalScore, count: practice.questions.length,
  })
  ok('作答前不泄露答案与解析', practice.questions.every((item) => item.correctAnswer === undefined
    && item.analysis === undefined && item.options.every((option) => option.isCorrect === undefined)))

  const invalid = await api('PUT', `/exams/${practice.recordId}/answers`,
    { questionId: q1.id, answer: 'Z' }, student, false)
  ok('非法选项被拒绝', invalid.status === 400 && invalid.data.code === 50011,
    { status: invalid.status, code: invalid.data.code })

  const fb1 = (await api('PUT', `/exams/${practice.recordId}/answers`,
    { questionId: q1.id, answer: 'A' }, student)).data.data
  ok('练习答对即时判题', fb1.isCorrect === true && Number(fb1.earnedScore) === 5
    && fb1.correctAnswer === 'A' && Boolean(fb1.analysis), fb1)

  const fb2 = (await api('PUT', `/exams/${practice.recordId}/answers`,
    { questionId: q2.id, answer: wrongLabel(q2, q2.correctAnswer) }, student)).data.data
  ok('练习答错即时判题并给出解析', fb2.isCorrect === false && Number(fb2.earnedScore) === 0
    && fb2.correctAnswer === 'B' && Boolean(fb2.analysis), fb2)

  const fb3 = (await api('PUT', `/exams/${practice.recordId}/answers`,
    { questionId: q3.id, answer: 'B' }, student)).data.data
  ok('多选题部分选对仍判错', fb3.isCorrect === false && Number(fb3.earnedScore) === 0, fb3)

  // 错题自动收集
  let wrongPage = (await api('GET', `/wrong-questions?subject=${encodeURIComponent(SUBJECT)}&page=1&size=20`,
    null, student)).data.data
  const wrongQ2 = wrongPage.items.find((item) => item.questionId === q2.id)
  const wrongQ3 = wrongPage.items.find((item) => item.questionId === q3.id)
  ok('答错自动进入错题本', Boolean(wrongQ2) && Boolean(wrongQ3)
    && wrongQ2.wrongCount >= 1 && wrongQ2.correctAnswer === 'B', {
    count: wrongPage.total, q2: wrongQ2?.wrongCount,
  })

  // 不筛选科目也能错题重做
  const redoAll = (await api('POST', '/practice/sessions',
    { source: 'WRONG', count: 10 }, student)).data.data
  ok('错题重做可不限科目', redoAll.questions.some((item) => item.questionId === q2.id)
    && redoAll.questions.some((item) => item.questionId === q3.id),
  redoAll.questions.map((item) => item.questionId))
  await api('POST', `/exams/${redoAll.recordId}/submit`, null, student)

  // 标记掌握
  await api('PUT', `/wrong-questions/${wrongQ2.id}/mastered`, { mastered: true }, student)
  wrongPage = (await api('GET', `/wrong-questions?subject=${encodeURIComponent(SUBJECT)}&mastered=true&page=1&size=20`,
    null, student)).data.data
  ok('错题标记掌握', wrongPage.items.some((item) => item.questionId === q2.id), wrongPage.total)

  // 错题重做：只含未掌握错题
  const redo = (await api('POST', '/practice/sessions',
    { subject: SUBJECT, source: 'WRONG', count: 10 }, student)).data.data
  ok('错题重做只选取未掌握错题', redo.questions.some((item) => item.questionId === q3.id)
    && !redo.questions.some((item) => item.questionId === q2.id),
  redo.questions.map((item) => item.questionId))
  const redoFeedback = (await api('PUT', `/exams/${redo.recordId}/answers`,
    { questionId: q3.id, answer: 'AC' }, student)).data.data
  ok('错题重做答对自动标记掌握', redoFeedback.isCorrect === true, redoFeedback.isCorrect)
  wrongPage = (await api('GET', `/wrong-questions?subject=${encodeURIComponent(SUBJECT)}&mastered=true&page=1&size=20`,
    null, student)).data.data
  ok('重做答对后错题状态更新', wrongPage.items.some((item) => item.questionId === q3.id), wrongPage.total)
  const redoResult = (await api('POST', `/exams/${redo.recordId}/submit`, null, student)).data.data
  ok('错题重做交卷', redoResult.record.status === 'SUBMITTED'
    && Number(redoResult.score) === 5 && redoResult.correctCount === 1, {
    score: redoResult.score, correct: redoResult.correctCount,
  })

  // 练习交卷与成绩
  const practiceResult = (await api('POST', `/exams/${practice.recordId}/submit`, null, student)).data.data
  ok('练习交卷计算成绩', practiceResult.record.status === 'SUBMITTED'
    && Number(practiceResult.score) === 5 && Number(practiceResult.totalScore) === 20
    && practiceResult.correctCount === 1 && practiceResult.wrongCount === 2
    && practiceResult.unansweredCount === 1, {
    score: practiceResult.score, total: practiceResult.totalScore,
    correct: practiceResult.correctCount, wrong: practiceResult.wrongCount, unanswered: practiceResult.unansweredCount,
  })
  ok('成绩报告包含题目解析与正确答案', practiceResult.questions.length === 4
    && practiceResult.questions.every((item) => Boolean(item.correctAnswer) && Boolean(item.analysis)))
  const submittedAgain = await api('PUT', `/exams/${practice.recordId}/answers`,
    { questionId: q1.id, answer: 'B' }, student, false)
  ok('已交卷后不可继续作答', submittedAgain.status === 400 && submittedAgain.data.code === 50008,
    submittedAgain.data.code)
  const resultAgain = (await api('GET', `/exams/${practice.recordId}/result`, null, student)).data.data
  ok('按记录查询成绩', resultAgain.record.id === practice.recordId && resultAgain.questions.length === 4)

  // ---------- 限时套卷 ----------
  const paperTitle = `SMOKE ${stamp} 模拟试卷`
  const paperDetail = (await api('POST', '/admin/papers', {
    title: paperTitle, type: 'MOCK', subject: SUBJECT, chapter: '第一章',
    description: '自动化冒烟试卷', durationMin: 5,
    questions: [
      { questionId: q1.id, score: 10 },
      { questionId: q3.id, score: 15 },
      { questionId: q4.id, score: 5 },
    ],
  }, admin)).data.data
  const paperId = paperDetail.paper.id
  ok('后台组卷计算总分', Number(paperDetail.paper.totalScore) === 30
    && paperDetail.questions.length === 3 && paperDetail.paper.status === 'DRAFT',
  { id: paperId, total: paperDetail.paper.totalScore })

  const published = (await api('PUT', `/admin/papers/${paperId}/status`,
    { status: 'PUBLISHED' }, admin)).data.data
  ok('发布试卷', published.status === 'PUBLISHED', published.status)
  const overviewAfterPublish = (await api('GET', '/practice/overview', null, student)).data.data
  ok('题库首页出现新发布试卷', overviewAfterPublish.papers.some((item) => item.id === paperId))

  const exam = (await api('POST', '/exams', { paperId }, student)).data.data
  ok('开始限时考试', exam.mode === 'EXAM' && exam.status === 'IN_PROGRESS'
    && exam.questions.length === 3 && exam.remainingSeconds > 0 && exam.remainingSeconds <= 300,
  { id: exam.recordId, remaining: exam.remainingSeconds })
  ok('考试中不泄露答案', exam.questions.every((item) => item.correctAnswer === undefined))

  const examFb = (await api('PUT', `/exams/${exam.recordId}/answers`,
    { questionId: q1.id, answer: 'A' }, student)).data.data
  ok('考试作答只保存不判题', examFb.answer === 'A' && examFb.isCorrect === null
    && examFb.correctAnswer === null, examFb)
  await api('PUT', `/exams/${exam.recordId}/answers`, { questionId: q4.id, answer: 'A' }, student)

  const examResult = (await api('POST', `/exams/${exam.recordId}/submit`, null, student)).data.data
  ok('套卷服务端判分', Number(examResult.score) === 15 && Number(examResult.totalScore) === 30
    && examResult.correctCount === 2 && examResult.wrongCount === 0
    && examResult.unansweredCount === 1 && examResult.durationSeconds !== null, {
    score: examResult.score, correct: examResult.correctCount,
    wrong: examResult.wrongCount, unanswered: examResult.unansweredCount,
  })
  ok('套卷成绩保留题目快照', examResult.questions.length === 3
    && examResult.questions.every((item) => Boolean(item.stem) && Boolean(item.correctAnswer)))

  const records = (await api('GET', '/exams/records?page=1&size=20', null, student)).data.data
  ok('我的练习/考试记录', records.items.some((item) => item.id === exam.recordId)
    && records.items.some((item) => item.id === practice.recordId), records.total)

  const stats = (await api('GET', '/practice/overview', null, student)).data.data.stats
  ok('练习统计更新', stats.attemptCount >= 3 && Number(stats.correctRate) >= 0
    && stats.wrongCount >= 0, stats)

  // ---------- 数据隔离与权限 ----------
  const otherRecord = await api('GET', `/exams/${exam.recordId}`, null, admin, false)
  ok('答题记录按用户隔离', otherRecord.status === 400 && otherRecord.data.code === 50007,
    { status: otherRecord.status, code: otherRecord.data.code })

  const studentAdmin = await api('GET', '/admin/questions', null, student, false)
  ok('学员不能访问题库后台', studentAdmin.status === 403, studentAdmin.status)

  const teacherPage = await api('GET', '/admin/questions?page=1&size=10', null, teacher)
  ok('讲师拥有题库权限（仅本人数据）', teacherPage.status === 200
    && teacherPage.data.data.items.every((item) => item.ownerUserId !== 1), teacherPage.data.data.total)

  const teacherQuestion = (await api('POST', '/admin/questions', {
    subject: SUBJECT, chapter: '讲师章节', type: 'JUDGE',
    stem: `SMOKE ${stamp} 讲师创建的判断题`,
    analysis: '讲师自有题目。', difficulty: 1,
    options: optionSets.judge,
  }, teacher)).data.data
  ok('讲师创建自有题目', teacherQuestion.ownerUserId !== 1 && teacherQuestion.correctAnswer === 'A',
    teacherQuestion.ownerUserId)

  const teacherEditAdmin = await api('PUT', `/admin/questions/${q1.id}`, {
    subject: SUBJECT, chapter: '第一章', type: 'SINGLE', stem: '越权修改', difficulty: 1,
    options: optionSets.single1,
  }, teacher, false)
  ok('讲师不能编辑他人题目', teacherEditAdmin.status === 403, teacherEditAdmin.status)

  const teacherEditOwn = (await api('PUT', `/admin/questions/${teacherQuestion.id}`, {
    subject: SUBJECT, chapter: '讲师章节', type: 'JUDGE',
    stem: `SMOKE ${stamp} 讲师题目（已更新）`,
    analysis: '讲师自有题目。', difficulty: 2, options: optionSets.judge,
  }, teacher)).data.data
  ok('讲师可编辑自有题目', teacherEditOwn.difficulty === 2 && teacherEditOwn.stem.includes('已更新'))

  // 停用保护：被试卷引用的题目不能停用
  const disableUsed = await api('PUT', `/admin/questions/${q1.id}/status`, { status: 'DISABLED' }, admin, false)
  ok('被试卷引用的题目不能停用', disableUsed.status === 400 && disableUsed.data.code === 50003,
    disableUsed.data.code)

  const extra = (await api('POST', '/admin/questions', {
    subject: SUBJECT, chapter: '第一章', type: 'JUDGE',
    stem: `SMOKE ${stamp} 待停用判断题`,
    analysis: '用于验证停用。', difficulty: 1, options: optionSets.judge,
  }, admin)).data.data
  const disabled = (await api('PUT', `/admin/questions/${extra.id}/status`, { status: 'DISABLED' }, admin)).data.data
  ok('停用未引用题目', disabled.status === 'DISABLED', disabled.status)

  // 下架试卷后不可再开考
  await api('PUT', `/admin/papers/${paperId}/status`, { status: 'OFF' }, admin)
  const startOff = await api('POST', '/exams', { paperId }, student, false)
  ok('下架试卷不可开考', startOff.status === 400 && startOff.data.code === 50005,
    { status: startOff.status, code: startOff.data.code })
  const overviewAfterOff = (await api('GET', '/practice/overview', null, student)).data.data
  ok('下架试卷不在题库首页展示', !overviewAfterOff.papers.some((item) => item.id === paperId))

  // 后台菜单
  const adminMe = (await api('GET', '/auth/me', null, admin)).data.data
  ok('管理员题库/试卷菜单与权限', adminMe.menus.some((menu) => menu.path === '/admin/questions')
    && adminMe.menus.some((menu) => menu.path === '/admin/papers')
    && Boolean(adminMe.rights.question) && Boolean(adminMe.rights.paper),
  { question: adminMe.rights.question, paper: adminMe.rights.paper })

  console.log(JSON.stringify({ subject: SUBJECT, paperId, practiceRecord: practice.recordId, examRecord: exam.recordId }))
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
