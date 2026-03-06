package kr.wayout.domain.solution;

import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberService;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.problem.ProblemRepository;
import kr.wayout.domain.solution.dto.SolutionDto;
import kr.wayout.global.exception.CustomBusinessException;
import kr.wayout.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SolutionService {

    private final MemberService memberService;
    private final ProblemRepository problemRepository;
    private final SolutionRepository solutionRepository;

    @Transactional
    public SolutionDto.CreateResponse create(String email, SolutionDto.CreateRequest dto) {
        Member member = memberService.read(email);

        Problem problem = problemRepository.findById(dto.getProblemId())
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.PROBLEM_NOT_FOUND));

        int nextVersion = solutionRepository.findTopByProblemOrderByVersionDesc(problem)
                .map(solution -> solution.getVersion() + 1)
                .orElse(1);

        Solution solution = Solution.create(
                member,
                problem,
                nextVersion,
                dto.getSourceCode(),
                dto.getLanguage(),
                dto.getIsOpen()
        );
        solutionRepository.save(solution);

        return SolutionDto.CreateResponse.of("정답 코드 등록에 성공했습니다.");
    }
}
