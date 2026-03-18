package kr.wayout.domain.solution;

import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.problem.Platform;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.problem.ProblemRepository;
import kr.wayout.domain.solution.dto.SolutionDto;
import kr.wayout.domain.submission.Language;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SolutionServiceTest {

    @InjectMocks
    private SolutionService solutionService;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private SolutionRepository solutionRepository;

    @Test
    @DisplayName("정답 코드 최초 등록 시 version은 1")
    void create_firstVersion_success() {
        // given
        String email = "test@gmail.com";
        SolutionDto.CreateRequest request = new SolutionDto.CreateRequest(1L, Language.JAVA, true, "public class Main {}");
        Member member = Mockito.mock(Member.class);
        Problem problem = Mockito.mock(Problem.class);
        Solution saved = Mockito.mock(Solution.class);

        BDDMockito.given(memberService.read(email)).willReturn(member);
        BDDMockito.given(problemRepository.findById(1L)).willReturn(Optional.of(problem));
        BDDMockito.given(solutionRepository.findTopByProblemOrderByVersionDesc(problem)).willReturn(Optional.empty());
        BDDMockito.given(solutionRepository.save(any(Solution.class))).willReturn(saved);

        // when
        SolutionDto.CreateResponse response = solutionService.create(email, request);

        // then
        Assertions.assertThat(response.getMessage()).isEqualTo("정답 코드 등록에 성공했습니다.");
    }

    @Test
    @DisplayName("기존 정답 코드가 있으면 version을 1 증가시켜 등록")
    void create_incrementVersion_success() {
        // given
        String email = "test@gmail.com";

        SolutionDto.CreateRequest request = new SolutionDto.CreateRequest(1L, Language.JAVA, true, "public class Main {}");
        Member member = Mockito.mock(Member.class);
        Problem problem = Mockito.mock(Problem.class);
        Solution lastSolution = Mockito.mock(Solution.class);
        Solution saved = Mockito.mock(Solution.class);

        BDDMockito.given(memberService.read(email)).willReturn(member);
        BDDMockito.given(problemRepository.findById(1L)).willReturn(Optional.of(problem));
        BDDMockito.given(solutionRepository.findTopByProblemOrderByVersionDesc(problem)).willReturn(Optional.of(lastSolution));
        BDDMockito.given(lastSolution.getVersion()).willReturn(3);
        BDDMockito.given(solutionRepository.save(any(Solution.class))).willReturn(saved);

        // when
        SolutionDto.CreateResponse response = solutionService.create(email, request);

        // then
        ArgumentCaptor<Solution> captor = ArgumentCaptor.forClass(Solution.class);
        verify(solutionRepository).save(captor.capture());
        Assertions.assertThat(captor.getValue().getVersion()).isEqualTo(4);
        Assertions.assertThat(captor.getValue().getLanguage()).isEqualTo(Language.JAVA);
        Assertions.assertThat(captor.getValue().getIsOpen()).isTrue();
        Assertions.assertThat(response.getMessage()).isEqualTo("정답 코드 등록에 성공했습니다.");
    }

    @Test
    @DisplayName("내 정답 코드 기여 조회 - 응답 필드를 매핑해 반환한다")
    void listContributions_success() {
        // given
        String email = "test@gmail.com";
        PageRequest pageable = PageRequest.of(0, 8);
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 17, 10, 30);

        Member member = Mockito.mock(Member.class);
        Problem problem = Mockito.mock(Problem.class);
        Solution solution = Mockito.mock(Solution.class);

        BDDMockito.given(memberService.read(email)).willReturn(member);
        BDDMockito.given(member.getId()).willReturn(1L);
        BDDMockito.given(problem.getId()).willReturn(101L);
        BDDMockito.given(problem.getProblemNo()).willReturn(1234);
        BDDMockito.given(problem.getPlatform()).willReturn(Platform.SWEA);
        BDDMockito.given(problem.getTitle()).willReturn("A+B");
        BDDMockito.given(solution.getProblem()).willReturn(problem);
        BDDMockito.given(solution.getLanguage()).willReturn(Language.JAVA);
        BDDMockito.given(solution.getCreatedAt()).willReturn(createdAt);
        BDDMockito.given(solutionRepository.findAllByMemberIdWithProblem(1L, pageable))
                .willReturn(new PageImpl<>(List.of(solution), pageable, 1));

        // when
        Page<SolutionDto.ContributionResponse> result = solutionService.listContributions(email, pageable);

        // then
        Assertions.assertThat(result.getTotalElements()).isEqualTo(1);
        Assertions.assertThat(result.getContent()).hasSize(1);
        SolutionDto.ContributionResponse item = result.getContent().get(0);
        Assertions.assertThat(item.getProblemId()).isEqualTo(101L);
        Assertions.assertThat(item.getProblemNo()).isEqualTo(1234);
        Assertions.assertThat(item.getPlatform()).isEqualTo(Platform.SWEA);
        Assertions.assertThat(item.getProblemTitle()).isEqualTo("A+B");
        Assertions.assertThat(item.getLanguage()).isEqualTo(Language.JAVA);
        Assertions.assertThat(item.getSubmissionDate()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("내 정답 코드 기여 조회 - 데이터가 없으면 빈 페이지를 반환한다")
    void listContributions_empty() {
        // given
        String email = "test@gmail.com";
        PageRequest pageable = PageRequest.of(0, 8);
        Member member = Mockito.mock(Member.class);

        BDDMockito.given(memberService.read(email)).willReturn(member);
        BDDMockito.given(member.getId()).willReturn(1L);
        BDDMockito.given(solutionRepository.findAllByMemberIdWithProblem(1L, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<SolutionDto.ContributionResponse> result = solutionService.listContributions(email, pageable);

        // then
        Assertions.assertThat(result.getContent()).isEmpty();
        Assertions.assertThat(result.getTotalElements()).isZero();
    }
}
