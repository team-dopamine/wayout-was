package kr.wayout.domain.submission;

import kr.wayout.domain.member.Member;
import kr.wayout.domain.member.MemberService;
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
    private final CounterExampleRunner counterExampleRunner;

    @Transactional
    public SubmissionDto.CreateCounterExampleResponse createCounterExample(String email,
                                                                           SubmissionDto.CreateCounterExampleRequest dto) {
        resolveSubmitter(email);

        // TODO: 다음 단계에서 아래 로직을 순서대로 구현
        // 1) runner에서 problem/generator/validator/solution 조회 및 실행
        CounterExampleRunResult result = counterExampleRunner.run(dto.getProblemId());
        // 2) submission 저장
        // 3) docker 실행
        // 4) 결과 저장 및 응답 구성
        return SubmissionDto.CreateCounterExampleResponse.builder()
                .status("PENDING")
                .message("반례 탐색 요청이 접수되었습니다.")
                .build();
    }

    private String resolveSubmitter(String email) {
        if (email == null || "anonymousUser".equals(email)) {
            return "anonymousUser";
        }

        Member member = memberService.read(email);

        return member.getNickname();
    }
}
