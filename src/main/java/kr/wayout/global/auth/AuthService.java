package kr.wayout.global.auth;

import kr.wayout.domain.member.Role;
import kr.wayout.global.auth.jwt.JwtProvider;
import kr.wayout.global.auth.jwt.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.CookieValue;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;

    // TODO: Custom Exception 적용 후 변경 필요

    public void signOut(String email) {
        refreshTokenStore.delete(email);
    }

    public String reissue(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 유효하지 않습니다.");
        }

        String email = jwtProvider.getEmail(refreshToken);
        if (!refreshTokenStore.validate(email, refreshToken)) {
            throw new IllegalArgumentException("Refresh Token이 존재하지 않습니다.");
        }

        return jwtProvider.createAccessToken(email, Role.USER);
    }

}
