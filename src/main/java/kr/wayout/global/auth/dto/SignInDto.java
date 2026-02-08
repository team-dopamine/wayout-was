package kr.wayout.global.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public class SignInDto {

    @Getter
    @Builder
    @Schema(name = "SignInDto.CheckResponse", description = "로그인 확인 API 응답 DTO")
    public static class CheckResponse {

        @Schema(description = "사용자 닉네임", example = "jsplix")
        private String nickname;

        public static CheckResponse from(String nickname) {
            return CheckResponse.builder()
                    .nickname(nickname)
                    .build();
        }
    }
}
