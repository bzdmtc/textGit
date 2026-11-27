package com.example.smart_learning.permission.service;

import java.util.Arrays;
import java.util.List;

public enum PrivilegeRegistry {

    ADMIN_ACCESS("admin", "access", "后台访问", "VIEW", "/admin/dashboard", "Setting", 10),
    MANAGE_ACCESS("manage", "access", "管理端访问", "VIEW", "/teacher", "Management", 20),

    USER_BROWSE("user", "browse", "用户列表", "VIEW", "/admin/users", "User", 110),
    USER_CREATE("user", "create", "新增用户", "ACTION", null, null, 111),
    USER_EDIT("user", "edit", "编辑用户", "ACTION", null, null, 112),
    USER_DISABLE("user", "disable", "禁用/启用用户", "ACTION", null, null, 113),
    USER_ASSIGN_GROUP("user", "assign-group", "分配用户分组", "ACTION", null, null, 114),

    GROUP_BROWSE("group", "browse", "权限分组列表", "VIEW", "/admin/groups", "Connection", 120),
    GROUP_CREATE("group", "create", "新增权限分组", "ACTION", null, null, 121),
    GROUP_EDIT("group", "edit", "编辑权限分组", "ACTION", null, null, 122),
    GROUP_DELETE("group", "delete", "删除权限分组", "ACTION", null, null, 123),
    GROUP_ASSIGN_PRIV("group", "assign-priv", "分配分组权限", "ACTION", null, null, 124),
    PRIV_BROWSE("priv", "browse", "权限字典", "VIEW", null, null, 130),

    COURSE_BROWSE("course", "browse", "课程管理", "VIEW", "/admin/courses", "Reading", 210),
    COURSE_CREATE("course", "create", "新增课程", "ACTION", null, null, 211),
    COURSE_EDIT("course", "edit", "编辑课程/章节课时", "ACTION", null, null, 212),
    COURSE_DELETE("course", "delete", "删除课程", "ACTION", null, null, 213),
    COURSE_PUBLISH("course", "publish", "上下架课程", "ACTION", null, null, 214),

    TEACHER_BROWSE("teacher", "browse", "讲师管理", "VIEW", "/admin/teachers", "User", 220),
    TEACHER_CREATE("teacher", "create", "新增讲师", "ACTION", null, null, 221),
    TEACHER_EDIT("teacher", "edit", "编辑讲师", "ACTION", null, null, 222),
    TEACHER_DELETE("teacher", "delete", "删除讲师", "ACTION", null, null, 223),

    ARTICLE_BROWSE("article", "browse", "文章管理", "VIEW", "/admin/articles", "Document", 230),
    ARTICLE_CREATE("article", "create", "新增文章", "ACTION", null, null, 231),
    ARTICLE_EDIT("article", "edit", "编辑文章", "ACTION", null, null, 232),
    ARTICLE_DELETE("article", "delete", "删除文章", "ACTION", null, null, 233),
    ARTICLE_PUBLISH("article", "publish", "发布文章", "ACTION", null, null, 234),

    MATERIAL_BROWSE("material", "browse", "资料管理", "VIEW", "/admin/materials", "Folder", 240),
    MATERIAL_CREATE("material", "create", "新增资料", "ACTION", null, null, 241),
    MATERIAL_EDIT("material", "edit", "编辑资料", "ACTION", null, null, 242),
    MATERIAL_DELETE("material", "delete", "删除资料", "ACTION", null, null, 243),

    BANNER_BROWSE("banner", "browse", "Banner 管理", "VIEW", "/admin/banners", "Picture", 250),
    BANNER_CREATE("banner", "create", "新增 Banner", "ACTION", null, null, 251),
    BANNER_EDIT("banner", "edit", "编辑 Banner", "ACTION", null, null, 252),
    BANNER_DELETE("banner", "delete", "删除 Banner", "ACTION", null, null, 253),

    FILE_UPLOAD("file", "upload", "上传文件", "ACTION", null, null, 260),

    ORDER_BROWSE("order", "browse", "订单管理", "VIEW", "/admin/orders", "List", 310),
    REFUND_BROWSE("refund", "browse", "退款审核", "VIEW", "/admin/refunds", "RefreshRight", 320),
    REFUND_APPROVE("refund", "approve", "处理退款", "ACTION", null, null, 321),
    COUPON_BROWSE("coupon", "browse", "优惠券管理", "VIEW", "/admin/coupons", "Ticket", 330),
    COUPON_CREATE("coupon", "create", "新增优惠券", "ACTION", null, null, 331),
    COUPON_EDIT("coupon", "edit", "编辑优惠券", "ACTION", null, null, 332),
    COUPON_DELETE("coupon", "delete", "停用优惠券", "ACTION", null, null, 333),

    SECKILL_BROWSE("seckill", "browse", "秒杀活动管理", "VIEW", "/admin/seckill", "Timer", 340),
    SECKILL_CREATE("seckill", "create", "新增秒杀活动", "ACTION", null, null, 341),
    SECKILL_EDIT("seckill", "edit", "编辑/预热秒杀活动", "ACTION", null, null, 342),
    SECKILL_DELETE("seckill", "delete", "删除/停用秒杀活动", "ACTION", null, null, 343),

    AUDIT_BROWSE("audit", "browse", "审计日志", "VIEW", "/admin/logs", "Document", 140),

    PROFILE_EDIT("profile", "edit", "编辑个人资料", "ACTION", null, null, 200);

    private final String module;
    private final String method;
    private final String name;
    private final String type;
    private final String menuPath;
    private final String icon;
    private final int sort;

    PrivilegeRegistry(String module, String method, String name, String type, String menuPath, String icon, int sort) {
        this.module = module;
        this.method = method;
        this.name = name;
        this.type = type;
        this.menuPath = menuPath;
        this.icon = icon;
        this.sort = sort;
    }

    public String module() {
        return module;
    }

    public String method() {
        return method;
    }

    public String displayName() {
        return name;
    }

    public String type() {
        return type;
    }

    public String menuPath() {
        return menuPath;
    }

    public String icon() {
        return icon;
    }

    public int sort() {
        return sort;
    }

    public String authority() {
        return module + ":" + method;
    }

    public static List<PrivilegeRegistry> all() {
        return Arrays.asList(values());
    }
}
