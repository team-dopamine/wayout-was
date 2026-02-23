package kr.wayout.domain.problem.controller;

import kr.wayout.domain.problem.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/problems")
public class ProblemController implements ProblemApi {

    private final ProblemService problemService;

    @Override
    @GetMapping
    public ResponseEntity<?> list(Pageable pageable) {
        return ResponseEntity.ok(problemService.list(pageable));
    }
}
