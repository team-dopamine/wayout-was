package kr.wayout.domain.member;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.awaitility.Awaitility.given;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("기존 회원 조회 성공")
    void readOrCreateTest_existing_success() {
        // given
        String email = "example_1@gmail.com";
        Member newMember = Member.builder()
                .email(email)
                .nickname("USER_1234")
                .role(Role.USER)
                .build();

        BDDMockito.given(memberRepository.findByEmail(email)).willReturn(Optional.of(newMember));

        // when
        Member result = memberService.readOrCreate(email);

        // then
        Assertions.assertThat(result.getEmail()).isEqualTo(email);
        Assertions.assertThat(result.getNickname()).isEqualTo(newMember.getNickname());
        verify(memberRepository, BDDMockito.never()).save(any());
    }

    @Test
    @DisplayName("신규 회원 생성 성공")
    void readOrCreateTest_new_success() {
        // given
        String email = "example_2@gmail.com";
        Member newMember = Member.builder()
                .email(email)
                .nickname("USER_1234")
                .role(Role.USER)
                .build();

        BDDMockito.given(memberRepository.findByEmail(email)).willReturn(Optional.empty());
        BDDMockito.given(memberRepository.save(any(Member.class))).willReturn(newMember);

        // when
        Member result = memberService.readOrCreate(email);

        // then
        Assertions.assertThat(result.getEmail()).isEqualTo(email);
        Assertions.assertThat(result.getNickname()).startsWith("USER_");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("닉네임 변경 성공")
    void changeNameTest_success() {
        String email = "example3@gmail.com";
        String newNickname = "변경 후 닉네임";

        Member member = Member.builder()
                .email(email)
                .nickname("변경 전 닉네임")
                .role(Role.USER)
                .build();

        BDDMockito.given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

        // when
        memberService.changeNickname(email, newNickname);

        // then
        Assertions.assertThat(member.getNickname()).isEqualTo(newNickname);
    }

    @Test
    @DisplayName("새 닉네임 변경 실패 - 길이가 0")
    void changeNickname_empty_failed() {
        // given
        String email = "example4@gmail.com";
        Member member = Member.builder().
                email(email)
                .nickname("USER_1234")
                .role(Role.USER)
                .build();

        BDDMockito.given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

        // when & then
        Assertions.assertThatThrownBy(() -> memberService.changeNickname(email, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 닉네임입니다.");
    }

    @Test
    @DisplayName("새 닉네임 길이 초과")
    void changeNickname_long_failed() {
        // given
        String email = "example5@gmail.com";
        String longNickname = "this_is_long_nickname";

        Member member = Member.builder().
                email(email)
                .nickname("USER_1234")
                .role(Role.USER)
                .build();

        BDDMockito.given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

        // when & then
        Assertions.assertThatThrownBy(() -> memberService.changeNickname(email, longNickname))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 닉네임입니다.");
    }
}