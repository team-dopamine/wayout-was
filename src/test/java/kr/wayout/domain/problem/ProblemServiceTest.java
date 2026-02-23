package kr.wayout.domain.problem;

import kr.wayout.domain.problem.dto.ProblemDto;
import kr.wayout.domain.submission.SubmissionRepository;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @InjectMocks
    private ProblemService problemService;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Test
    @DisplayName("문제 목록 조회 서비스 - 문제와 제출 집계를 병합해 반환한다")
    void list_success_with_aggregation() {
        // given
        Problem problem = Mockito.mock(Problem.class);
        BDDMockito.given(problem.getId()).willReturn(1L);
        BDDMockito.given(problem.getProblemNo()).willReturn(1000);
        BDDMockito.given(problem.getTitle()).willReturn("A+B");
        BDDMockito.given(problem.getPlatform()).willReturn(Platform.SWEA);

        PageRequest pageable = PageRequest.of(0, 8);
        Page<Problem> problemPage = new PageImpl<>(List.of(problem), pageable, 1);

        SubmissionRepository.SubmissionCountRow countRow = new SubmissionRepository.SubmissionCountRow() {
            @Override
            public Long getProblemId() {
                return 1L;
            }

            @Override
            public long getTotalSubmissions() {
                return 12L;
            }

            @Override
            public long getFoundSubmissions() {
                return 3L;
            }
        };

        BDDMockito.given(problemRepository.findAll(pageable)).willReturn(problemPage);
        BDDMockito.given(submissionRepository.countByProblemIds(List.of(1L))).willReturn(List.of(countRow));

        // when
        Page<ProblemDto.List> result = problemService.list(pageable);

        // then
        Assertions.assertThat(result.getContent()).hasSize(1);
        ProblemDto.List item = result.getContent().get(0);
        Assertions.assertThat(item.getProblemId()).isEqualTo(1L);
        Assertions.assertThat(item.getProblemNo()).isEqualTo(1000);
        Assertions.assertThat(item.getTitle()).isEqualTo("A+B");
        Assertions.assertThat(item.getPlatform()).isEqualTo(Platform.SWEA);
        Assertions.assertThat(item.getTotalSubmissions()).isEqualTo(12L);
        Assertions.assertThat(item.getFoundSubmissions()).isEqualTo(3L);
        Assertions.assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("문제 목록 조회 서비스 - 문제가 없으면 빈 페이지를 반환한다")
    void list_empty_page() {
        // given
        PageRequest pageable = PageRequest.of(0, 8);
        BDDMockito.given(problemRepository.findAll(pageable))
                .willReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        // when
        Page<ProblemDto.List> result = problemService.list(pageable);

        // then
        Assertions.assertThat(result.getContent()).isEmpty();
        Assertions.assertThat(result.getTotalElements()).isZero();
        verify(submissionRepository, never()).countByProblemIds(anyList());
    }
}
