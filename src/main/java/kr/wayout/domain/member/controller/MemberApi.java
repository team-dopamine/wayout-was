package kr.wayout.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import kr.wayout.domain.member.dto.UpdateNicknameDto;
import org.springframework.http.ResponseEntity;

@Tag(name = "[사용자 관련 API]", description = "사용자 정보 관련 API")
public interface MemberApi {

    @Operation(summary = "닉네임 변경 API", description = "사용자의 닉네임을 변경하기 위한 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임 변경 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UpdateNicknameDto.Response.class))),
            @ApiResponse(responseCode = "400", description = "닉네임 변경 실패"),
            @ApiResponse(responseCode = "401", description = "토큰 만료로 인한 인증 실패")
    })
    ResponseEntity<?> update(String email, UpdateNicknameDto.Request dto);
}
