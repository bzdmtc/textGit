package com.example.smart_learning.security;

import com.example.smart_learning.permission.dto.PermissionSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "dGVzdC1vbmx5LWp3dC1zZWNyZXQta2V5LXBsZWFzZS1kby1ub3QtdXNlLWluLXByb2Q=",
            Duration.ofHours(2));

    @Test
    void createdTokenCanBeParsedWithUserAndPrivVersion() {
        PermissionSnapshot snapshot = new PermissionSnapshot(5L,
                Map.of("course", Set.of("browse")), List.of(), List.of("ADMIN"), "ALL", false);
        LoginUser user = new LoginUser(42L, "admin", "hash", "ACTIVE", snapshot);

        String token = jwtService.createAccessToken(user);
        JwtService.AccessTokenPayload payload = jwtService.parseAccessToken(token);

        assertThat(payload.userId()).isEqualTo(42L);
        assertThat(payload.account()).isEqualTo("admin");
        assertThat(payload.privVersion()).isEqualTo(5L);
        assertThat(jwtService.accessTtlSeconds()).isEqualTo(7200);
    }
}
