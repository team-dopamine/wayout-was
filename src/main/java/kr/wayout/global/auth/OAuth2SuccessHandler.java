package kr.wayout.global.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.dto.OAuth2UserInfo;
import kr.wayout.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2UserInfo userInfo = (OAuth2UserInfo) authentication.getPrincipal();
        Member member = userInfo.getMember();

        String accessToken = jwtProvider.createAccessToken(member.getEmail(), member.getRole());
        String refreshToken = jwtProvider.createRefreshToken(member.getEmail(), member.getRole());
        boolean isNewMember = member.getNickname().startsWith("USER_");

        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge((int) (jwtProvider.getExpirationTime(accessToken) / 1000));
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setMaxAge((int) (jwtProvider.getExpirationTime(refreshToken) / 1000));
        response.addCookie(refreshTokenCookie);

        String redirectUrl = "/auth/oauth?isNewMember=" + isNewMember;
        response.sendRedirect(redirectUrl);
    }
}
