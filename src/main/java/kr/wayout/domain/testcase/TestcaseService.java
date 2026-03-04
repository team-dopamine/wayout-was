package kr.wayout.domain.testcase;

import jakarta.persistence.EntityNotFoundException;
import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberRepository;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.problem.ProblemRepository;
import kr.wayout.domain.testcase.dto.TestcaseDto;
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
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 사용자입니다."));

        Problem problem = problemRepository.findProblemById(dto.getProblemId());

        if (problem == null) {
            throw new EntityNotFoundException("존재하지 않는 문제입니다.");
        }

        dockerCounterExampleRunner.validateSingleCase(problem, dto.getInput());

        Testcase testcase = Testcase.builder()
                .input(dto.getInput())
                .output(dto.getOutput())
                .member(member)
                .problem(problem)
                .build();

        testcaseRepository.save(testcase);
        return TestcaseDto.CreateResponse.of("테스트 케이스 등록에 성공하였습니다.");
    }

}
