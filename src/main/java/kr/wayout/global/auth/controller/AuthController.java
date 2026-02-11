package kr.wayout.global.auth.controller;


import jakarta.servlet.http.HttpServletResponse;
import kr.wayout.global.auth.AuthService;
import kr.wayout.global.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final String JSESSIONID_DOMAIN = "wayout.kr";

    @Value("${app.cookie-domain}")
    private String frontendUrl;

    @Override
    @GetMapping("/me")
    public ResponseEntity<?> check(@AuthenticationPrincipal String email) {
        if (email == null || "anonymousUser".equals(email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.check(email));
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
