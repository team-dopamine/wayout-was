package kr.wayout.global.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import kr.wayout.domain.member.Role;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private FilterChain filterChain;

    private JwtProvider jwtProvider;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        String secret = "test-secret-key-must-be-at-least-512-bits-long-for-hs512-algorithm";
        jwtProvider = new JwtProvider(secret, 3600, 3600, "wayout-test");
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtProvider);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 토큰에 대한 인증 정보 등록 테스트")
    void 유효한_토큰_인증_정보_등록() throws ServletException, IOException {
        // given
        String email = "test@example.com";
        Role role = Role.USER;

        String token = jwtProvider.createAccessToken(email, role);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", token));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assertions.assertThat(authentication).isNotNull();
        Assertions.assertThat(authentication.getPrincipal()).isEqualTo(email);
        Assertions.assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰이 존재하지 않을 때 - 인증 정보 등록 X")
    void 토큰_없어서_인증_정보_등록안됨() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assertions.assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("유효하지 않은 토큰 - 인증 정보 등록 X")
    void 유효하지_않은_토큰_검증() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", "invalid-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assertions.assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 접두사가 없음 - 인증 정보 등록 X")
    void noBearerPrefix_noAuthentication() throws ServletException, IOException {
        // given
        String token = jwtProvider.createAccessToken("test@example.com", Role.USER);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", token);  // Bearer 없음
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Assertions.assertThat(authentication).isNull();
        verify(filterChain).doFilter(request, response);
    }
}