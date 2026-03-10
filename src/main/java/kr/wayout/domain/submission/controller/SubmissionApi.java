package kr.wayout.domain.submission.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.wayout.domain.submission.dto.SubmissionDto;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "[제출 API]", description = "반례 탐색 요청 API")
public interface SubmissionApi {

    @Operation(summary = "반례 탐색 API", description = "사용자의 코드를 입력 받아 반례 케이스를 찾습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "반례 찾기 요청 성공 - 반례 발견의 유무와는 다름",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SubmissionDto.CreateCounterExampleResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청값 오류")
    })
    ResponseEntity<?> createCounterExample(String email, SubmissionDto.CreateCounterExampleRequest dto);

    @Operation(summary = "제출 목록 조회 API", description = "사용자들의 제출 기록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제출 기록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SubmissionDto.ListResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청값 오류")

    })
    ResponseEntity<?> list(@ParameterObject Pageable pageable);

    @Operation(summary = "특정 문제의 제출 목록 조회 API", description = "특정 문제에 대하여 사용자들의 제출 기록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제출 기록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SubmissionDto.ListResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청값 오류")

    })
    ResponseEntity<?> listByProblem(@ParameterObject Pageable pageable, @Parameter(description = "문제 id값") Long problemId);
}
