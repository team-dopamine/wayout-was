package kr.wayout.domain.testcase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.wayout.domain.testcase.dto.TestcaseDto;
import org.springframework.http.ResponseEntity;

@Tag(name = "[테스트 케이스 API]", description = "커스텀 테스트 케이스 및 엣지 케이스 관리를 위한 API")
public interface TestcaseApi {

    @Operation(summary = "테스트 케이스 등록 API", description = "사용자가 테스트 케이스를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "테스트 케이스 등록 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TestcaseDto.CreateResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 입력")
    })
    ResponseEntity<?> create(String email, TestcaseDto.CreateRequest dto);
}
