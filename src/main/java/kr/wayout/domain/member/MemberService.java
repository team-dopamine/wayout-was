package kr.wayout.domain.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Member readOrCreate(String email) {
        return memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .email(email)
                                .nickname(generateTemporaryNickname())
                                .role(Role.USER)
                                .build()
                ));
    }

    public void changeNickname(String email, String nickname) {
        Member member = findByEmail(email);
        member.changeNickname(nickname);
    }

    private Member findByEmail(String email) {
        return memberRepository.findByEmail(email).get();
    }

    private String generateTemporaryNickname() {
        return "USER_" + System.currentTimeMillis();
    }
}
