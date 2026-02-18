package kr.wayout.domain.solution.controller;

import jakarta.validation.Valid;
import kr.wayout.domain.solution.SolutionService;
import kr.wayout.domain.solution.dto.SolutionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/solutions")
public class SolutionController implements SolutionApi {

    private final SolutionService solutionService;

    @Override
    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal String email,
                                    @Valid @RequestBody SolutionDto.CreateRequest dto) {
        if (email == null || "anonymousUser".equals(email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(solutionService.create(email, dto));
    }
}
