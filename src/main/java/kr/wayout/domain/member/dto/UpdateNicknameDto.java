package kr.wayout.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UpdateNicknameDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "UpdateNicknameDto.Request", description = "닉네임 변경 요청 DTO")
    public static class Request {

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 12, message = "닉네임은 2~12자여야 합니다")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 가능합니다")
        private String nickname;

    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "UpdateNicknameDto.Response", description = "닉네임 변경 응답 DTO")
    public static class Response {

        private String nickname;
        private String message;

        public static Response of(String nickname, String message) {
            return new Response(nickname, message);
        }

    }

}
