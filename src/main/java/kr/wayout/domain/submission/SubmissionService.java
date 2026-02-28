package kr.wayout.domain.submission;

import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.problem.ProblemRepository;
import kr.wayout.domain.submission.dto.SubmissionDto;
import kr.wayout.domain.submission.runner.CounterExampleRunResult;
import kr.wayout.domain.submission.runner.CounterExampleRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문제입니다."));

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

    private Member resolveMember(String email) {
        if (email == null || "anonymousUser".equals(email)) {
            return null;
        }

        return memberService.read(email);
    }

}
