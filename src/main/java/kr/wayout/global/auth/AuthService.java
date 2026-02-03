package kr.wayout.global.auth;

import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.member.Role;
import kr.wayout.global.auth.jwt.JwtProvider;
import kr.wayout.global.auth.jwt.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final MemberService memberService;
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

    @Transactional
    public String withdraw(String email) {
        Member member = memberService.read(email);

        if (member.isDeleted()) {
            throw new IllegalStateException("이미 삭제 된 사용자입니다.");
        }

        member.delete();

        return "탈퇴 처리가 완료되었습니다.";
    }

}
