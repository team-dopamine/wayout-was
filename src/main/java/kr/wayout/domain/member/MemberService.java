package kr.wayout.domain.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
    public void changeNickname(String email, String nickname) {
        Member member = findByEmail(email);
        // TODO: Custom Exception 도입 후 수정 필요
        if (nickname.isBlank() && nickname.length() > 12) {
            throw new IllegalArgumentException("유효하지 않은 닉네임입니다.");
        }
        member.changeNickname(nickname);
    }

    private Member findByEmail(String email) {
        // TODO: 존재하지 않는 이메일에 대한 예외 처리 필요
        return memberRepository.findByEmail(email).get();
    }

    private String generateTemporaryNickname() {
        return "USER_" + UUID.randomUUID().toString().substring(0, 4);
    }
}
