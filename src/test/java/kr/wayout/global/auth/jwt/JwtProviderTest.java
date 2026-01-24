package kr.wayout.global.auth.jwt;

import kr.wayout.domain.member.Role;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        String secret = "test-secret-key-must-be-at-least-512-bits-long-for-hs512-algorithm";
        long accessExpiration = 3600;
        long refreshExpiration = 3600;
        String issuer = "wayout-test";

        jwtProvider = new JwtProvider(secret, accessExpiration, refreshExpiration, issuer);
    }

    @Test
    @DisplayName("Access Token 생성 테스트 - 성공")
    void createAccessToken() {
        // given
        String email = "test@example.com";
        Role role = Role.USER;

        // when
        String token = jwtProvider.createAccessToken(email, role);

        // then
        Assertions.assertThat(token).isNotNull();
        Assertions.assertThat(jwtProvider.getEmail(token)).isEqualTo(email);
    }

    @Test
    @DisplayName("Refresh Token 생성 테스트 - 성공")
    void createRefreshToken() {
        // given
        String email = "test@example.com";
        Role role = Role.USER;

        // when
        String token = jwtProvider.createRefreshToken(email, role);

        // then
        Assertions.assertThat(token).isNotNull();
        Assertions.assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateToken_success() {
        // given
        String email = "test@example.com";
        Role role = Role.USER;

        String token = jwtProvider.createAccessToken(email, role);

        // when
        boolean isValid = jwtProvider.validateToken(token);

        // then
        Assertions.assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("null 토큰 검증 실패")
    void validateToken_null() {
        // when
        boolean isValid = jwtProvider.validateToken(null);

        // then
        Assertions.assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("유효하지 않은 토큰 검증 실패")
    void validateToken_invalid() {
        // when
        boolean isValid = jwtProvider.validateToken("This-is-invalid-token");

        // then
        Assertions.assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void validateToken_expired() {
        // given
        JwtProvider shortLivedProvider = new JwtProvider(
                "test-secret-key-must-be-at-least-512-bits-long-for-hs512-algorithm",
                1L,  // 1ms (즉시 만료)
                1L,
                "wayout-test"
        );
        String token = shortLivedProvider.createAccessToken("test@example.com", Role.USER);

        // when
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean isValid = shortLivedProvider.validateToken(token);

        // then
        Assertions.assertThat(isValid).isFalse();

    }

    @Test
    @DisplayName("이메일 추출 성공")
    void getEmail() {
        // given
        String email = "test@example.com";
        Role role = Role.USER;

        String token = jwtProvider.createAccessToken(email, role);

        // when
        String extractedEmail = jwtProvider.getEmail(token);


        // then
        Assertions.assertThat(extractedEmail).isEqualTo(email);
    }

    @Test
    @DisplayName("Role 추출 성공")
    void getRole() {
        // given
        String email = "test@example.com";
        Role role = Role.USER;

        String token = jwtProvider.createAccessToken(email, role);

        // when
        Role extractedRole = jwtProvider.getRole(token);


        // then
        Assertions.assertThat(extractedRole).isEqualTo(role);
    }
}