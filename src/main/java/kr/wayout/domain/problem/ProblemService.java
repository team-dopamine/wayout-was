package kr.wayout.domain.problem;

import kr.wayout.domain.problem.dto.ProblemDto;
import kr.wayout.domain.submission.SubmissionRepository;
import kr.wayout.global.exception.CustomBusinessException;
import kr.wayout.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private static final int MIN_SEARCH_KEYWORD_LENGTH = 2;
    private static final int SEARCH_LIMIT = 10;

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

    @Transactional(readOnly = true)
    public List<ProblemDto.Search> search(String keyword, Integer limit) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            return List.of();
        }

        String[] tokens = normalizedKeyword.split("\\s+");
        Integer problemNo = null;
        List<String> titleTokens = new ArrayList<>();

        for (String token : tokens) {
            Integer parsedProblemNo = (problemNo == null) ? parseProblemNoToken(token) : null;
            if (parsedProblemNo != null) {
                problemNo = parsedProblemNo;
                continue;
            }
            titleTokens.add(token);
        }

        String titleKeyword = titleTokens.isEmpty() ? null : String.join(" ", titleTokens);
        Pageable pageable = createSearchPageable(limit);

        if (problemNo != null) {
            List<Problem> byProblemNo = problemRepository.search(problemNo, null, pageable);
            if (!byProblemNo.isEmpty()) {
                return byProblemNo.stream().map(ProblemDto.Search::from).toList();
            }

            String fallbackTitleKeyword = (titleKeyword != null) ? titleKeyword : normalizedKeyword;
            if (fallbackTitleKeyword.length() < MIN_SEARCH_KEYWORD_LENGTH) {
                return List.of();
            }

            return problemRepository.search(null, toLikeKeyword(fallbackTitleKeyword), pageable).stream()
                    .map(ProblemDto.Search::from)
                    .toList();
        }

        if (titleKeyword == null || titleKeyword.length() < MIN_SEARCH_KEYWORD_LENGTH) {
            return List.of();
        }

        return problemRepository.search(null, toLikeKeyword(titleKeyword), pageable).stream()
                .map(ProblemDto.Search::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProblemDto.Detail detail(Long problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        SubmissionAgg agg = toAggMap(List.of(problemId))
                .getOrDefault(problemId, SubmissionAgg.ZERO);

        return new ProblemDto.Detail(
                problem.getId(),
                problem.getProblemNo(),
                problem.getTitle(),
                problem.getPlatform(),
                agg.total,
                agg.found
        );
    }

    private String toLikeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        return "%" + keyword.toLowerCase(Locale.ROOT) + "%";
    }

    private Integer parseProblemNoToken(String token) {
        if (token == null || token.isBlank() || !token.chars().allMatch(Character::isDigit)) {
            return null;
        }

        try {
            return Integer.valueOf(token);
        } catch (NumberFormatException ignored) {
            return null;
        }
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

    private int sanitizeLimit(Integer limit) {
        int requested = (limit == null) ? SEARCH_LIMIT : limit;
        if (requested < 1) {
            throw new IllegalArgumentException("limit은 1 이상이어야 합니다.");
        }

        return Math.min(requested, SEARCH_LIMIT);
    }

    private Pageable createSearchPageable(Integer limit) {
        return PageRequest.of(0, sanitizeLimit(limit), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

}
