package kr.wayout.global.auth.controller;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import kr.wayout.global.auth.OAuth2UserInfo;
import kr.wayout.global.auth.AuthService;
import kr.wayout.global.auth.dto.SignOutDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    @PostMapping("/sign-out")
    public ResponseEntity<?> signOut(@AuthenticationPrincipal String email, HttpServletResponse response) {
        authService.signOut(email);

        // Refresh Token 쿠키 삭제
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setPath("/");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);

        // Access Token 쿠키 삭제
        Cookie accessCookie = new Cookie("accessToken", null);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);
        return ResponseEntity.ok(new SignOutDto.Response("로그아웃에 성공하였습니다."));
    }

    @Override
    @PostMapping("/reissue")
    public void reissue(@CookieValue(name = "refreshToken") String refreshToken, HttpServletResponse response) {
        String newAccessToken = authService.reissue(refreshToken);

        Cookie cookie = new Cookie("accessToken", newAccessToken);
        cookie.setPath("/");
        cookie.setMaxAge(3600);
        response.addCookie(cookie);
    }
}
