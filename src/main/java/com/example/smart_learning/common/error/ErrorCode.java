package com.example.smart_learning.common.error;

public enum ErrorCode {

    INTERNAL_ERROR(10000, "系统内部错误"),
    BAD_REQUEST(10001, "请求参数不合法"),
    NOT_FOUND(10002, "资源不存在"),
    CONFLICT(10003, "数据冲突"),

    UNAUTHORIZED(20001, "未登录或登录已过期"),
    FORBIDDEN(20002, "没有操作权限"),
    INVALID_CREDENTIALS(20003, "账号或密码错误"),
    CODE_INVALID(20004, "验证码错误或已过期"),
    TOO_MANY_REQUESTS(20005, "请求过于频繁，请稍后再试"),
    USER_DISABLED(20006, "账号已被禁用"),
    TOKEN_INVALID(20007, "令牌无效"),

    GROUP_NOT_FOUND(30001, "权限分组不存在"),
    GROUP_CODE_EXISTS(30002, "分组编码已存在"),
    PRIV_NOT_FOUND(30003, "权限点不存在"),
    USER_NOT_FOUND(30004, "用户不存在"),
    PHONE_EXISTS(30005, "手机号已被绑定"),
    EMAIL_EXISTS(30006, "邮箱已被绑定"),
    GROUP_HAS_USERS(30007, "分组下还有用户，不能删除"),
    LESSON_NOT_FOUND(30008, "课时不存在"),
    NOT_ENROLLED(30009, "请先购买课程后再学习"),
    REVIEW_NOT_FOUND(30010, "评价不存在"),
    REVIEW_STATUS_INVALID(30011, "评价当前状态不允许该操作"),

    COURSE_NOT_PURCHASABLE(40001, "课程不存在或未上架"),
    ALREADY_ENROLLED(40002, "您已拥有该课程"),
    ORDER_NOT_FOUND(40003, "订单不存在"),
    ORDER_STATUS_INVALID(40004, "当前订单状态不允许该操作"),
    ORDER_EXPIRED(40005, "订单已超时关闭"),
    COUPON_NOT_FOUND(40006, "优惠券不存在"),
    COUPON_NOT_CLAIMABLE(40007, "优惠券不在可领取时间或已停用"),
    COUPON_ALREADY_CLAIMED(40008, "优惠券领取已达上限"),
    COUPON_EXHAUSTED(40009, "优惠券已领完"),
    COUPON_NOT_USABLE(40010, "优惠券不可用于本订单"),
    PAYMENT_FAILED(40011, "支付失败"),
    REFUND_NOT_ALLOWED(40012, "当前订单不可申请退款"),
    REFUND_EXISTS(40013, "该订单已有待处理的退款申请"),
    REFUND_NOT_FOUND(40014, "退款申请不存在"),
    REFUND_ALREADY_HANDLED(40015, "退款申请已处理"),
    PENDING_ORDER_EXISTS(40016, "该课程有待支付订单，请先支付或取消"),

    SECKILL_ACTIVITY_NOT_FOUND(70001, "秒杀活动不存在"),
    SECKILL_NOT_STARTED(70002, "秒杀活动尚未开始"),
    SECKILL_ENDED(70003, "秒杀活动已结束"),
    SECKILL_STOCK_EMPTY(70004, "秒杀名额已抢光"),
    SECKILL_LIMIT_REACHED(70005, "您已达到该活动的限购数量"),
    SECKILL_UNAVAILABLE(70006, "秒杀服务暂不可用，请稍后再试"),
    SECKILL_ALREADY_JOINED(70007, "您已参与该秒杀活动"),
    SECKILL_COURSE_INVALID(70008, "秒杀课程不存在或未上架"),
    SECKILL_STATUS_INVALID(70009, "秒杀活动当前状态不允许该操作"),
    SECKILL_STOCK_INVALID(70010, "剩余库存不能小于已售数量");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
