package kr.wayout.domain.problem.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.wayout.domain.problem.dto.ProblemDto;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "[문제 관련 API]", description = "반례를 찾을 수 있도록 등록된 문제에 대한 API")
public interface ProblemApi {

    @Operation(summary = "문제 목록 조회 API", description = "등록된 문제 목록을 페이지 단위로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "문제 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDto.List.class))),
            @ApiResponse(responseCode = "400", description = "요청값 오류")
    })
    ResponseEntity<?> list(@ParameterObject Pageable pageable);

    @Operation(summary = "문제 검색 API", description = "문제 제목 또는 문제 번호를 키워드로 검색해 최대 limit개까지 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "문제 검색 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDto.Search.class))),
            @ApiResponse(responseCode = "400", description = "요청값 오류")
    })
    ResponseEntity<?> search(
            @Parameter(description = "검색 키워드 (제목은 2글자 이상, 문제 번호는 숫자 입력)") String keyword,
            @Parameter(description = "반환 개수 (기본 10, 최대 10)") Integer limit
    );

}
