package kr.wayout.global.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.wayout.domain.member.Member;
import kr.wayout.global.auth.jwt.JwtProvider;
import kr.wayout.global.auth.jwt.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.cookie-domain}")
    private String domain;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2UserInfo userInfo = (OAuth2UserInfo) authentication.getPrincipal();
        Member member = userInfo.getMember();

        String accessToken = jwtProvider.createAccessToken(member.getEmail(), member.getRole());
        String refreshToken = jwtProvider.createRefreshToken(member.getEmail(), member.getRole());
        boolean isNewMember = member.getNickname().startsWith("USER_");

        refreshTokenStore.save(member.getEmail(), refreshToken);

        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", accessToken)
                .path("/")
                .domain(domain)
//                .secure(true)
                .sameSite("Lax")
                .httpOnly(true)
                .maxAge((int) jwtProvider.getExpirationTime(accessToken) / 1000)
                .build();
        response.addHeader("Set-Cookie", accessTokenCookie.toString());


        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
                .path("/")
                .domain(domain)
//                .secure(true)
                .sameSite("Lax")
                .httpOnly(true)
                .maxAge((int) jwtProvider.getExpirationTime(refreshToken) / 1000)
                .build();

        String redirectUrl = frontendUrl + "/auth/oauth?isNewMember=" + isNewMember;
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());
        response.sendRedirect(redirectUrl);
    }
}
