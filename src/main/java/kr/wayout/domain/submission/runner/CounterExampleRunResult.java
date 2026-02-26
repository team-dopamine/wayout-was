package kr.wayout.domain.submission.runner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CounterExampleRunResult {

    private boolean found;
    private Double executionTime;
    private String message;
    private List<String> generatedInputs;
    private String outputFilePath;
}
