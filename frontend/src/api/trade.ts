import http from './http'
import type {
  ApiResponse,
  Coupon,
  MyCourse,
  Order,
  OrderPreview,
  PageResult,
  PaymentChannel,
  PaymentResult,
  PurchaseStatus,
  Refund,
  UserCoupon,
} from '@/types'

export interface CouponPayload {
  name: string
  type: 'FIXED' | 'PERCENT'
  value: number
  minAmount: number
  totalQuantity: number
  perUserLimit: number
  startAt: string
  endAt: string
  subject?: string
  description?: string
  status?: string
}

export interface CreateOrderPayload {
  courseIds: number[]
  userCouponId?: number | null
}

export const publicCouponApi = {
  forCourse(courseId: number) {
    return http.get<ApiResponse<Coupon[]>>('/public/coupons', { params: { courseId } })
  },
}

export const couponApi = {
  claim(couponId: number) {
    return http.post<ApiResponse<UserCoupon>>(`/coupons/${couponId}/claim`)
  },
  mine(status?: string) {
    return http.get<ApiResponse<UserCoupon[]>>('/my/coupons', { params: { status: status || undefined } })
  },
}

export const orderApi = {
  preview(payload: CreateOrderPayload) {
    return http.post<ApiResponse<OrderPreview>>('/orders/preview', payload)
  },
  create(payload: CreateOrderPayload) {
    return http.post<ApiResponse<Order>>('/orders', payload)
  },
  mine(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<Order>>>('/my/orders', { params })
  },
  detail(orderNo: string) {
    return http.get<ApiResponse<Order>>(`/my/orders/${orderNo}`)
  },
  cancel(orderNo: string) {
    return http.post<ApiResponse<Order>>(`/orders/${orderNo}/cancel`)
  },
  applyRefund(orderNo: string, reason: string) {
    return http.post<ApiResponse<Refund>>(`/orders/${orderNo}/refund`, { reason })
  },
  purchaseStatus(courseId: number) {
    return http.get<ApiResponse<PurchaseStatus>>('/my/purchase-status', { params: { courseId } })
  },
}

export const paymentApi = {
  channels() {
    return http.get<ApiResponse<PaymentChannel[]>>('/payments/channels')
  },
  pay(orderNo: string, channel: string) {
    return http.post<ApiResponse<PaymentResult>>(`/payments/${orderNo}/pay`, { channel })
  },
}

export const myCourseApi = {
  list() {
    return http.get<ApiResponse<MyCourse[]>>('/my/courses')
  },
}

export const couponAdminApi = {
  page(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<Coupon>>>('/admin/coupons', { params })
  },
  create(payload: CouponPayload) {
    return http.post<ApiResponse<Coupon>>('/admin/coupons', payload)
  },
  update(id: number, payload: CouponPayload) {
    return http.put<ApiResponse<Coupon>>(`/admin/coupons/${id}`, payload)
  },
  setStatus(id: number, status: string) {
    return http.put<ApiResponse<Coupon>>(`/admin/coupons/${id}/status`, { status })
  },
}

export const orderAdminApi = {
  page(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<Order>>>('/admin/orders', { params })
  },
  detail(orderNo: string) {
    return http.get<ApiResponse<Order>>(`/admin/orders/${orderNo}`)
  },
}

export const refundAdminApi = {
  page(params: Record<string, unknown>) {
    return http.get<ApiResponse<PageResult<Refund>>>('/admin/refunds', { params })
  },
  approve(id: number, adminNote?: string) {
    return http.post<ApiResponse<Refund>>(`/admin/refunds/${id}/approve`, { adminNote })
  },
  reject(id: number, adminNote?: string) {
    return http.post<ApiResponse<Refund>>(`/admin/refunds/${id}/reject`, { adminNote })
  },
}
