package kr.wayout.domain.submission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.problem.ProblemRepository;
import kr.wayout.domain.submission.dto.SubmissionDto;
import kr.wayout.domain.submission.runner.CounterExampleRunResult;
import kr.wayout.domain.submission.runner.CounterExampleRunner;
import kr.wayout.global.exception.CustomBusinessException;
import kr.wayout.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubmissionService {

    private final MemberService memberService;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final CounterExampleRunner counterExampleRunner;
    private final ObjectMapper objectMapper;

    @Transactional
    public SubmissionDto.CreateCounterExampleResponse createCounterExample(String email,
                                                                           SubmissionDto.CreateCounterExampleRequest dto) {
        Member member = resolveMember(email);
        Problem problem = problemRepository.findById(dto.getProblemId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        CounterExampleRunResult result = counterExampleRunner.run(
                dto.getProblemId(),
                dto.getSourceCode(),
                dto.getLanguage()
        );

        JsonNode counterExamples = objectMapper.valueToTree(
                result.getCounterExamples() == null ? List.of() : result.getCounterExamples()
        );

        Submission submission = Submission.create(
                member,
                problem,
                dto.getLanguage(),
                dto.getSourceCode(),
                counterExamples,
                dto.getIsOpen(),
                result.isFound(),
                result.getExecutionTime()
        );
        submissionRepository.save(submission);

        String status = result.isFound() ? "FOUND" : "NOT_FOUND";
        String message = result.isFound() ? "반례를 찾았습니다." : "반례를 찾지 못했습니다.";

        return SubmissionDto.CreateCounterExampleResponse.builder()
                .status(status)
                .message(message)
                .found(result.isFound())
                .executionTime(result.getExecutionTime())
                .counterExamples(result.getCounterExamples())
                .outputFilePath(result.getOutputFilePath())
                .build();
    }

    public Page<SubmissionDto.ListResponse> list(Pageable pageable) {

        Page<Submission> page = submissionRepository.findAll(pageable);

        List<Submission> submissions = page.getContent();
        if (submissions.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        List<SubmissionDto.ListResponse> content = submissions.stream()
                .map(submission -> {
                    return new SubmissionDto.ListResponse(
                            submission.getId(),
                            submission.getProblem().getProblemNo(),
                            submission.getMember() != null ? submission.getMember().getNickname() : "익명",
                            submission.getProblem().getTitle(),
                            submission.getLanguage(),
                            submission.getProblem().getPlatform(),
                            submission.getExecutionTime(),
                            countCounterExamples(submission),
                            submission.getCreatedAt(),
                            submission.getIsOpen()
                    );
                })
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public Page<SubmissionDto.ListResponse> listByProblemId(Pageable pageable, Long problemId) {

        Problem problem = problemRepository.findProblemById(problemId);
        if (problem == null) {
            throw new CustomBusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        Page<Submission> page = submissionRepository.findAllByProblem(pageable, problem);
        List<Submission> submissions = page.getContent();
        if (submissions.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        List<SubmissionDto.ListResponse> content = submissions.stream()
                .map(submission -> {
                    return new SubmissionDto.ListResponse(
                            submission.getId(),
                            submission.getProblem().getProblemNo(),
                            submission.getMember() != null ? submission.getMember().getNickname() : "익명",
                            problem.getTitle(),
                            submission.getLanguage(),
                            problem.getPlatform(),
                            submission.getExecutionTime(),
                            countCounterExamples(submission),
                            submission.getCreatedAt(),
                            submission.getIsOpen()
                    );
                })
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public SubmissionDto.Detail detail(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.SUBMISSION_NOT_FOUND));

        if (!isDetailViewAllowed(submission)) {
            throw new CustomBusinessException(ErrorCode.SUBMISSION_NOT_OPENED);
        }

        List<SubmissionDto.CounterExampleCase> counterExamples = List.of();
        JsonNode counterExamplesNode = submission.getCounterExamples();
        if (counterExamplesNode != null && !counterExamplesNode.isNull()) {
            counterExamples = objectMapper.convertValue(
                    counterExamplesNode,
                    new TypeReference<List<SubmissionDto.CounterExampleCase>>() {
                    }
            );
        }

        return SubmissionDto.Detail.builder()
                .id(submission.getId())
                .problemNo(submission.getProblem().getProblemNo().longValue())
                .title(submission.getProblem().getTitle())
                .sourceCode(submission.getSourceCode())
                .counterExampleCount(counterExamples.size())
                .counterExamples(counterExamples)
                .language(submission.getLanguage())
                .platform(submission.getProblem().getPlatform())
                .executionTime(submission.getExecutionTime())
                .createdAt(submission.getCreatedAt())
                .build();
    }

    private Member resolveMember(String email) {
        if (email == null || "anonymousUser".equals(email)) {
            return null;
        }

        return memberService.read(email);
    }

    private boolean isDetailViewAllowed(Submission submission) {
        return Boolean.TRUE.equals(submission.getIsOpen());
    }

    private int countCounterExamples(Submission submission) {
        JsonNode counterExamplesNode = submission.getCounterExamples();
        if (counterExamplesNode == null || counterExamplesNode.isNull()) {
            return 0;
        }

        return counterExamplesNode.size();
    }

}
