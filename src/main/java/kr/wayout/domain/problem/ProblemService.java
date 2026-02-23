package kr.wayout.domain.problem;

import kr.wayout.domain.problem.dto.ProblemDto;
import kr.wayout.domain.submission.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    private record SubmissionAgg(long total, long found) {
        static final SubmissionAgg ZERO = new SubmissionAgg(0, 0);
    }

    @Transactional(readOnly = true)
    public Page<ProblemDto.List> list(Pageable pageable) {

        Page<Problem> page = problemRepository.findAll(pageable);

        List<Problem> problems = page.getContent();
        if (problems.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        List<Long> problemIds = page.getContent().stream()
                .map(Problem::getId)
                .toList();

        Map<Long, SubmissionAgg> aggMap = toAggMap(problemIds);

        List<ProblemDto.List> content = problems.stream()
                .map(p -> {
                    SubmissionAgg agg = aggMap.getOrDefault(p.getId(), SubmissionAgg.ZERO);

                    return new ProblemDto.List(
                            p.getId(),
                            p.getProblemNo(),
                            p.getTitle(),
                            p.getPlatform(),
                            agg.total,
                            agg.found
                    );
                })
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    private Map<Long, SubmissionAgg> toAggMap(List<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) return Collections.emptyMap();

        return submissionRepository.countByProblemIds(problemIds).stream()
                .collect(Collectors.toMap(
                        SubmissionRepository.SubmissionCountRow::getProblemId,
                        r -> new SubmissionAgg(r.getTotalSubmissions(), r.getFoundSubmissions()),
                        // 혹시 모를 중복 키 방어 (원칙상 중복 없어야 정상)
                        (a, b) -> a
                ));
    }

}
