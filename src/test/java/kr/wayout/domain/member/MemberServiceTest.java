package kr.wayout.domain.member;

import kr.wayout.domain.member.dto.UpdateNicknameDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


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

        BDDMockito.given(memberRepository.findByEmailIncludingDeleted(email)).willReturn(Optional.of(newMember));

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

        BDDMockito.given(memberRepository.findByEmailIncludingDeleted(email)).willReturn(Optional.empty());
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

        UpdateNicknameDto.Request request = new UpdateNicknameDto.Request(newNickname);

        BDDMockito.given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));

        // when
        UpdateNicknameDto.Response response = memberService.changeNickname(email, request);

        // then
        Assertions.assertThat(member.getNickname()).isEqualTo(newNickname);
        Assertions.assertThat(response.getNickname()).isEqualTo(newNickname);
    }

}