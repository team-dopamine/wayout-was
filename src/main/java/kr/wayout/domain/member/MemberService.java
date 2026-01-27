package kr.wayout.domain.member;

import kr.wayout.domain.member.dto.UpdateNicknameDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .email(email)
                                .nickname(generateTemporaryNickname())
                                .role(Role.USER)
                                .build()
                ));
    }

    @Transactional
    public UpdateNicknameDto.Response changeNickname(String email, UpdateNicknameDto.Request dto) {
        Member member = findByEmail(email);
        // TODO: Custom Exception 도입 후 수정 필요
        String oldNickName = member.getNickname();
        String nickname = dto.getNickname();

        member.changeNickname(nickname);

        String message = "닉네임 변경에 성공하였습니다.";
        log.info("사용자({})의 닉네임이 (전: {})에서 (후: {})로 변경되었습니다.", email, oldNickName, nickname);
        return new UpdateNicknameDto.Response(nickname, message);
    }

    private Member findByEmail(String email) {
        // TODO: 존재하지 않는 이메일에 대한 예외 처리 필요
        return memberRepository.findByEmail(email).get();
    }

    private String generateTemporaryNickname() {
        return "USER_" + UUID.randomUUID().toString().substring(0, 4);
    }
}
