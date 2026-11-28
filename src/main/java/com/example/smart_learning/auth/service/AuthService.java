package com.example.smart_learning.auth.service;

import com.example.smart_learning.auth.dto.EmailLoginRequest;
import com.example.smart_learning.auth.dto.LoginRequest;
import com.example.smart_learning.auth.dto.MeResponse;
import com.example.smart_learning.auth.dto.RefreshRequest;
import com.example.smart_learning.auth.dto.RegisterRequest;
import com.example.smart_learning.auth.dto.SmsLoginRequest;
import com.example.smart_learning.auth.dto.TokenResponse;
import com.example.smart_learning.auth.dto.UserProfile;
import com.example.smart_learning.auth.entity.RefreshToken;
import com.example.smart_learning.auth.repository.RefreshTokenRepository;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.permission.entity.SysGroup;
import com.example.smart_learning.permission.entity.SysUserGroup;
import com.example.smart_learning.permission.repository.SysGroupRepository;
import com.example.smart_learning.permission.repository.SysUserGroupRepository;
import com.example.smart_learning.security.JwtService;
import com.example.smart_learning.security.LoginUser;
import com.example.smart_learning.security.UserDetailsServiceImpl;
import com.example.smart_learning.user.entity.SysUser;
import com.example.smart_learning.user.repository.SysUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Master
public class AuthService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SysUserRepository userRepository;
    private final SysUserGroupRepository userGroupRepository;
    private final SysGroupRepository groupRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final VerificationCodeService verificationCodeService;
    private final CaptchaService captchaService;
    private final StringRedisTemplate redis;
    private final Duration refreshTtl;

    public AuthService(SysUserRepository userRepository,
                       SysUserGroupRepository userGroupRepository,
                       SysGroupRepository groupRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserDetailsServiceImpl userDetailsService,
                       VerificationCodeService verificationCodeService,
                       CaptchaService captchaService,
                       StringRedisTemplate redis,
                       @Value("${app.jwt.refresh-ttl:P7D}") Duration refreshTtl) {
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.groupRepository = groupRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.verificationCodeService = verificationCodeService;
        this.captchaService = captchaService;
        this.redis = redis;
        this.refreshTtl = refreshTtl;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String account = request.account();
        checkLoginCooldown(account);
        // 账号密码登录每次都必须通过图形验证码
        captchaService.verify(request.captchaId(), request.captchaCode());
        SysUser user = userRepository.findByAccount(account)
                .or(() -> userRepository.findByPhone(account))
                .or(() -> userRepository.findByEmail(account))
                .orElse(null);
        if (user == null) {
            recordLoginFailure(account);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        ensureActive(user);
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordLoginFailure(account);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        clearLoginFailures(account);
        user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
        LoginUser loginUser = userDetailsService.loadById(user.getId());
        return issueTokens(user, loginUser);
    }

    @Transactional
    public TokenResponse loginBySms(SmsLoginRequest request) {
        captchaService.verify(request.captchaId(), request.captchaCode());
        // 先验码再查用户，避免未注册手机号未持有验证码时被枚举
        verificationCodeService.verify("sms", request.phone(), request.code());
        SysUser user = userRepository.findByPhone(request.phone())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "该手机号未注册，请先注册"));
        ensureActive(user);
        user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
        LoginUser loginUser = userDetailsService.loadById(user.getId());
        return issueTokens(user, loginUser);
    }

    @Transactional
    public TokenResponse loginByEmail(EmailLoginRequest request) {
        captchaService.verify(request.captchaId(), request.captchaCode());
        verificationCodeService.verify("email", request.email(), request.code());
        SysUser user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "该邮箱未注册，请先注册"));
        ensureActive(user);
        user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
        LoginUser loginUser = userDetailsService.loadById(user.getId());
        return issueTokens(user, loginUser);
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String target = request.target().trim();
        boolean phoneTarget = PHONE_PATTERN.matcher(target).matches();
        boolean emailTarget = EMAIL_PATTERN.matcher(target).matches();
        if (!phoneTarget && !emailTarget) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入正确的手机号或邮箱");
        }
        String channel = phoneTarget ? "sms" : "email";
        // 同一目标并发注册只放行一个，避免双击/并发请求建出重复账号
        String lockKey = "auth:register:lock:" + channel + ":" + target;
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "注册处理中，请勿重复提交");
        }
        try {
            verificationCodeService.verify(channel, target, request.code());

            if (phoneTarget && userRepository.existsByPhone(target)) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS, "该手机号已注册，请直接登录");
            }
            if (emailTarget && userRepository.existsByEmail(target)) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS, "该邮箱已注册，请直接登录");
            }

            String nickname = request.nickname() == null ? "" : request.nickname().trim();
            SysUser user = new SysUser();
            if (phoneTarget) {
                user.setAccount(uniqueAccount("u" + target));
                user.setPhone(target);
                if (nickname.isEmpty()) {
                    nickname = "学员" + target.substring(target.length() - 4);
                }
            } else {
                String prefix = target.substring(0, target.indexOf('@'));
                user.setAccount(uniqueAccount("e_" + prefix));
                user.setEmail(target);
                if (nickname.isEmpty()) {
                    nickname = prefix;
                }
            }
            user.setNickname(nickname);
            user.setPosition("学员");
            user.setStatus("ACTIVE");
            user.setPrivVersion(0L);

            String password = request.password() == null ? "" : request.password().trim();
            if (!password.isEmpty()) {
                if (password.length() < 6) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "密码至少 6 位");
                }
                user.setPasswordHash(passwordEncoder.encode(password));
            }
            user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));

            user = userRepository.save(user);
            assignStudentGroup(user.getId());
            LoginUser loginUser = userDetailsService.loadById(user.getId());
            return issueTokens(user, loginUser);
        } finally {
            redis.delete(lockKey);
        }
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String hash = sha256(request.refreshToken());
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
        if (Boolean.TRUE.equals(token.getRevoked()) || token.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        token.setRevoked(true);
        token.setRotatedAt(LocalDateTime.now(ZoneOffset.UTC));
        refreshTokenRepository.save(token);

        SysUser user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
        ensureActive(user);
        LoginUser loginUser = userDetailsService.loadById(user.getId());
        return issueTokens(user, loginUser);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public MeResponse me(LoginUser loginUser) {
        SysUser user = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return buildMe(user, loginUser);
    }

    private TokenResponse issueTokens(SysUser user, LoginUser loginUser) {
        String accessToken = jwtService.createAccessToken(loginUser);
        String rawRefresh = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(sha256(rawRefresh));
        refreshToken.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plus(refreshTtl));
        refreshTokenRepository.save(refreshToken);
        return new TokenResponse(accessToken, rawRefresh, "Bearer",
                jwtService.accessTtlSeconds(), buildMe(user, loginUser));
    }

    private MeResponse buildMe(SysUser user, LoginUser loginUser) {
        UserProfile profile = new UserProfile(
                user.getId(), user.getAccount(), user.getRealname(), user.getNickname(), user.getPosition(),
                user.getPhone(), user.getEmail(), user.getAvatar(),
                loginUser.getPermissions().groupCodes());
        return new MeResponse(profile,
                loginUser.getPermissions().rights(),
                loginUser.getPermissions().menus(),
                loginUser.getPermissions().dataScope(),
                loginUser.getPermissions().limited(),
                loginUser.getPermissions().privVersion());
    }

    private String uniqueAccount(String base) {
        String safe = base.length() > 48 ? base.substring(0, 48) : base;
        if (!userRepository.existsByAccount(safe)) {
            return safe;
        }
        return safe + "_" + UUID.randomUUID().toString().substring(0, 6);
    }

    private void assignStudentGroup(Long userId) {
        Optional<SysGroup> student = groupRepository.findByCode("STUDENT");
        student.ifPresent(group -> {
            SysUserGroup relation = new SysUserGroup();
            relation.setUserId(userId);
            relation.setGroupId(group.getId());
            userGroupRepository.save(relation);
        });
    }

    private void ensureActive(SysUser user) {
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
    }

    /** 冷却期内不允许再尝试，但不锁定账号，冷却结束即可继续。 */
    private void checkLoginCooldown(String account) {
        Long seconds = redis.getExpire(loginCooldownKey(account), TimeUnit.SECONDS);
        if (seconds != null && seconds > 0) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "登录尝试过于频繁，请 " + seconds + " 秒后再试");
        }
    }

    private void recordLoginFailure(String account) {
        String key = loginFailKey(account);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofMinutes(10));
        }
        int seconds = cooldownSeconds(count == null ? 0 : count);
        if (seconds > 0) {
            redis.opsForValue().set(loginCooldownKey(account), String.valueOf(seconds), Duration.ofSeconds(seconds));
        }
    }

    /** 第 3 次失败起逐级冷却：10s / 20s / 40s，最长 60s。 */
    private int cooldownSeconds(long failures) {
        if (failures < 3) {
            return 0;
        }
        return (int) Math.min(60L, 10L << Math.min(failures - 3, 3));
    }

    private void clearLoginFailures(String account) {
        redis.delete(loginFailKey(account));
        redis.delete(loginCooldownKey(account));
    }

    private String loginFailKey(String account) {
        // 账号大小写不敏感（与数据库排序规则一致），避免换大小写绕过计数
        return "auth:login:fail:" + account.toLowerCase(Locale.ROOT);
    }

    private String loginCooldownKey(String account) {
        return "auth:login:cooldown:" + account.toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
