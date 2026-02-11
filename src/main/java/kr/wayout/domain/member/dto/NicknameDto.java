package kr.wayout.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class NicknameDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "NicknameDto.UpdateRequest", description = "닉네임 변경 요청 DTO")
    public static class UpdateRequest {

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 12, message = "닉네임은 2~12자여야 합니다")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 가능합니다")
        private String nickname;

    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "NicknameDto.UpdateResponse", description = "닉네임 변경 응답 DTO")
    public static class UpdateResponse {

        private String nickname;
        private String message;

        public static UpdateResponse of(String nickname, String message) {
            return new UpdateResponse(nickname, message);
        }

    }

    @Getter
    @AllArgsConstructor
    @Schema(name = "NicknameDto.ReadResponse", description = "닉네임 조회 응답 DTO")
    public static class ReadResponse {

        private String nickname;

        public static ReadResponse of(String nickname) {
            return new ReadResponse(nickname);
        }

    }
}
