package com.example.demo.security;

import com.example.demo.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {
    @Test void signsTokensWithConfiguredExpiryAndRejectsTampering() {
        var provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "test-only-secret-012345678901234567890123456789012345678901234567890123456789");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 60000);
        provider.validateConfiguration();
        var user = new User();
        user.setId("user-id"); user.setUsername("alice"); user.setEmail("alice@example.org"); user.setRoles(Set.of("ROLE_USER"));
        var principal = new CustomUserDetails(user);
        long now = System.currentTimeMillis();
        String token = provider.generateToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserIdFromJWT(token)).isEqualTo("user-id");
        assertThat(provider.getExpirationFromToken(token)).isBetween(now + 58000, now + 62000);
        int signatureStart = token.lastIndexOf('.') + 1;
        String forged = token.substring(0, signatureStart) + (token.charAt(signatureStart) == 'A' ? 'B' : 'A') + token.substring(signatureStart + 1);
        assertThat(provider.validateToken(forged)).isFalse();
        assertThat(provider.validateToken("invalid")).isFalse();
    }

    @Test void rejectsWeakSecretsAtStartup() {
        var provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "short-secret");
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 60000);
        assertThatThrownBy(provider::validateConfiguration).isInstanceOf(IllegalStateException.class);
    }
}
