package kr.wayout.domain.testcase;

import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberRepository;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.problem.ProblemRepository;
import kr.wayout.domain.testcase.dto.TestcaseDto;
import kr.wayout.global.exception.CustomBusinessException;
import kr.wayout.global.exception.ErrorCode;
import kr.wayout.infrastructure.runner.DockerCounterExampleRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestcaseService {

    private final MemberRepository memberRepository;
    private final ProblemRepository problemRepository;
    private final TestcaseRepository testcaseRepository;
    private final DockerCounterExampleRunner dockerCounterExampleRunner;

    @Transactional
    public TestcaseDto.CreateResponse create(String email, TestcaseDto.CreateRequest dto) {

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Problem problem = problemRepository.findProblemById(dto.getProblemId());

        if (problem == null) {
            throw new CustomBusinessException(ErrorCode.PROBLEM_NOT_FOUND);
        }

        boolean isValid = dockerCounterExampleRunner.validateSingleCase(problem, dto.getInput());
        if (!isValid) {
            throw new CustomBusinessException(ErrorCode.INVALID_TESTCASE_INPUT);
        }

        save(member, problem, dto.getInput(), dto.getOutput());

        return TestcaseDto.CreateResponse.of("테스트 케이스 등록에 성공하였습니다.");
    }

    @Transactional
    public void save(Member member, Problem problem, String input, String output) {
        Testcase testcase = Testcase.builder()
                .input(input)
                .output(output)
                .member(member)
                .problem(problem)
                .build();

        testcaseRepository.save(testcase);
    }

}
