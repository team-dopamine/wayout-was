package kr.wayout.domain.solution;

import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.problem.ProblemRepository;
import kr.wayout.domain.solution.dto.SolutionDto;
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
        SolutionDto.CreateRequest request = new SolutionDto.CreateRequest(1L, "public class Main {}");
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

        SolutionDto.CreateRequest request = new SolutionDto.CreateRequest(1L, "public class Main {}");
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
        Assertions.assertThat(response.getMessage()).isEqualTo("정답 코드 등록에 성공했습니다.");
    }
}
