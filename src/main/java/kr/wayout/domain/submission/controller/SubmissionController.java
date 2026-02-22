package kr.wayout.domain.submission.controller;

import jakarta.validation.Valid;
import kr.wayout.domain.submission.SubmissionService;
import kr.wayout.domain.submission.dto.SubmissionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/submissions")
public class SubmissionController implements SubmissionApi {

    private final SubmissionService submissionService;

    @Override
    @PostMapping("/counter-examples")
    public ResponseEntity<?> createCounterExample(@AuthenticationPrincipal String email,
                                                  @Valid @RequestBody SubmissionDto.CreateCounterExampleRequest dto) {
        return ResponseEntity.ok(submissionService.createCounterExample(email, dto));
    }
}
