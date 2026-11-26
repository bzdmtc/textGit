package com.example.smart_learning.auth.service;

import com.example.smart_learning.auth.dto.EmailLoginRequest;
import com.example.smart_learning.auth.dto.LoginRequest;
import com.example.smart_learning.auth.dto.MeResponse;
import com.example.smart_learning.auth.dto.RefreshRequest;
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
import java.util.Optional;
import java.util.UUID;

@Service
@Master
public class AuthService {

    private final SysUserRepository userRepository;
    private final SysUserGroupRepository userGroupRepository;
    private final SysGroupRepository groupRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final VerificationCodeService verificationCodeService;
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
        this.redis = redis;
        this.refreshTtl = refreshTtl;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String account = request.account();
        checkLoginAllowed(account);
        SysUser user = userRepository.findByAccount(account).orElse(null);
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
        verificationCodeService.verify("sms", request.phone(), request.code());
        SysUser user = userRepository.findByPhone(request.phone())
                .orElseGet(() -> registerByPhone(request.phone()));
        ensureActive(user);
        user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
        LoginUser loginUser = userDetailsService.loadById(user.getId());
        return issueTokens(user, loginUser);
    }

    @Transactional
    public TokenResponse loginByEmail(EmailLoginRequest request) {
        verificationCodeService.verify("email", request.email(), request.code());
        SysUser user = userRepository.findByEmail(request.email())
                .orElseGet(() -> registerByEmail(request.email()));
        ensureActive(user);
        user.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        userRepository.save(user);
        LoginUser loginUser = userDetailsService.loadById(user.getId());
        return issueTokens(user, loginUser);
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
                user.getId(), user.getAccount(), user.getRealname(), user.getNickname(),
                user.getPhone(), user.getEmail(), user.getAvatar(),
                loginUser.getPermissions().groupCodes());
        return new MeResponse(profile,
                loginUser.getPermissions().rights(),
                loginUser.getPermissions().menus(),
                loginUser.getPermissions().dataScope(),
                loginUser.getPermissions().limited(),
                loginUser.getPermissions().privVersion());
    }

    private SysUser registerByPhone(String phone) {
        SysUser user = new SysUser();
        user.setAccount(uniqueAccount("u" + phone));
        user.setPhone(phone);
        user.setNickname("学员" + phone.substring(phone.length() - 4));
        user.setStatus("ACTIVE");
        user.setPrivVersion(0L);
        user = userRepository.save(user);
        assignStudentGroup(user.getId());
        return user;
    }

    private SysUser registerByEmail(String email) {
        SysUser user = new SysUser();
        String prefix = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        user.setAccount(uniqueAccount("e_" + prefix));
        user.setEmail(email);
        user.setNickname(prefix);
        user.setStatus("ACTIVE");
        user.setPrivVersion(0L);
        user = userRepository.save(user);
        assignStudentGroup(user.getId());
        return user;
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

    private void checkLoginAllowed(String account) {
        String value = redis.opsForValue().get(loginFailKey(account));
        if (value != null && Integer.parseInt(value) >= 5) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "登录失败次数过多，请 10 分钟后再试");
        }
    }

    private void recordLoginFailure(String account) {
        String key = loginFailKey(account);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofMinutes(10));
        }
    }

    private void clearLoginFailures(String account) {
        redis.delete(loginFailKey(account));
    }

    private String loginFailKey(String account) {
        return "auth:login:fail:" + account;
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
