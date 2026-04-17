package kr.wayout.domain.submission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.problem.Platform;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.problem.ProblemRepository;
import kr.wayout.domain.submission.dto.SubmissionDto;
import kr.wayout.domain.submission.runner.CounterExampleRunner;
import kr.wayout.global.exception.CustomBusinessException;
import kr.wayout.global.exception.ErrorCode;
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
import java.util.Optional;

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

    @Mock
    private ObjectMapper objectMapper;

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
        JsonNode firstCounterExamples = Mockito.mock(JsonNode.class);

        LocalDateTime firstCreatedAt = LocalDateTime.of(2026, 3, 8, 12, 0);
        LocalDateTime secondCreatedAt = LocalDateTime.of(2026, 3, 8, 12, 30);

        BDDMockito.given(member.getNickname()).willReturn("Jsplix");
        BDDMockito.given(firstProblem.getProblemNo()).willReturn(1001);
        BDDMockito.given(firstProblem.getTitle()).willReturn("A+B");
        BDDMockito.given(firstProblem.getPlatform()).willReturn(Platform.SWEA);
        BDDMockito.given(secondProblem.getProblemNo()).willReturn(1002);
        BDDMockito.given(secondProblem.getTitle()).willReturn("최대값 찾기");
        BDDMockito.given(secondProblem.getPlatform()).willReturn(Platform.SWEA);

        BDDMockito.given(firstSubmission.getId()).willReturn(1L);
        BDDMockito.given(firstSubmission.getMember()).willReturn(member);
        BDDMockito.given(firstSubmission.getProblem()).willReturn(firstProblem);
        BDDMockito.given(firstSubmission.getLanguage()).willReturn(Language.JAVA);
        BDDMockito.given(firstSubmission.getExecutionTime()).willReturn(1.5);
        BDDMockito.given(firstSubmission.getCounterExamples()).willReturn(firstCounterExamples);
        BDDMockito.given(firstCounterExamples.isNull()).willReturn(false);
        BDDMockito.given(firstCounterExamples.size()).willReturn(2);
        BDDMockito.given(firstSubmission.getCreatedAt()).willReturn(firstCreatedAt);
        BDDMockito.given(firstSubmission.getIsOpen()).willReturn(true);

        BDDMockito.given(secondSubmission.getId()).willReturn(2L);
        BDDMockito.given(secondSubmission.getMember()).willReturn(null);
        BDDMockito.given(secondSubmission.getProblem()).willReturn(secondProblem);
        BDDMockito.given(secondSubmission.getLanguage()).willReturn(Language.PYTHON);
        BDDMockito.given(secondSubmission.getExecutionTime()).willReturn(2.3);
        BDDMockito.given(secondSubmission.getCounterExamples()).willReturn(null);
        BDDMockito.given(secondSubmission.getCreatedAt()).willReturn(secondCreatedAt);
        BDDMockito.given(secondSubmission.getIsOpen()).willReturn(false);

        Page<Submission> page = new PageImpl<>(List.of(firstSubmission, secondSubmission), pageable, 2);
        BDDMockito.given(submissionRepository.findAll(pageable)).willReturn(page);

        // when
        Page<SubmissionDto.ListResponse> result = submissionService.list(pageable);

        // then
        Assertions.assertThat(result.getTotalElements()).isEqualTo(2);
        Assertions.assertThat(result.getContent()).hasSize(2);

        SubmissionDto.ListResponse first = result.getContent().get(0);
        Assertions.assertThat(first.getId()).isEqualTo(1L);
        Assertions.assertThat(first.getProblemNo()).isEqualTo(1001);
        Assertions.assertThat(first.getNickname()).isEqualTo("Jsplix");
        Assertions.assertThat(first.getTitle()).isEqualTo("A+B");
        Assertions.assertThat(first.getLanguage()).isEqualTo(Language.JAVA);
        Assertions.assertThat(first.getPlatform()).isEqualTo(Platform.SWEA);
        Assertions.assertThat(first.getExecutionTime()).isEqualTo(1.5);
        Assertions.assertThat(first.getCounterExampleCount()).isEqualTo(2);
        Assertions.assertThat(first.getCreatedAt()).isEqualTo(firstCreatedAt);

        SubmissionDto.ListResponse second = result.getContent().get(1);
        Assertions.assertThat(second.getId()).isEqualTo(2L);
        Assertions.assertThat(second.getProblemNo()).isEqualTo(1002);
        Assertions.assertThat(second.getNickname()).isEqualTo("익명");
        Assertions.assertThat(second.getTitle()).isEqualTo("최대값 찾기");
        Assertions.assertThat(second.getLanguage()).isEqualTo(Language.PYTHON);
        Assertions.assertThat(second.getPlatform()).isEqualTo(Platform.SWEA);
        Assertions.assertThat(second.getExecutionTime()).isEqualTo(2.3);
        Assertions.assertThat(second.getCounterExampleCount()).isZero();
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

    @Test
    @DisplayName("특정 문제 제출 목록 조회 서비스 - 해당 문제 제출 정보를 응답으로 매핑한다")
    void list_by_problem_success() {
        // given
        PageRequest pageable = PageRequest.of(0, 8);
        Long problemId = 1L;

        Problem problem = Mockito.mock(Problem.class);
        Member member = Mockito.mock(Member.class);
        Submission submission = Mockito.mock(Submission.class);
        JsonNode counterExamplesNode = Mockito.mock(JsonNode.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 10, 10, 0);

        BDDMockito.given(problemRepository.findProblemById(problemId)).willReturn(problem);
        BDDMockito.given(problem.getProblemNo()).willReturn(1001);
        BDDMockito.given(problem.getTitle()).willReturn("A+B");
        BDDMockito.given(problem.getPlatform()).willReturn(Platform.SWEA);
        BDDMockito.given(member.getNickname()).willReturn("Jsplix");
        BDDMockito.given(submission.getId()).willReturn(10L);
        BDDMockito.given(submission.getMember()).willReturn(member);
        BDDMockito.given(submission.getProblem()).willReturn(problem);
        BDDMockito.given(submission.getLanguage()).willReturn(Language.CPP);
        BDDMockito.given(submission.getExecutionTime()).willReturn(0.7);
        BDDMockito.given(submission.getCounterExamples()).willReturn(counterExamplesNode);
        BDDMockito.given(counterExamplesNode.isNull()).willReturn(false);
        BDDMockito.given(counterExamplesNode.size()).willReturn(1);
        BDDMockito.given(submission.getCreatedAt()).willReturn(createdAt);
        BDDMockito.given(submission.getIsOpen()).willReturn(true);

        BDDMockito.given(submissionRepository.findAllByProblem(pageable, problem))
                .willReturn(new PageImpl<>(List.of(submission), pageable, 1));

        // when
        Page<SubmissionDto.ListResponse> result = submissionService.listByProblemId(pageable, problemId);

        // then
        Assertions.assertThat(result.getTotalElements()).isEqualTo(1);
        Assertions.assertThat(result.getContent()).hasSize(1);

        SubmissionDto.ListResponse item = result.getContent().get(0);
        Assertions.assertThat(item.getId()).isEqualTo(10L);
        Assertions.assertThat(item.getProblemNo()).isEqualTo(1001);
        Assertions.assertThat(item.getNickname()).isEqualTo("Jsplix");
        Assertions.assertThat(item.getTitle()).isEqualTo("A+B");
        Assertions.assertThat(item.getLanguage()).isEqualTo(Language.CPP);
        Assertions.assertThat(item.getPlatform()).isEqualTo(Platform.SWEA);
        Assertions.assertThat(item.getExecutionTime()).isEqualTo(0.7);
        Assertions.assertThat(item.getCounterExampleCount()).isEqualTo(1);
        Assertions.assertThat(item.getCreatedAt()).isEqualTo(createdAt);

        verifyNoInteractions(memberService, counterExampleRunner);
    }

    @Test
    @DisplayName("특정 문제 제출 목록 조회 서비스 - 데이터가 없으면 빈 페이지를 반환한다")
    void list_by_problem_empty_page() {
        // given
        PageRequest pageable = PageRequest.of(0, 8);
        Long problemId = 1L;

        Problem problem = Mockito.mock(Problem.class);
        BDDMockito.given(problemRepository.findProblemById(problemId)).willReturn(problem);
        BDDMockito.given(submissionRepository.findAllByProblem(pageable, problem))
                .willReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        // when
        Page<SubmissionDto.ListResponse> result = submissionService.listByProblemId(pageable, problemId);

        // then
        Assertions.assertThat(result.getContent()).isEmpty();
        Assertions.assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(memberService, counterExampleRunner);
    }

    @Test
    @DisplayName("특정 문제 제출 목록 조회 서비스 - 문제가 없으면 예외를 던진다")
    void list_by_problem_problem_not_found() {
        // given
        PageRequest pageable = PageRequest.of(0, 8);
        Long problemId = 999L;
        BDDMockito.given(problemRepository.findProblemById(problemId)).willReturn(null);

        // when
        Assertions.assertThatThrownBy(() -> submissionService.listByProblemId(pageable, problemId))
                .isInstanceOf(CustomBusinessException.class)
                .satisfies(throwable ->
                        Assertions.assertThat(((CustomBusinessException) throwable).getErrorCode())
                                .isEqualTo(ErrorCode.PROBLEM_NOT_FOUND));

        // then
        verifyNoInteractions(memberService, submissionRepository, counterExampleRunner);
    }

    @Test
    @DisplayName("내 제출 목록 조회 서비스 - 해당 사용자 제출 정보를 응답으로 매핑한다")
    void list_mine_success() {
        // given
        String email = "test@gmail.com";
        PageRequest pageable = PageRequest.of(0, 8);

        Member member = Mockito.mock(Member.class);
        Problem problem = Mockito.mock(Problem.class);
        Submission submission = Mockito.mock(Submission.class);
        JsonNode counterExamplesNode = Mockito.mock(JsonNode.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 16, 9, 30);

        BDDMockito.given(memberService.read(email)).willReturn(member);
        BDDMockito.given(member.getId()).willReturn(7L);
        BDDMockito.given(member.getNickname()).willReturn("Jsplix");
        BDDMockito.given(problem.getProblemNo()).willReturn(1200);
        BDDMockito.given(problem.getTitle()).willReturn("부분 수열의 합");
        BDDMockito.given(problem.getPlatform()).willReturn(Platform.SWEA);
        BDDMockito.given(submission.getId()).willReturn(31L);
        BDDMockito.given(submission.getMember()).willReturn(member);
        BDDMockito.given(submission.getProblem()).willReturn(problem);
        BDDMockito.given(submission.getLanguage()).willReturn(Language.JAVA);
        BDDMockito.given(submission.getExecutionTime()).willReturn(0.13);
        BDDMockito.given(submission.getCounterExamples()).willReturn(counterExamplesNode);
        BDDMockito.given(counterExamplesNode.isNull()).willReturn(false);
        BDDMockito.given(counterExamplesNode.size()).willReturn(3);
        BDDMockito.given(submission.getCreatedAt()).willReturn(createdAt);
        BDDMockito.given(submission.getIsOpen()).willReturn(true);
        BDDMockito.given(submissionRepository.findAllByMemberIdWithProblem(7L, pageable))
                .willReturn(new PageImpl<>(List.of(submission), pageable, 1));

        // when
        Page<SubmissionDto.ListResponse> result = submissionService.listMine(email, pageable);

        // then
        Assertions.assertThat(result.getTotalElements()).isEqualTo(1);
        Assertions.assertThat(result.getContent()).hasSize(1);

        SubmissionDto.ListResponse item = result.getContent().get(0);
        Assertions.assertThat(item.getId()).isEqualTo(31L);
        Assertions.assertThat(item.getProblemNo()).isEqualTo(1200);
        Assertions.assertThat(item.getNickname()).isEqualTo("Jsplix");
        Assertions.assertThat(item.getTitle()).isEqualTo("부분 수열의 합");
        Assertions.assertThat(item.getLanguage()).isEqualTo(Language.JAVA);
        Assertions.assertThat(item.getPlatform()).isEqualTo(Platform.SWEA);
        Assertions.assertThat(item.getExecutionTime()).isEqualTo(0.13);
        Assertions.assertThat(item.getCounterExampleCount()).isEqualTo(3);
        Assertions.assertThat(item.getCreatedAt()).isEqualTo(createdAt);

        verifyNoInteractions(problemRepository, counterExampleRunner);
    }

    @Test
    @DisplayName("내 제출 목록 조회 서비스 - 데이터가 없으면 빈 페이지를 반환한다")
    void list_mine_empty() {
        // given
        String email = "test@gmail.com";
        PageRequest pageable = PageRequest.of(0, 8);
        Member member = Mockito.mock(Member.class);

        BDDMockito.given(memberService.read(email)).willReturn(member);
        BDDMockito.given(member.getId()).willReturn(7L);
        BDDMockito.given(submissionRepository.findAllByMemberIdWithProblem(7L, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<SubmissionDto.ListResponse> result = submissionService.listMine(email, pageable);

        // then
        Assertions.assertThat(result.getContent()).isEmpty();
        Assertions.assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(problemRepository, counterExampleRunner);
    }

    @Test
    @DisplayName("제출 상세 조회 서비스 - 반례 목록이 있으면 카운트를 계산해 반환한다")
    void detail_success_with_counter_example_count() {
        // given
        Long submissionId = 1L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 24, 10, 0);

        Submission submission = Mockito.mock(Submission.class);
        Problem problem = Mockito.mock(Problem.class);
        JsonNode counterExamplesNode = Mockito.mock(JsonNode.class);
        SubmissionDto.CounterExampleCase counterExample = SubmissionDto.CounterExampleCase.builder()
                .input("3\n1 2 3")
                .expectedOutput("6")
                .actualOutput("5")
                .build();

        BDDMockito.given(submissionRepository.findById(submissionId)).willReturn(Optional.of(submission));
        BDDMockito.given(submission.getIsOpen()).willReturn(true);
        BDDMockito.given(submission.getCounterExamples()).willReturn(counterExamplesNode);
        BDDMockito.given(counterExamplesNode.isNull()).willReturn(false);
        BDDMockito.given(submission.getId()).willReturn(submissionId);
        BDDMockito.given(submission.getProblem()).willReturn(problem);
        BDDMockito.given(problem.getProblemNo()).willReturn(1001);
        BDDMockito.given(problem.getTitle()).willReturn("A+B");
        BDDMockito.given(problem.getPlatform()).willReturn(Platform.SWEA);
        BDDMockito.given(submission.getSourceCode()).willReturn("public class Main {}");
        BDDMockito.given(submission.getLanguage()).willReturn(Language.JAVA);
        BDDMockito.given(submission.getExecutionTime()).willReturn(1.23);
        BDDMockito.given(submission.getCreatedAt()).willReturn(createdAt);
        Mockito.doReturn(List.of(counterExample))
                .when(objectMapper)
                .convertValue(Mockito.eq(counterExamplesNode), Mockito.any(TypeReference.class));

        // when
        SubmissionDto.Detail result = submissionService.detail(submissionId);

        // then
        Assertions.assertThat(result.getId()).isEqualTo(1L);
        Assertions.assertThat(result.getProblemNo()).isEqualTo(1001L);
        Assertions.assertThat(result.getCounterExampleCount()).isEqualTo(1);
        Assertions.assertThat(result.getCounterExamples()).hasSize(1);
        Assertions.assertThat(result.getCounterExamples().get(0).getInput()).isEqualTo("3\n1 2 3");
        Assertions.assertThat(result.getCounterExamples().get(0).getExpectedOutput()).isEqualTo("6");
        Assertions.assertThat(result.getCounterExamples().get(0).getActualOutput()).isEqualTo("5");
    }

    @Test
    @DisplayName("제출 상세 조회 서비스 - 반례 JSON이 없으면 카운트 0과 빈 목록을 반환한다")
    void detail_success_with_empty_counter_examples_when_json_is_null() {
        // given
        Long submissionId = 1L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 24, 10, 0);

        Submission submission = Mockito.mock(Submission.class);
        Problem problem = Mockito.mock(Problem.class);

        BDDMockito.given(submissionRepository.findById(submissionId)).willReturn(Optional.of(submission));
        BDDMockito.given(submission.getIsOpen()).willReturn(true);
        BDDMockito.given(submission.getCounterExamples()).willReturn(null);
        BDDMockito.given(submission.getId()).willReturn(submissionId);
        BDDMockito.given(submission.getProblem()).willReturn(problem);
        BDDMockito.given(problem.getProblemNo()).willReturn(1001);
        BDDMockito.given(problem.getTitle()).willReturn("A+B");
        BDDMockito.given(problem.getPlatform()).willReturn(Platform.SWEA);
        BDDMockito.given(submission.getSourceCode()).willReturn("public class Main {}");
        BDDMockito.given(submission.getLanguage()).willReturn(Language.JAVA);
        BDDMockito.given(submission.getExecutionTime()).willReturn(1.23);
        BDDMockito.given(submission.getCreatedAt()).willReturn(createdAt);

        // when
        SubmissionDto.Detail result = submissionService.detail(submissionId);

        // then
        Assertions.assertThat(result.getCounterExampleCount()).isZero();
        Assertions.assertThat(result.getCounterExamples()).isEmpty();
        verifyNoInteractions(objectMapper);
    }
}
