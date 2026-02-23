package kr.wayout.domain.problem.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;

@Tag(name = "[문제 관련 API]", description = "반례를 찾을 수 있도록 등록된 문제에 대한 API")
public interface ProblemApi {

    ResponseEntity<?> list(@PageableDefault(size = 8, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable);

}
