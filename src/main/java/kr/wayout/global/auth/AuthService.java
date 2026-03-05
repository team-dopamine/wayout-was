package kr.wayout.global.auth;

import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.member.Role;
import kr.wayout.global.auth.dto.SignInDto;
import kr.wayout.global.auth.jwt.JwtProvider;
import kr.wayout.global.auth.jwt.RefreshTokenStore;
import kr.wayout.global.exception.CustomBusinessException;
import kr.wayout.global.exception.ErrorCode;
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

    public SignInDto.CheckResponse check(String email) {
        Member member = memberService.read(email);
        return SignInDto.CheckResponse.from(member.getNickname());
    }

    public void signOut(String email) {
        refreshTokenStore.delete(email);
    }

    public String reissue(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomBusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtProvider.getEmail(refreshToken);
        if (!refreshTokenStore.validate(email, refreshToken)) {
            throw new CustomBusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        return jwtProvider.createAccessToken(email, Role.USER);
    }

    @Transactional
    public String withdraw(String email) {
        Member member = memberService.read(email);

        if (member.isDeleted()) {
            throw new CustomBusinessException(ErrorCode.MEMBER_ALREADY_DELETED);
        }

        member.delete();
        refreshTokenStore.delete(email);

        return "탈퇴 처리가 완료되었습니다.";
    }

}
