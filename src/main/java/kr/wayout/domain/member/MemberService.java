package kr.wayout.domain.member;

import kr.wayout.domain.member.dto.MemberDto;
import kr.wayout.domain.member.dto.NicknameDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public Member readOrCreate(String email) {
        // TODO: 동시성 처리에 대한 예외 처리 필요
        return memberRepository.findByEmailIncludingDeleted(email)
                .map(this::handleExisting)
                .orElseGet(() -> create(email));
    }

    private Member create(String email) {
        return memberRepository.save(
                Member.builder()
                        .email(email)
                        .nickname(generateTemporaryNickname())
                        .role(Role.USER)
                        .build()
        );
    }

    public NicknameDto.ReadResponse getNickname(String email) {
        Member member = read(email);
        return NicknameDto.ReadResponse.of(member.getNickname());
    }

    public MemberDto.Info getMember(String email) {
        Member member = read(email);
        return MemberDto.Info.from(member);
    }

    private Member handleExisting(Member member) {
        if (!member.isDeleted()) {
            return member;
        }

        if (member.isRestoreable()) {
            member.restore();
            return member;
        }

        throw new OAuth2AuthenticationException("탈퇴 후 30일이 경과하여 복구할 수 없습니다.");
    }


    @Transactional
    public NicknameDto.UpdateResponse changeNickname(String email, NicknameDto.UpdateRequest dto) {
        Member member = read(email);
        // TODO: Custom Exception 도입 후 수정 필요
        String oldNickName = member.getNickname();
        String nickname = dto.getNickname();

        member.changeNickname(nickname);

        String message = "닉네임 변경에 성공하였습니다.";
        log.info("사용자({})의 닉네임이 (전: {})에서 (후: {})로 변경되었습니다.", email, oldNickName, nickname);
        return new NicknameDto.UpdateResponse(nickname, message);
    }

    public Member read(String email) {
        // TODO: 존재하지 않는 이메일에 대한 예외 처리 필요
        return memberRepository.findByEmail(email).get();
    }

    private String generateTemporaryNickname() {
        return "USER_" + UUID.randomUUID().toString().substring(0, 4);
    }
}
