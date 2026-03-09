package kr.wayout.domain.submission;

import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.problem.ProblemRepository;
import kr.wayout.domain.submission.dto.SubmissionDto;
import kr.wayout.domain.submission.runner.CounterExampleRunner;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @InjectMocks
    private SubmissionService submissionService;

    @Mock
    private MemberService memberService;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private CounterExampleRunner counterExampleRunner;

    @Test
    @DisplayName("제출 목록 조회 서비스 - 회원/비회원 제출 정보를 응답으로 매핑한다")
    void list_success_with_member_and_anonymous() {
        // given
        PageRequest pageable = PageRequest.of(0, 8);

        Member member = Mockito.mock(Member.class);
        Problem firstProblem = Mockito.mock(Problem.class);
        Problem secondProblem = Mockito.mock(Problem.class);
        Submission firstSubmission = Mockito.mock(Submission.class);
        Submission secondSubmission = Mockito.mock(Submission.class);

        LocalDateTime firstCreatedAt = LocalDateTime.of(2026, 3, 8, 12, 0);
        LocalDateTime secondCreatedAt = LocalDateTime.of(2026, 3, 8, 12, 30);

        BDDMockito.given(member.getNickname()).willReturn("Jsplix");
        BDDMockito.given(firstProblem.getTitle()).willReturn("A+B");
        BDDMockito.given(secondProblem.getTitle()).willReturn("최대값 찾기");

        BDDMockito.given(firstSubmission.getId()).willReturn(1L);
        BDDMockito.given(firstSubmission.getMember()).willReturn(member);
        BDDMockito.given(firstSubmission.getProblem()).willReturn(firstProblem);
        BDDMockito.given(firstSubmission.getLanguage()).willReturn(Language.JAVA);
        BDDMockito.given(firstSubmission.getExecutionTime()).willReturn(1.5);
        BDDMockito.given(firstSubmission.getCreatedAt()).willReturn(firstCreatedAt);

        BDDMockito.given(secondSubmission.getId()).willReturn(2L);
        BDDMockito.given(secondSubmission.getMember()).willReturn(null);
        BDDMockito.given(secondSubmission.getProblem()).willReturn(secondProblem);
        BDDMockito.given(secondSubmission.getLanguage()).willReturn(Language.PYTHON);
        BDDMockito.given(secondSubmission.getExecutionTime()).willReturn(2.3);
        BDDMockito.given(secondSubmission.getCreatedAt()).willReturn(secondCreatedAt);

        Page<Submission> page = new PageImpl<>(List.of(firstSubmission, secondSubmission), pageable, 2);
        BDDMockito.given(submissionRepository.findAll(pageable)).willReturn(page);

        // when
        Page<SubmissionDto.ListResponse> result = submissionService.list(pageable);

        // then
        Assertions.assertThat(result.getTotalElements()).isEqualTo(2);
        Assertions.assertThat(result.getContent()).hasSize(2);

        SubmissionDto.ListResponse first = result.getContent().get(0);
        Assertions.assertThat(first.getId()).isEqualTo(1L);
        Assertions.assertThat(first.getNickname()).isEqualTo("Jsplix");
        Assertions.assertThat(first.getTitle()).isEqualTo("A+B");
        Assertions.assertThat(first.getLanguage()).isEqualTo(Language.JAVA);
        Assertions.assertThat(first.getExecutionTime()).isEqualTo(1.5);
        Assertions.assertThat(first.getCreatedAt()).isEqualTo(firstCreatedAt);

        SubmissionDto.ListResponse second = result.getContent().get(1);
        Assertions.assertThat(second.getId()).isEqualTo(2L);
        Assertions.assertThat(second.getNickname()).isEqualTo("익명");
        Assertions.assertThat(second.getTitle()).isEqualTo("최대값 찾기");
        Assertions.assertThat(second.getLanguage()).isEqualTo(Language.PYTHON);
        Assertions.assertThat(second.getExecutionTime()).isEqualTo(2.3);
        Assertions.assertThat(second.getCreatedAt()).isEqualTo(secondCreatedAt);

        verifyNoInteractions(memberService, problemRepository, counterExampleRunner);
    }

    @Test
    @DisplayName("제출 목록 조회 서비스 - 데이터가 없으면 빈 페이지를 반환한다")
    void list_empty_page() {
        // given
        PageRequest pageable = PageRequest.of(0, 8);
        BDDMockito.given(submissionRepository.findAll(pageable))
                .willReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        // when
        Page<SubmissionDto.ListResponse> result = submissionService.list(pageable);

        // then
        Assertions.assertThat(result.getContent()).isEmpty();
        Assertions.assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(memberService, problemRepository, counterExampleRunner);
    }
}
