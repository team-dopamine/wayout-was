package kr.wayout.global.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import kr.wayout.global.auth.OAuth2UserInfo;
import kr.wayout.global.auth.dto.SignOutDto;
import org.springframework.http.ResponseEntity;

@Tag(name = "[인증 관련 API]", description = "사용자 인증 관련 API")
public interface AuthApi {

    @Operation(summary = "로그아웃 API", description = "로그인 된 사용자의 로그아웃 처리를 위한 API")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 처리 완료",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SignOutDto.Response.class))
            ),
            @ApiResponse(responseCode = "401", description = "토큰이 만료되어 이미 로그아웃 된 상태")
    })
    ResponseEntity<?> signOut(String email, HttpServletResponse response);

    @Operation(
            summary = "Access Token 재발급",
            description = "Refresh Token을 이용하여 새로운 Access Token을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "Refresh Token이 유효하지 않음")
    })
    void reissue(String refreshToken, HttpServletResponse response);
}
