export function parseUtc(value?: string | null): Date | null {
  if (!value) return null
  const normalized = /[zZ]|[+-]\d{2}:\d{2}$/.test(value) ? value : `${value}Z`
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

export function formatDateTime(value?: string | null): string {
  const date = parseUtc(value)
  if (!date) return '-'
  return date.toLocaleString('zh-CN', { hour12: false })
}

export function formatDate(value?: string | null): string {
  const date = parseUtc(value)
  if (!date) return '-'
  return date.toLocaleDateString('zh-CN')
}

export function couponValueText(type: string, value: number): string {
  if (type === 'PERCENT') {
    return `${(Number(value) / 10).toFixed(1)} 折`
  }
  return `¥${Number(value).toFixed(2)}`
}

export function couponRuleText(type: string, minAmount: number): string {
  const min = Number(minAmount || 0)
  if (type === 'PERCENT') {
    return min > 0 ? `满 ¥${min.toFixed(2)} 可用` : '无门槛折扣'
  }
  return min > 0 ? `满 ¥${min.toFixed(2)} 可用` : '无门槛立减'
}

export interface StatusMeta {
  text: string
  type: 'success' | 'warning' | 'danger' | 'info' | 'primary'
}

const ORDER_STATUS: Record<string, StatusMeta> = {
  PENDING_PAYMENT: { text: '待支付', type: 'warning' },
  PAID: { text: '已支付', type: 'success' },
  CANCELED: { text: '已取消', type: 'info' },
  REFUNDING: { text: '退款审核中', type: 'warning' },
  REFUNDED: { text: '已退款', type: 'danger' },
  COMPLETED: { text: '已完成', type: 'success' },
}

export function orderStatusMeta(status?: string | null): StatusMeta {
  if (!status) return { text: '-', type: 'info' }
  return ORDER_STATUS[status] || { text: status, type: 'info' }
}

const REFUND_STATUS: Record<string, StatusMeta> = {
  PENDING: { text: '待审核', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已驳回', type: 'danger' },
}

export function refundStatusMeta(status?: string | null): StatusMeta {
  if (!status) return { text: '-', type: 'info' }
  return REFUND_STATUS[status] || { text: status, type: 'info' }
}

const USER_COUPON_STATUS: Record<string, StatusMeta> = {
  UNUSED: { text: '未使用', type: 'success' },
  LOCKED: { text: '已锁定', type: 'warning' },
  USED: { text: '已使用', type: 'info' },
  EXPIRED: { text: '已过期', type: 'danger' },
}

export function userCouponStatusMeta(status?: string | null): StatusMeta {
  if (!status) return { text: '-', type: 'info' }
  return USER_COUPON_STATUS[status] || { text: status, type: 'info' }
}
