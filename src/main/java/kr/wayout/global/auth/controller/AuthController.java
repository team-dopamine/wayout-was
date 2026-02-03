package kr.wayout.global.auth.controller;


import jakarta.servlet.http.HttpServletResponse;
import kr.wayout.global.auth.AuthService;
import kr.wayout.global.auth.dto.SignOutDto;
import kr.wayout.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @Value("${app.cookie-domain}")
    private String frontendUrl;

    @Override
    @PostMapping("/sign-out")
    public ResponseEntity<?> signOut(@AuthenticationPrincipal String email, HttpServletResponse response) {
        authService.signOut(email);
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "")
                .path("/")
                .domain(frontendUrl)
//                .secure(true)
                .sameSite("Lax")
                .httpOnly(true)
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", "")
                .path("/")
                .domain(frontendUrl)
//                .secure(true)
                .sameSite("Lax")
                .httpOnly(true)
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());

        return ResponseEntity.ok(new SignOutDto.Response("로그아웃에 성공하였습니다."));
    }

    @Override
    @PostMapping("/reissue")
    public void reissue(@CookieValue(name = "refreshToken") String refreshToken, HttpServletResponse response) {
        String newAccessToken = authService.reissue(refreshToken);
        // TODO: Nginx HTTPS 설정 후 secure 부분 주석 해제
        ResponseCookie accessTokenCookie = ResponseCookie.from("accessToken", newAccessToken)
                .path("/")
                .domain(frontendUrl)
//                .secure(true)
                .sameSite("Lax")
                .httpOnly(true)
                .maxAge(jwtProvider.getExpirationTime(newAccessToken) / 1000)
                .build();

        response.addHeader("Set-Cookie", accessTokenCookie.toString());
    }

    @Override
    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(authService.withdraw(email));
    }
}
