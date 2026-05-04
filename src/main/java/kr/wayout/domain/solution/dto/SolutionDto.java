package kr.wayout.domain.solution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.wayout.domain.problem.Platform;
import kr.wayout.domain.solution.Solution;
import kr.wayout.domain.submission.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SolutionDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull
        private Long problemId;

        @NotNull
        private Language language;

        @NotNull
        private Boolean isOpen;

        @NotBlank
        private String sourceCode;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CreateResponse {

        private String message;

        public static CreateResponse of(String message) {
            return CreateResponse.builder()
                    .message(message)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ContributionResponse {
        private Long problemId;
        private Integer problemNo;
        private Platform platform;
        private String problemTitle;
        private Language language;
        private LocalDateTime submissionDate;

        public static ContributionResponse from(Solution solution) {
            return ContributionResponse.builder()
                    .problemId(solution.getProblem().getId())
                    .problemNo(solution.getProblem().getProblemNo())
                    .platform(solution.getProblem().getPlatform())
                    .problemTitle(solution.getProblem().getTitle())
                    .language(solution.getLanguage())
                    .submissionDate(solution.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MyContributionDetailResponse {
        private Long id;
        private Long problemId;
        private Integer problemNo;
        private String title;
        private String sourceCode;
        private Language language;
        private LocalDateTime contributionDate;
        private Boolean isOpen;

        public static MyContributionDetailResponse from(Solution solution) {
            return MyContributionDetailResponse.builder()
                    .id(solution.getId())
                    .problemId(solution.getProblem().getId())
                    .problemNo(solution.getProblem().getProblemNo())
                    .title(solution.getProblem().getTitle())
                    .sourceCode(solution.getSourceCode())
                    .language(solution.getLanguage())
                    .contributionDate(solution.getCreatedAt())
                    .isOpen(solution.getIsOpen())
                    .build();
        }
    }
}
