package kr.wayout.global.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class SignOutDto {

    @Getter
    @AllArgsConstructor
    @Schema(name = "SignOutDto.Response", description = "로그아웃 응답 DTO")
    public static class Response {
        @Setter
        @Schema(description = "응답 메세지", example = "로그아웃에 성공하였습니다.")
        private String message;

        public static Response of(String message) {
            return new Response(message);
        }
    }

}
