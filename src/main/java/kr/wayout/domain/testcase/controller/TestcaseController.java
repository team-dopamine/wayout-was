package kr.wayout.domain.testcase.controller;

import kr.wayout.domain.testcase.TestcaseService;
import kr.wayout.domain.testcase.dto.TestcaseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/testcases")
public class TestcaseController implements TestcaseApi {

    private final TestcaseService testcaseService;

    @Override
    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal String email, @RequestBody TestcaseDto.CreateRequest dto) {
        return ResponseEntity.ok(testcaseService.create(email, dto));
    }
}
