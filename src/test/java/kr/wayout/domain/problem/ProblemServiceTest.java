package kr.wayout.domain.problem;

import kr.wayout.domain.problem.dto.ProblemDto;
import kr.wayout.domain.submission.SubmissionRepository;
import kr.wayout.global.exception.CustomBusinessException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
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

    @Test
    @DisplayName("문제 상세 조회 서비스 - 문제 정보와 제출 집계를 반환한다")
    void detail_success_with_aggregation() {
        // given
        Problem problem = Mockito.mock(Problem.class);
        BDDMockito.given(problem.getId()).willReturn(1L);
        BDDMockito.given(problem.getProblemNo()).willReturn(1000);
        BDDMockito.given(problem.getTitle()).willReturn("A+B");
        BDDMockito.given(problem.getPlatform()).willReturn(Platform.SWEA);

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

        BDDMockito.given(problemRepository.findById(1L)).willReturn(Optional.of(problem));
        BDDMockito.given(submissionRepository.countByProblemIds(List.of(1L))).willReturn(List.of(countRow));

        // when
        ProblemDto.Detail result = problemService.detail(1L);

        // then
        Assertions.assertThat(result.getProblemId()).isEqualTo(1L);
        Assertions.assertThat(result.getProblemNo()).isEqualTo(1000);
        Assertions.assertThat(result.getTitle()).isEqualTo("A+B");
        Assertions.assertThat(result.getPlatform()).isEqualTo(Platform.SWEA);
        Assertions.assertThat(result.getTotalSubmissions()).isEqualTo(12L);
        Assertions.assertThat(result.getFoundSubmissions()).isEqualTo(3L);
    }

    @Test
    @DisplayName("문제 상세 조회 서비스 - 제출이 없으면 집계를 0으로 반환한다")
    void detail_success_without_submission() {
        // given
        Problem problem = Mockito.mock(Problem.class);
        BDDMockito.given(problem.getId()).willReturn(2L);
        BDDMockito.given(problem.getProblemNo()).willReturn(2000);
        BDDMockito.given(problem.getTitle()).willReturn("B+C");
        BDDMockito.given(problem.getPlatform()).willReturn(Platform.SWEA);

        BDDMockito.given(problemRepository.findById(2L)).willReturn(Optional.of(problem));
        BDDMockito.given(submissionRepository.countByProblemIds(List.of(2L))).willReturn(List.of());

        // when
        ProblemDto.Detail result = problemService.detail(2L);

        // then
        Assertions.assertThat(result.getTotalSubmissions()).isZero();
        Assertions.assertThat(result.getFoundSubmissions()).isZero();
    }

    @Test
    @DisplayName("문제 상세 조회 서비스 - 존재하지 않는 문제면 예외를 던진다")
    void detail_throws_when_problem_not_found() {
        // given
        BDDMockito.given(problemRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        Assertions.assertThatThrownBy(() -> problemService.detail(999L))
                .isInstanceOf(CustomBusinessException.class)
                .hasMessage("존재하지 않는 문제입니다.");
        verify(submissionRepository, never()).countByProblemIds(anyList());
    }

    @Test
    @DisplayName("문제 제목 검색 서비스 - 키워드가 2글자 미만이면 빈 목록을 반환한다")
    void search_by_title_returns_empty_when_keyword_too_short() {
        // when
        List<ProblemDto.Search> result = problemService.search(" A ", 10);

        // then
        Assertions.assertThat(result).isEmpty();
        verify(problemRepository, never()).search(any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("문제 제목 검색 서비스 - 제목 포함 검색 결과를 DTO로 반환한다")
    void search_by_title_success() {
        // given
        Problem problem = Mockito.mock(Problem.class);
        BDDMockito.given(problem.getId()).willReturn(3L);
        BDDMockito.given(problem.getProblemNo()).willReturn(1200);
        BDDMockito.given(problem.getTitle()).willReturn("A+B");
        BDDMockito.given(problem.getPlatform()).willReturn(Platform.SWEA);

        BDDMockito.given(problemRepository.search(isNull(), eq("%a+b%"), any(Pageable.class)))
                .willReturn(List.of(problem));

        // when
        List<ProblemDto.Search> result = problemService.search("  A+B  ", 20);

        // then
        Assertions.assertThat(result).hasSize(1);
        ProblemDto.Search item = result.getFirst();
        Assertions.assertThat(item.getProblemId()).isEqualTo(3L);
        Assertions.assertThat(item.getProblemNo()).isEqualTo(1200);
        Assertions.assertThat(item.getTitle()).isEqualTo("A+B");
        Assertions.assertThat(item.getPlatform()).isEqualTo(Platform.SWEA);
    }

    @Test
    @DisplayName("문제 검색 서비스 - 숫자 키워드는 문제 번호로도 검색한다")
    void search_by_problem_no_success() {
        // given
        Problem problem = Mockito.mock(Problem.class);
        BDDMockito.given(problem.getId()).willReturn(11L);
        BDDMockito.given(problem.getProblemNo()).willReturn(1000);
        BDDMockito.given(problem.getTitle()).willReturn("A+B");
        BDDMockito.given(problem.getPlatform()).willReturn(Platform.SWEA);

        BDDMockito.given(problemRepository.search(eq(1000), isNull(), any(Pageable.class)))
                .willReturn(List.of(problem));

        // when
        List<ProblemDto.Search> result = problemService.search("1000", 20);

        // then
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.getFirst().getProblemNo()).isEqualTo(1000);
        verify(problemRepository).search(eq(1000), isNull(), any(Pageable.class));
        verify(problemRepository, never()).search(isNull(), eq("1000"), any(Pageable.class));
    }

    @Test
    @DisplayName("문제 검색 서비스 - 문제 번호 검색 결과가 없으면 제목 검색으로 fallback 한다")
    void search_fallback_to_title_when_problem_no_not_found() {
        // given
        Problem problem = Mockito.mock(Problem.class);
        BDDMockito.given(problem.getId()).willReturn(12L);
        BDDMockito.given(problem.getProblemNo()).willReturn(1000);
        BDDMockito.given(problem.getTitle()).willReturn("A+B");
        BDDMockito.given(problem.getPlatform()).willReturn(Platform.SWEA);

        BDDMockito.given(problemRepository.search(eq(1000), isNull(), any(Pageable.class)))
                .willReturn(List.of());
        BDDMockito.given(problemRepository.search(isNull(), eq("%a+b%"), any(Pageable.class)))
                .willReturn(List.of(problem));

        // when
        List<ProblemDto.Search> result = problemService.search("1000 A+B", 20);

        // then
        Assertions.assertThat(result).hasSize(1);
        InOrder inOrder = inOrder(problemRepository);
        inOrder.verify(problemRepository).search(eq(1000), isNull(), any(Pageable.class));
        inOrder.verify(problemRepository).search(isNull(), eq("%a+b%"), any(Pageable.class));
    }

    @Test
    @DisplayName("문제 검색 서비스 - limit이 0 이하면 예외를 던진다")
    void search_throws_when_limit_invalid() {
        // when & then
        Assertions.assertThatThrownBy(() -> problemService.search("AB", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limit은 1 이상이어야 합니다.");
    }

    @Test
    @DisplayName("문제 검색 서비스 - limit은 최대 10으로 제한된다")
    void search_clamps_limit_to_max() {
        // given
        BDDMockito.given(problemRepository.search(isNull(), eq("%ab%"), any(Pageable.class)))
                .willReturn(List.of());

        // when
        problemService.search("AB", 100);

        // then
        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(problemRepository).search(isNull(), eq("%ab%"), captor.capture());
        Assertions.assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }
}
