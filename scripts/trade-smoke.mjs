// 模块 3 交易链路 API 冒烟：登录 → 领券 → 下单 → 支付 → 开通 → 退款 → 审核 → 驳回/取消
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
    throw new Error(`${method} ${path} -> HTTP ${response.status}: ${text.slice(0, 500)}`)
  }
  return { status: response.status, data }
}

async function login(account, password) {
  const response = await api('POST', '/auth/login', { account, password })
  return { token: response.data.data.accessToken, me: response.data.data.me }
}

function money(value) {
  return Number(Number(value).toFixed(2))
}

async function main() {
  const student = await login('student', 'Student@123')
  const admin = await login('admin', 'Admin@123')
  const teacher = await login('teacher', 'Teacher@123')
  ok('三种账号登录', Boolean(student.token && admin.token && teacher.token))

  const publicCoupons = await api('GET', '/public/coupons?courseId=1')
  ok('前台课程优惠券（从库）', publicCoupons.data.data.length >= 1, publicCoupons.data.data.map((c) => c.name))

  const before = await api('GET', '/my/coupons', null, student.token)
  ok('初始我的优惠券为空', before.data.data.length === 0)

  const claim = await api('POST', '/coupons/1/claim', null, student.token)
  const userCouponId = claim.data.data.id
  ok('领取满 500 减 50', claim.data.data.status === 'UNUSED', claim.data.data.name)
  const duplicateClaim = await api('POST', '/coupons/1/claim', null, student.token, false)
  ok('重复领取被拒绝', duplicateClaim.status === 400 && duplicateClaim.data.code === 40008,
    { status: duplicateClaim.status, code: duplicateClaim.data.code })

  const preview = await api('POST', '/orders/preview', { courseIds: [1], userCouponId }, student.token)
  ok('预览订单服务端计价', money(preview.data.data.totalAmount) === 599
    && money(preview.data.data.discountAmount) === 50
    && money(preview.data.data.payAmount) === 549, preview.data.data)

  const created = await api('POST', '/orders', { courseIds: [1], userCouponId }, student.token)
  const order = created.data.data
  ok('创建待支付订单', order.status === 'PENDING_PAYMENT' && order.payAmount === 549, order.orderNo)
  ok('优惠券锁定', (await api('GET', '/my/coupons', null, student.token)).data.data[0].status === 'LOCKED')

  const pendingAgain = await api('POST', '/orders', { courseIds: [1], userCouponId }, student.token, false)
  ok('同课程待支付订单去重', pendingAgain.status === 400 && pendingAgain.data.code === 40016,
    { status: pendingAgain.status, code: pendingAgain.data.code })

  const paid = await api('POST', `/payments/${order.orderNo}/pay`, { channel: 'MOCK_ALIPAY' }, student.token)
  ok('模拟支付成功', paid.data.data.order.status === 'PAID', paid.data.data.transactionNo)
  const paidAgain = await api('POST', `/payments/${order.orderNo}/pay`, { channel: 'MOCK_ALIPAY' }, student.token)
  ok('重复支付幂等', paidAgain.data.data.message === '订单已支付')

  const myCourses = await api('GET', '/my/courses', null, student.token)
  ok('支付后开通课程', myCourses.data.data.some((c) => c.courseId === 1), myCourses.data.data.map((c) => c.courseTitle))
  const status = await api('GET', '/my/purchase-status?courseId=1', null, student.token)
  ok('课程购买状态为已购买', status.data.data.enrolled === true)
  const usedCoupon = (await api('GET', '/my/coupons', null, student.token)).data.data.find((c) => c.id === userCouponId)
  ok('支付后优惠券核销', usedCoupon.status === 'USED')

  const enrollAgain = await api('POST', '/orders', { courseIds: [1] }, student.token, false)
  ok('已购课程不可重复下单', enrollAgain.status === 400 && enrollAgain.data.code === 40002,
    { status: enrollAgain.status, code: enrollAgain.data.code })

  const refund = await api('POST', `/orders/${order.orderNo}/refund`, { reason: '课程内容与预期不符，申请退款' }, student.token)
  ok('提交退款申请', refund.data.data.status === 'PENDING')
  const refundAgain = await api('POST', `/orders/${order.orderNo}/refund`, { reason: '再次申请' }, student.token, false)
  ok('重复退款申请被拒绝', refundAgain.status === 400 && refundAgain.data.code === 40012,
    { status: refundAgain.status, code: refundAgain.data.code })

  const refundPage = await api('GET', '/admin/refunds?status=PENDING', null, admin.token)
  const pendingRefund = refundPage.data.data.items.find((item) => item.orderNo === order.orderNo)
  ok('管理员退款列表可见', Boolean(pendingRefund), refundPage.data.data.total)

  const teacherOrders = await api('GET', '/admin/orders', null, teacher.token, false)
  ok('讲师无订单管理权限 403', teacherOrders.status === 403, teacherOrders.status)

  const approved = await api('POST', `/admin/refunds/${pendingRefund.id}/approve`, { adminNote: '同意退款，自动撤销课程' }, admin.token)
  ok('退款审核通过', approved.data.data.status === 'APPROVED')
  const refundedOrder = await api('GET', `/admin/orders/${order.orderNo}`, null, admin.token)
  ok('订单状态变为已退款', refundedOrder.data.data.status === 'REFUNDED')
  const afterRevoke = await api('GET', '/my/courses', null, student.token)
  ok('退款后撤销课程权限', !afterRevoke.data.data.some((c) => c.courseId === 1))

  // 折扣券 + 下单后取消
  const percentClaim = await api('POST', '/coupons/2/claim', null, student.token)
  ok('领取数学 9 折券', percentClaim.data.data.status === 'UNUSED')
  const percentPreview = await api('POST', '/orders/preview',
    { courseIds: [3], userCouponId: percentClaim.data.data.id }, student.token)
  ok('折扣券计价 699 -> 629.10', money(percentPreview.data.data.discountAmount) === 69.9
    && money(percentPreview.data.data.payAmount) === 629.1, percentPreview.data.data)

  const cancelOrder = await api('POST', '/orders', { courseIds: [2] }, student.token)
  const canceled = await api('POST', `/orders/${cancelOrder.data.data.orderNo}/cancel`, null, student.token)
  ok('取消待支付订单', canceled.data.data.status === 'CANCELED')

  const mathOrder = await api('POST', '/orders',
    { courseIds: [3], userCouponId: percentClaim.data.data.id }, student.token)
  const mathPaid = await api('POST', `/payments/${mathOrder.data.data.orderNo}/pay`, { channel: 'MOCK_WECHAT' }, student.token)
  ok('数学课程支付开通', mathPaid.data.data.order.status === 'PAID')

  // 管理员优惠券 CRUD
  const now = Date.now()
  const iso = (ms) => new Date(ms).toISOString().slice(0, 19)
  const couponCreate = await api('POST', '/admin/coupons', {
    name: '自动化测试券',
    type: 'FIXED',
    value: 10,
    minAmount: 100,
    totalQuantity: 5,
    perUserLimit: 1,
    startAt: iso(now - 86400000),
    endAt: iso(now + 86400000),
    subject: '',
    description: '冒烟测试创建',
    status: 'ON',
  }, admin.token)
  ok('管理员创建优惠券', couponCreate.data.data.remainingQuantity === 5)
  const couponOff = await api('PUT', `/admin/coupons/${couponCreate.data.data.id}/status`, { status: 'OFF' }, admin.token)
  ok('管理员停用优惠券', couponOff.data.data.status === 'OFF')

  const orders = await api('GET', '/admin/orders?size=20', null, admin.token)
  ok('管理员订单列表', orders.data.data.total >= 3, orders.data.data.total)

  console.log(failures === 0 ? '\nALL CHECKS PASSED' : `\n${failures} CHECK(S) FAILED`)
  process.exitCode = failures === 0 ? 0 : 1
}

main().catch((error) => {
  console.error('SMOKE ABORTED:', error.message)
  process.exitCode = 1
})
