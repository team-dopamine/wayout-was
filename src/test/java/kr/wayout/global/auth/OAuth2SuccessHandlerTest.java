package kr.wayout.global.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.member.Role;
import kr.wayout.domain.member.dto.OAuth2UserInfo;
import kr.wayout.global.auth.jwt.JwtProvider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @InjectMocks
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private Authentication authentication;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private Member createMember(String email, String nickname) {
        return Member.builder()
                .email(email)
                .nickname(nickname)
                .role(Role.USER)
                .build();
    }

    private Cookie findCookie(MockHttpServletResponse response, String name) {
        for (Cookie cookie : response.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    @BeforeEach
    void set() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("로그인 성공 시 쿠키에 Access Token 생성")
    void createAccessTokenCookie() throws ServletException, IOException {
        // given
        Member member = createMember("example1@gmail.com", "member");
        OAuth2UserInfo userInfo = new OAuth2UserInfo(member, null);

        BDDMockito.when(authentication.getPrincipal()).thenReturn(userInfo);
        BDDMockito.when(jwtProvider.createAccessToken(member.getEmail(), Role.USER)).thenReturn("access-token");
        BDDMockito.when(jwtProvider.createRefreshToken(member.getEmail(), Role.USER)).thenReturn("refresh-token");

        // when
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        Cookie accessTokenCookie = findCookie(response, "accessToken");
        Assertions.assertThat(accessTokenCookie).isNotNull();
        Assertions.assertThat(accessTokenCookie.getValue()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("로그인 성공 시 쿠키에 HttpOnly Refresh Token이 생성")
    void createRefreshTokenCookie() throws ServletException, IOException {
        // given
        Member member = createMember("example2@gmail.com", "member2");
        OAuth2UserInfo userInfo = new OAuth2UserInfo(member, null);

        BDDMockito.when(authentication.getPrincipal()).thenReturn(userInfo);
        BDDMockito.when(jwtProvider.createAccessToken(member.getEmail(), Role.USER)).thenReturn("access-token");
        BDDMockito.when(jwtProvider.createRefreshToken(member.getEmail(), Role.USER)).thenReturn("refresh-token");

        // when
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        Cookie refreshTokenCookie = findCookie(response, "refreshToken");
        Assertions.assertThat(refreshTokenCookie).isNotNull();
        Assertions.assertThat(refreshTokenCookie.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("신규 회원은 isNewMember=true로 리다이렉트")
    void redirectNewMember() throws ServletException, IOException {
        // given
        Member member = createMember("example3@gmail.com", "USER_1234");
        OAuth2UserInfo userInfo = new OAuth2UserInfo(member, null);

        BDDMockito.when(authentication.getPrincipal()).thenReturn(userInfo);
        BDDMockito.when(jwtProvider.createAccessToken(member.getEmail(), member.getRole())).thenReturn("access-token");
        BDDMockito.when(jwtProvider.createRefreshToken(member.getEmail(), member.getRole())).thenReturn("refresh-token");

        // when
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        Assertions.assertThat(response.getRedirectedUrl()).contains("isNewMember=true");
    }

    @Test
    @DisplayName("기존 회원은 isNewMember=false로 리다이렉트")
    void redirectExistingMember() throws ServletException, IOException {
        // given
        Member member = createMember("example3@gmail.com", "기존닉네임");
        OAuth2UserInfo userInfo = new OAuth2UserInfo(member, null);

        BDDMockito.when(authentication.getPrincipal()).thenReturn(userInfo);
        BDDMockito.when(jwtProvider.createAccessToken(member.getEmail(), member.getRole())).thenReturn("access-token");
        BDDMockito.when(jwtProvider.createRefreshToken(member.getEmail(), member.getRole())).thenReturn("refresh-token");

        // when
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        Assertions.assertThat(response.getRedirectedUrl()).contains("isNewMember=false");
    }
}