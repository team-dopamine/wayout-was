package kr.wayout.domain.solution.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.wayout.domain.solution.dto.SolutionDto;
import org.springframework.http.ResponseEntity;

@Tag(name = "[정답 코드 API]", description = "정답 코드 등록 관련 API")
public interface SolutionApi {

    @Operation(summary = "정답 코드 등록 API", description = "문제에 대한 정답 코드를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정답 코드 등록 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SolutionDto.CreateResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "400", description = "요청값 오류"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 문제")
    })
    ResponseEntity<?> create(String email, SolutionDto.CreateRequest dto);
}
