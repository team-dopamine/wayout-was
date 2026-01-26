package kr.wayout.global.auth;

import kr.wayout.domain.member.Role;
import kr.wayout.global.auth.jwt.JwtProvider;
import kr.wayout.global.auth.jwt.RefreshTokenStore;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("유효한 Refresh Token으로 Access Token 재발급 성공")
    void reissue_sccuess() {
        // given
        String refreshToken = "valid-token";
        String email = "example1@gmail.com";
        String newAccessToken = "new-access-token";

        given(jwtProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtProvider.getEmail(refreshToken)).willReturn(email);
        given(refreshTokenStore.validate(email, refreshToken)).willReturn(true);
        given(jwtProvider.createAccessToken(email, Role.USER)).willReturn(newAccessToken);

        // when
        String result = authService.reissue(refreshToken);

        // then
        Assertions.assertThat(result).isEqualTo(newAccessToken);
    }

    @Test
    @DisplayName("만료된 Refresh Token으로 재발급 시 예외 발생")
    void reissueAccessToken_ExpiredToken_ThrowsException() {
        // given
        String refreshToken = "expired-refresh-token";

        given(jwtProvider.validateToken(refreshToken)).willReturn(false);

        // when & then
        Assertions.assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    @DisplayName("저장되지 않은 refresh token으로 재발급 요청 시 예외 발생")
    void reissue_not_stored() {
        // given
        String refreshToken = "not-stored-refresh-token";
        String email = "test@example.com";

        given(jwtProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtProvider.getEmail(refreshToken)).willReturn(email);
        given(refreshTokenStore.validate(email, refreshToken)).willReturn(false);

        // TODO: Custom Exception 적용하면 수정해야 함.
        // when & then
        Assertions.assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("로그아웃 시 RefreshTokenStore에서 토큰 삭제")
    void signOut_Success() {
        // given
        String email = "test@example.com";

        // when
        authService.signOut(email);

        // then
        BDDMockito.verify(refreshTokenStore).delete(email);
    }
}