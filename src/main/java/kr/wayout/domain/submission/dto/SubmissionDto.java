package kr.wayout.domain.submission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.wayout.domain.problem.Platform;
import kr.wayout.domain.submission.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class SubmissionDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCounterExampleRequest {

        @NotNull
        private Long problemId;

        @NotNull
        private Language language;

        @NotBlank
        private String sourceCode;

        @NotNull
        private Boolean isOpen;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CreateCounterExampleResponse {
        private String status;
        private String message;
        private Boolean found;
        private Double executionTime;
        private List<CounterExampleCase> counterExamples;
        private String outputFilePath;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CounterExampleCase {
        private String input;
        private String expectedOutput;
        private String actualOutput;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ListResponse {
        private Long id;
        private String nickname;
        private String title;
        private Language language;
        private Platform platform;
        private Double executionTime;
        private int counterExampleCount;
        private LocalDateTime createdAt;
        private boolean isOpen;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Detail {
        private Long id;
        private Long problemNo;
        private String title;
        private String sourceCode;
        private int counterExampleCount;
        private List<CounterExampleCase> counterExamples;
        private Language language;
        private Platform platform;
        private Double executionTime;
        private LocalDateTime createdAt;
    }
}
