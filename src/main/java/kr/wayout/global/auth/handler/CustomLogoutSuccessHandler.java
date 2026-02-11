package kr.wayout.global.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.wayout.global.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AuthService authService;

    @Value("${app.cookie-domain}")
    private String cookieDomain;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        if (authentication != null) {
            String email = authentication.getName();
            authService.signOut(email);
        }

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .path("/")
                .domain(cookieDomain)
//                .secure(true)
                .sameSite("Lax")
                .httpOnly(true)
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .path("/")
                .domain(cookieDomain)
//                .secure(true)
                .sameSite("Lax")
                .httpOnly(true)
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        response.setStatus(HttpServletResponse.SC_OK);

    }
}
