package kr.wayout.domain.submission.runner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import kr.wayout.domain.submission.dto.SubmissionDto;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CounterExampleRunResult {

    private boolean found;
    private Double executionTime;
    private String message;
    private List<String> generatedInputs;
    private List<Boolean> validationResults;
    private List<SubmissionDto.CounterExampleCase> counterExamples;
    private String outputFilePath;
}
