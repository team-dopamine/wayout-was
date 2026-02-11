package kr.wayout.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.wayout.domain.member.Member;
import lombok.Builder;
import lombok.Getter;

public class MemberDto {

    @Getter
    @Builder
    @Schema(name = "MemberDto.Info", description = "사용자 정보 조회 응답 DTO")
    public static class Info {

        private String email;
        private String nickname;

        public static Info from(Member member) {
            return Info.builder()
                    .email(member.getEmail())
                    .nickname(member.getNickname())
                    .build();
        }
    }
}
