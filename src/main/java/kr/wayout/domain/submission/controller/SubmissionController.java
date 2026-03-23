package kr.wayout.domain.submission.controller;

import jakarta.validation.Valid;
import kr.wayout.domain.submission.SubmissionService;
import kr.wayout.domain.submission.dto.SubmissionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubmissionController implements SubmissionApi {

    private final SubmissionService submissionService;

    @Override
    @PostMapping("/submissions/counter-examples")
    public ResponseEntity<?> createCounterExample(@AuthenticationPrincipal String email,
                                                  @Valid @RequestBody SubmissionDto.CreateCounterExampleRequest dto) {
        return ResponseEntity.ok(submissionService.createCounterExample(email, dto));
    }

    @Override
    @GetMapping("/submissions")
    public ResponseEntity<?> list(@PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(submissionService.list(pageable));
    }

    @Override
    @GetMapping("/problems/{problemId}/submissions")
    public ResponseEntity<?> listByProblem(@PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                           @PathVariable Long problemId) {
        return ResponseEntity.ok(submissionService.listByProblemId(pageable, problemId));
    }

    @Override
    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<?> detail(@PathVariable Long submissionId) {
        return ResponseEntity.ok(submissionService.detail(submissionId));
    }

}
