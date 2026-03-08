package kr.wayout.domain.submission;

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

        Submission submission = Submission.create(
                member,
                problem,
                dto.getLanguage(),
                dto.getSourceCode(),
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
                            submission.getMember() != null ? submission.getMember().getNickname() : "익명",
                            submission.getProblem().getTitle(),
                            submission.getLanguage(),
                            submission.getExecutionTime(),
                            submission.getCreatedAt()
                    );
                })
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    private Member resolveMember(String email) {
        if (email == null || "anonymousUser".equals(email)) {
            return null;
        }

        return memberService.read(email);
    }

}
