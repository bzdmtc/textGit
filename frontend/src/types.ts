export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface MenuItem {
  module: string
  method: string
  name: string
  path: string
  icon?: string | null
  sort: number
}

export interface UserProfile {
  id: number
  account: string
  realname?: string | null
  nickname?: string | null
  position?: string | null
  phone?: string | null
  email?: string | null
  avatar?: string | null
  groups: string[]
}

export interface MeResponse {
  user: UserProfile
  rights: Record<string, string[]>
  menus: MenuItem[]
  dataScope: string
  limited: boolean
  privVersion: number
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  me: MeResponse
}

export interface Group {
  id: number
  code: string
  name: string
  description?: string | null
  dataScope: string
  limited: boolean
  scopeType: string
  scopeId: number
  status: string
  sort: number
  userCount: number
}

export interface GroupUser {
  id: number
  account: string
  realname?: string | null
  nickname?: string | null
  position?: string | null
  status: string
  lastLoginAt?: string | null
}

export interface Priv {
  id: number
  module: string
  method: string
  name: string
  type: 'VIEW' | 'ACTION' | string
  parentId?: number | null
  menuPath?: string | null
  icon?: string | null
  sort: number
  status: string
}

export interface User {
  id: number
  account: string
  realname?: string | null
  nickname?: string | null
  position?: string | null
  phone?: string | null
  email?: string | null
  avatar?: string | null
  status: string
  privVersion: number
  lastLoginAt?: string | null
  groups: Group[]
}

export interface UserPermissionView {
  userId: number
  account: string
  rights: Record<string, string[]>
  menus: MenuItem[]
  dataScope: string
  limited: boolean
  privVersion: number
}

export interface AuditLog {
  id: number
  userId?: number | null
  action: string
  targetType: string
  targetId?: string | null
  beforeJson?: string | null
  afterJson?: string | null
  ip?: string | null
  createdAt: string
}

export interface RouteCheck {
  route: string
  database: string
  userCount?: number
}

export interface Teacher {
  id: number
  name: string
  title?: string | null
  avatar?: string | null
  subject?: string | null
  introduction?: string | null
  sort: number
  status: string
  ownerUserId?: number | null
}

export interface Course {
  id: number
  title: string
  subtitle?: string | null
  subject: string
  stage: string
  coverUrl?: string | null
  description?: string | null
  price: number
  originalPrice?: number | null
  rating: number
  studentCount: number
  status: string
  ownerUserId?: number | null
  createdAt?: string | null
  teachers: Teacher[]
}

export interface Lesson {
  id: number
  chapterId: number
  title: string
  videoUrl?: string | null
  durationSec: number
  isTrial: boolean
  sort: number
  status: string
}

export interface Chapter {
  id: number
  courseId: number
  title: string
  sort: number
  lessons: Lesson[]
}

export interface CourseDetail {
  course: Course
  chapters: Chapter[]
}

export interface ArticleCategory {
  id: number
  name: string
  sort: number
}

export interface Article {
  id: number
  categoryId?: number | null
  categoryName?: string | null
  title: string
  summary?: string | null
  coverUrl?: string | null
  contentHtml?: string | null
  author?: string | null
  status: string
  publishedAt?: string | null
  viewCount: number
  ownerUserId?: number | null
  createdAt?: string | null
}

export interface Material {
  id: number
  title: string
  subject?: string | null
  fileUrl: string
  fileSize: number
  downloadCount: number
  status: string
  ownerUserId?: number | null
}

export interface Banner {
  id: number
  title: string
  imageUrl: string
  linkUrl?: string | null
  sort: number
  status: string
  startAt?: string | null
  endAt?: string | null
}

export interface HomeData {
  banners: Banner[]
  hotCourses: Course[]
  teachers: Teacher[]
  latestArticles: Article[]
}

export interface Coupon {
  id: number
  name: string
  type: 'FIXED' | 'PERCENT' | string
  value: number
  minAmount: number
  totalQuantity: number
  remainingQuantity: number
  perUserLimit: number
  startAt: string
  endAt: string
  subject?: string | null
  description?: string | null
  status: string
}

export interface UserCoupon {
  id: number
  couponId: number
  name: string
  type: 'FIXED' | 'PERCENT' | string
  value: number
  minAmount: number
  subject?: string | null
  description?: string | null
  status: string
  receivedAt: string
  usedAt?: string | null
  startAt: string
  endAt: string
  orderId?: number | null
}

export interface OrderItem {
  id?: number | null
  courseId: number
  courseTitle: string
  coverUrl?: string | null
  subject?: string | null
  price: number
}

export interface Order {
  id: number
  orderNo: string
  userId: number
  userAccount?: string | null
  nickname?: string | null
  status: string
  totalAmount: number
  discountAmount: number
  payAmount: number
  userCouponId?: number | null
  couponName?: string | null
  orderSource: string
  payMethod?: string | null
  paidAt?: string | null
  expireAt?: string | null
  canceledAt?: string | null
  completedAt?: string | null
  createdAt?: string | null
  items: OrderItem[]
  refundStatus?: string | null
  refundReason?: string | null
}

export interface OrderPreview {
  items: OrderItem[]
  totalAmount: number
  discountAmount: number
  payAmount: number
  userCouponId?: number | null
  couponName?: string | null
  couponDescription?: string | null
}

export interface PaymentChannel {
  code: string
  name: string
}

export interface PaymentResult {
  order: Order
  channel: string
  transactionNo?: string | null
  paidAt?: string | null
  message: string
}

export interface Refund {
  id: number
  orderId: number
  orderNo: string
  userId: number
  userAccount?: string | null
  nickname?: string | null
  reason: string
  amount: number
  status: string
  adminNote?: string | null
  handledBy?: number | null
  handledAt?: string | null
  createdAt?: string | null
  orderStatus?: string | null
}

export interface MyCourse {
  id: number
  courseId: number
  courseTitle: string
  coverUrl?: string | null
  subject: string
  stage: string
  price: number
  enrolledAt: string
  progressPercent: number
  status: string
  orderId?: number | null
}

export interface PurchaseStatus {
  courseId: number
  enrolled: boolean
  pendingOrderNo?: string | null
  paidOrderNo?: string | null
}

export interface SeckillActivity {
  id: number
  courseId: number
  courseTitle: string
  courseCover?: string | null
  coursePrice: number
  seckillPrice: number
  totalStock: number
  availableStock: number
  redisStock?: number | null
  soldCount: number
  perUserLimit: number
  startAt: string
  endAt: string
  status: string
  phase: string
  createdAt?: string | null
}

export interface PublicSeckill {
  id: number
  courseId: number
  courseTitle: string
  courseSubtitle?: string | null
  courseCover?: string | null
  subject?: string | null
  coursePrice: number
  seckillPrice: number
  totalStock: number
  remainingStock: number
  soldCount: number
  perUserLimit: number
  startAt: string
  endAt: string
  phase: string
  serverTime: string
}

export interface SeckillClaimResult {
  activityId: number
  status: string
  message: string
  serverTime: string
}

export interface SeckillResult {
  activityId: number
  status: string
  orderNo?: string | null
  reason?: string | null
  phase: string
  serverTime: string
}

export interface SeckillRecord {
  id: number
  userId: number
  userAccount?: string | null
  orderNo?: string | null
  status: string
  reason?: string | null
  createdAt?: string | null
}

export interface LearnLesson {
  id: number
  title: string
  videoUrl?: string | null
  durationSec: number
  isTrial: boolean
  sort: number
  watchedSec: number
  completed: boolean
}

export interface LearnChapter {
  id: number
  title: string
  sort: number
  lessons: LearnLesson[]
}

export interface LearnCourse {
  courseId: number
  title: string
  subtitle?: string | null
  coverUrl?: string | null
  subject: string
  stage: string
  progressPercent: number
  lastLessonId?: number | null
  favorited: boolean
  myReview?: CourseReview | null
  chapters: LearnChapter[]
}

export interface ProgressResult {
  lessonId: number
  watchedSec: number
  completed: boolean
  courseProgressPercent: number
}

export interface NoteItem {
  id: number
  lessonId: number
  lessonTitle?: string | null
  content: string
  createdAt?: string | null
  updatedAt?: string | null
}

export interface FavoriteCourse {
  id: number
  courseId: number
  courseTitle: string
  coverUrl?: string | null
  subject: string
  stage: string
  price: number
  rating: number
  studentCount: number
  enrolled: boolean
  progressPercent: number
  favoritedAt?: string | null
}

export interface CourseReview {
  id: number
  courseId: number
  courseTitle?: string | null
  userId: number
  userAccount?: string | null
  userNickname?: string | null
  orderId?: number | null
  rating: number
  content: string
  status: string
  adminNote?: string | null
  reviewedAt?: string | null
  createdAt?: string | null
}

export interface ReviewSummary {
  courseId: number
  averageRating: number
  reviewCount: number
  items: CourseReview[]
}

// ===== 模块 6：题库与考试 =====

export interface PracticeChapter {
  chapter: string
  questionCount: number
}

export interface PracticeSubject {
  subject: string
  questionCount: number
  chapters: PracticeChapter[]
}

export interface PracticeStats {
  attemptCount: number
  averagePercent: number
  correctRate: number
  wrongCount: number
  masteredCount: number
}

export interface ExamRecord {
  id: number
  paperId?: number | null
  title: string
  mode: 'PRACTICE' | 'EXAM' | string
  source: 'NORMAL' | 'WRONG' | string
  subject?: string | null
  chapter?: string | null
  status: 'IN_PROGRESS' | 'SUBMITTED' | string
  score?: number | null
  totalScore: number
  correctCount: number
  wrongCount: number
  questionCount: number
  startedAt: string
  submittedAt?: string | null
  durationSeconds?: number | null
}

export interface Paper {
  id: number
  title: string
  type: 'CHAPTER' | 'PAST' | 'MOCK' | string
  subject: string
  chapter?: string | null
  description?: string | null
  totalScore: number
  durationMin: number
  status: string
  ownerUserId?: number | null
  questionCount: number
  createdAt?: string | null
}

export interface PracticeOverview {
  subjects: PracticeSubject[]
  papers: Paper[]
  stats: PracticeStats
  recentRecords: ExamRecord[]
}

export interface ExamOption {
  label: string
  content: string
}

export interface ExamQuestion {
  questionId: number
  type: 'SINGLE' | 'MULTIPLE' | 'JUDGE' | string
  stem: string
  options: ExamOption[]
  score: number
  sort: number
}

export interface ExamAnswerState {
  questionId: number
  answer?: string | null
  isCorrect?: boolean | null
  earnedScore?: number | null
  correctAnswer?: string | null
  analysis?: string | null
}

export interface ExamSession {
  recordId: number
  title: string
  mode: string
  source: string
  status: string
  totalScore: number
  startedAt: string
  deadlineAt?: string | null
  remainingSeconds?: number | null
  questions: ExamQuestion[]
  answers: ExamAnswerState[]
}

export interface AnswerFeedback {
  questionId: number
  answer: string
  isCorrect?: boolean | null
  earnedScore?: number | null
  correctAnswer?: string | null
  analysis?: string | null
}

export interface ExamResultQuestion {
  questionId: number
  subject: string
  chapter: string
  type: string
  stem: string
  options: ExamOption[]
  correctAnswer: string
  myAnswer?: string | null
  isCorrect: boolean
  score: number
  earnedScore: number
  analysis?: string | null
}

export interface ExamResult {
  record: ExamRecord
  score: number
  totalScore: number
  correctCount: number
  wrongCount: number
  unansweredCount: number
  durationSeconds?: number | null
  questions: ExamResultQuestion[]
}

export interface WrongQuestion {
  id: number
  questionId: number
  subject: string
  chapter: string
  type: string
  stem: string
  analysis?: string | null
  correctAnswer: string
  wrongCount: number
  lastWrongAt: string
  mastered: boolean
  options: QuestionOption[]
}

export interface QuestionOption {
  id?: number
  label: string
  content: string
  isCorrect?: boolean | null
  sort?: number
}

export interface QuestionItem {
  id: number
  subject: string
  chapter: string
  type: string
  stem: string
  analysis?: string | null
  difficulty: number
  status: string
  ownerUserId?: number | null
  correctAnswer: string
  options: QuestionOption[]
  createdAt?: string | null
}

export interface PaperQuestionItem {
  questionId: number
  subject: string
  chapter: string
  type: string
  stem: string
  analysis?: string | null
  correctAnswer: string
  score: number
  sort: number
  options: QuestionOption[]
}

export interface PaperDetail {
  paper: Paper
  questions: PaperQuestionItem[]
}
