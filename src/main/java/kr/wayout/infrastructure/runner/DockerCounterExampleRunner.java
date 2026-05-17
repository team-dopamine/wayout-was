package kr.wayout.infrastructure.runner;

import jakarta.persistence.EntityNotFoundException;
import kr.wayout.domain.generator.Generator;
import kr.wayout.domain.generator.GeneratorRepository;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.problem.ProblemRepository;
import kr.wayout.domain.solution.Solution;
import kr.wayout.domain.solution.SolutionRepository;
import kr.wayout.domain.submission.Language;
import kr.wayout.domain.submission.dto.SubmissionDto;
import kr.wayout.domain.submission.runner.CounterExampleRunResult;
import kr.wayout.domain.submission.runner.CounterExampleRunner;
import kr.wayout.domain.testcase.Testcase;
import kr.wayout.domain.testcase.TestcaseRepository;
import kr.wayout.domain.validator.Validator;
import kr.wayout.domain.validator.ValidatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
@RequiredArgsConstructor
public class DockerCounterExampleRunner implements CounterExampleRunner {

    private static final String CPP_DOCKER_IMAGE = "gcc:14";
    private static final String JAVA_DOCKER_IMAGE = "eclipse-temurin:17";
    private static final String PYTHON_DOCKER_IMAGE = "python:3.11";
    private static final int TOTAL_CASES = 100;
    private static final int GROUP1_CASES = 20;
    private static final int GROUP2_CASES = 50;
    private static final int GROUP3_CASES = 30;
    private static final int TARGET_PER_GROUP_DIVISOR = 5; // 20%
    private static final String TESTLIB_HEADER = "testlib.h";
    private static final String TESTLIB_RESOURCE_PATH = "include/" + TESTLIB_HEADER;
    private static final int DOCKER_TIMEOUT_SECONDS = 60;
    private static final int PROCESS_OUTPUT_READ_TIMEOUT_SECONDS = 5;
    private static final String WORK_ROOT_ENV = "RUNNER_WORK_ROOT";
    private static final String WORK_ROOT_PROP = "wayout.runner.workRoot";

    private final ProblemRepository problemRepository;
    private final GeneratorRepository generatorRepository;
    private final TestcaseRepository testcaseRepository;
    private final ValidatorRepository validatorRepository;
    private final SolutionRepository solutionRepository;

    @Override
    public CounterExampleRunResult run(Long problemId, String sourceCode, Language language) {
        long startedAt = System.nanoTime();
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 문제입니다."));
        Generator generator = generatorRepository.findGeneratorByProblem(problem);
        if (generator == null) {
            throw new EntityNotFoundException("Generator가 존재하지 않습니다.");
        }
        Validator validator = validatorRepository.findValidatorByProblem(problem);
        if (validator == null) {
            throw new EntityNotFoundException("Validator가 존재하지 않습니다.");
        }
        Solution solution = solutionRepository.findTopByProblemOrderByVersionDesc(problem)
                .orElseThrow(() -> new EntityNotFoundException("정답 코드가 존재하지 않습니다."));
        List<Testcase> registeredTestcases = hasRegisteredTestcases(problem)
                ? loadRegisteredTestcases(problem)
                : List.of();

        long baseSeed = System.nanoTime();
        List<String> generatorInputs = buildGeneratorInputs(baseSeed);
        List<String> generatedOutputs = runGeneratorBatch(generator, generatorInputs);
        List<Boolean> validationResults = validateGeneratedOutputs(validator, generatedOutputs);
        List<String> filteredOutputs = new ArrayList<>();
        List<Boolean> filteredResults = new ArrayList<>();
        int invalidCount = 0;
        for (int i = 0; i < generatedOutputs.size(); i++) {
            if (Boolean.TRUE.equals(validationResults.get(i))) {
                filteredOutputs.add(generatedOutputs.get(i));
                filteredResults.add(true);
            } else {
                invalidCount++;
            }
        }

        if (filteredOutputs.isEmpty() && registeredTestcases.isEmpty()) {
            return CounterExampleRunResult.builder()
                    .found(false)
                    .executionTime(elapsedSeconds(startedAt))
                    .message("NO_VALID_CASES")
                    .totalTestcaseCount(0)
                    .generatedInputs(filteredOutputs)
                    .validationResults(filteredResults)
                    .counterExamples(List.of())
                    .build();
        }

        List<String> registeredProgramInputs = wrapCasesForProgram(extractRegisteredInputs(registeredTestcases));
        List<String> generatedProgramInputs = wrapCasesForProgram(filteredOutputs);
        List<String> allProgramInputs = new ArrayList<>(registeredProgramInputs.size() + generatedProgramInputs.size());
        allProgramInputs.addAll(registeredProgramInputs);
        allProgramInputs.addAll(generatedProgramInputs);

        List<String> allSubmissionOutputs = executeInDockerForStdouts(
                "submission",
                sourceCode,
                language,
                allProgramInputs
        );
        List<String> solutionOutputs = generatedProgramInputs.isEmpty()
                ? List.of()
                : executeInDockerForStdouts(
                        "solution",
                        solution.getSourceCode(),
                        solution.getLanguage(),
                        generatedProgramInputs
                );

        List<SubmissionDto.CounterExampleCase> counterExamples = new ArrayList<>();
        int registeredCaseCount = registeredTestcases.size();
        for (int i = 0; i < registeredCaseCount; i++) {
            Testcase testcase = registeredTestcases.get(i);
            String submissionOutput = allSubmissionOutputs.get(i);
            if (!outputsMatch(submissionOutput, testcase.getOutput())) {
                counterExamples.add(SubmissionDto.CounterExampleCase.builder()
                        .input(testcase.getInput())
                        .expectedOutput(testcase.getOutput())
                        .actualOutput(submissionOutput)
                        .build());
            }
        }

        for (int i = 0; i < filteredOutputs.size(); i++) {
            String submissionOutput = allSubmissionOutputs.get(registeredCaseCount + i);
            String solutionOutput = solutionOutputs.get(i);
            if (!outputsMatch(submissionOutput, solutionOutput)) {
                counterExamples.add(SubmissionDto.CounterExampleCase.builder()
                        .input(filteredOutputs.get(i))
                        .expectedOutput(solutionOutput)
                        .actualOutput(submissionOutput)
                        .build());
            }
        }

        boolean found = !counterExamples.isEmpty();
        return CounterExampleRunResult.builder()
                .found(found)
                .executionTime(elapsedSeconds(startedAt))
                .message(found ? "COUNTER_EXAMPLE_FOUND" : "NO_COUNTER_EXAMPLE")
                .totalTestcaseCount(allProgramInputs.size())
                .generatedInputs(filteredOutputs)
                .validationResults(filteredResults)
                .counterExamples(counterExamples)
                .build();
    }

    public boolean validateSingleCase(Problem problem, String inputBody) {
        Validator validator = validatorRepository.findValidatorByProblem(problem);
        if (validator == null) {
            throw new EntityNotFoundException("Validator가 존재하지 않습니다.");
        }

        String wrapped = wrapSingleCaseForProgram(inputBody);
        List<Boolean> results = executeInDockerForValidationResults(
                "validator",
                validator.getSourceCode(),
                Language.CPP,
                List.of(wrapped)
        );
        if (results.size() != 1) {
            throw new IllegalStateException("Validator 결과 개수가 일치하지 않습니다. expected=1 actual=" + results.size());
        }
        return results.get(0);
    }

    private List<String> runGeneratorBatch(Generator generator, List<String> stdins) {
        return executeGeneratorInDockerForStdouts(
                "generator",
                generator.getSourceCode(),
                Language.CPP,
                stdins
        );
    }

    private List<Boolean> validateGeneratedOutputs(Validator validator, List<String> generatedOutputs) {
        List<String> validatorInputs = new ArrayList<>(generatedOutputs.size());
        for (String output : generatedOutputs) {
            validatorInputs.add(wrapSingleCaseForProgram(output));
        }
        return executeInDockerForValidationResults(
                "validator",
                validator.getSourceCode(),
                Language.CPP,
                validatorInputs
        );
    }

    boolean hasRegisteredTestcases(Problem problem) {
        return testcaseRepository != null && testcaseRepository.existsByProblem(problem);
    }

    List<Testcase> loadRegisteredTestcases(Problem problem) {
        if (testcaseRepository == null) {
            return List.of();
        }
        return testcaseRepository.findAllByProblemOrderByIdAsc(problem);
    }

    private List<String> buildGeneratorInputs(long baseSeed) {
        Random random = new Random(baseSeed);

        int group1Target = GROUP1_CASES / TARGET_PER_GROUP_DIVISOR;
        int group2Target = GROUP2_CASES / TARGET_PER_GROUP_DIVISOR;
        int group3Target = GROUP3_CASES / TARGET_PER_GROUP_DIVISOR;

        List<Integer> targetGroups = new ArrayList<>();
        addGroup(targetGroups, 1, group1Target);
        addGroup(targetGroups, 2, group2Target);
        addGroup(targetGroups, 3, group3Target);
        Collections.shuffle(targetGroups, random);

        List<Integer> targetPatterns = new ArrayList<>();
        addPattern(targetPatterns, 3, 5);
        addPattern(targetPatterns, 5, 5);
        addPattern(targetPatterns, 2, 5);
        addPattern(targetPatterns, 1, 5);
        Collections.shuffle(targetPatterns, random);

        List<String> inputs = new ArrayList<>(TOTAL_CASES);
        for (int i = 0; i < targetGroups.size(); i++) {
            long seed = baseSeed + i;
            inputs.add(formatGeneratorStdin(targetGroups.get(i), targetPatterns.get(i), seed));
        }

        int group1Random = GROUP1_CASES - group1Target;
        int group2Random = GROUP2_CASES - group2Target;
        int group3Random = GROUP3_CASES - group3Target;

        List<Integer> randomGroups = new ArrayList<>();
        addGroup(randomGroups, 1, group1Random);
        addGroup(randomGroups, 2, group2Random);
        addGroup(randomGroups, 3, group3Random);
        Collections.shuffle(randomGroups, random);

        for (int i = 0; i < randomGroups.size(); i++) {
            long seed = baseSeed + targetGroups.size() + i;
            inputs.add(formatGeneratorStdin(randomGroups.get(i), 0, seed));
        }
        return inputs;
    }

    private void addGroup(List<Integer> groups, int group, int count) {
        for (int i = 0; i < count; i++) {
            groups.add(group);
        }
    }

    private void addPattern(List<Integer> patterns, int pattern, int count) {
        for (int i = 0; i < count; i++) {
            patterns.add(pattern);
        }
    }

    private String formatGeneratorStdin(int group, int pattern, long seed) {
        return group + " " + pattern + " " + seed;
    }

    private List<String> extractRegisteredInputs(List<Testcase> registeredTestcases) {
        List<String> inputs = new ArrayList<>(registeredTestcases.size());
        for (Testcase registeredTestcase : registeredTestcases) {
            inputs.add(registeredTestcase.getInput());
        }
        return inputs;
    }

    private List<String> wrapCasesForProgram(List<String> inputs) {
        List<String> wrappedInputs = new ArrayList<>(inputs.size());
        for (String input : inputs) {
            wrappedInputs.add(wrapSingleCaseForProgram(input));
        }
        return wrappedInputs;
    }

    private List<String> executeGeneratorInDockerForStdouts(String role, String sourceCode, Language language, List<String> stdins) {
        if (language != Language.CPP) {
            throw new IllegalArgumentException("현재 Docker 실행기는 C++만 지원합니다.");
        }
        if (stdins == null || stdins.isEmpty()) {
            return List.of();
        }

        List<String> seedArgs = new ArrayList<>(stdins.size());
        for (int i = 0; i < stdins.size(); i++) {
            seedArgs.add(resolveSeedArg(stdins.get(i), i));
        }
        return executeInDocker(new DockerExecutionSpec<>(
                role,
                sourceCode,
                language,
                stdins,
                buildGeneratorScript(seedArgs),
                CPP_DOCKER_IMAGE,
                true,
                (stdout, outputsDir, expectedCount) -> readCaseOutputs(outputsDir, expectedCount)
        ));
    }

    private List<String> executeInDockerForStdouts(String role,
                                                   String sourceCode,
                                                   Language language,
                                                   List<String> stdins) {
        if (stdins == null || stdins.isEmpty()) {
            return List.of();
        }
        if (language == null) {
            throw new IllegalArgumentException("언어가 지정되지 않았습니다.");
        }

        return executeInDocker(new DockerExecutionSpec<>(
                role,
                sourceCode,
                language,
                stdins,
                buildProgramScript(language, stdins.size()),
                resolveDockerImage(language),
                language == Language.CPP,
                (stdout, outputsDir, expectedCount) -> readCaseOutputs(outputsDir, expectedCount)
        ));
    }

    private List<Boolean> executeInDockerForValidationResults(String role,
                                                              String sourceCode,
                                                              Language language,
                                                              List<String> stdins) {
        if (language != Language.CPP) {
            throw new IllegalArgumentException("현재 Docker 실행기는 C++만 지원합니다.");
        }
        if (stdins == null || stdins.isEmpty()) {
            return List.of();
        }

        return executeInDocker(new DockerExecutionSpec<>(
                role,
                sourceCode,
                language,
                stdins,
                buildValidatorScript(stdins.size()),
                CPP_DOCKER_IMAGE,
                true,
                this::parseValidationResults
        ));
    }

    private <T> T executeInDocker(DockerExecutionSpec<T> spec) {
        Path workDir = null;
        try {
            workDir = createWorkDir(spec.role());
            Path inputsDir = workDir.resolve("inputs");
            Path outputsDir = workDir.resolve("outputs");
            Files.createDirectories(inputsDir);
            Files.createDirectories(outputsDir);

            Path sourceFile = resolveSourceFile(workDir, spec.language());
            Files.writeString(sourceFile, spec.sourceCode(), StandardCharsets.UTF_8);
            writeStdins(inputsDir, spec.stdins());
            if (spec.copyTestlibHeader()) {
                copyTestlibHeaderIfNeeded(spec.sourceCode(), workDir);
            }

            List<String> command = List.of(
                    "docker", "run", "--rm",
                    "-v", workDir.toAbsolutePath() + ":/work",
                    "-w", "/work",
                    spec.dockerImage(),
                    "bash", "-lc", spec.script()
            );

            Process process = new ProcessBuilder(command).start();
            InputStream stdoutStream = process.getInputStream();
            InputStream stderrStream = process.getErrorStream();
            CompletableFuture<String> stdoutFuture = readProcessOutputAsync(stdoutStream);
            CompletableFuture<String> stderrFuture = readProcessOutputAsync(stderrStream);
            boolean isFinished = process.waitFor(DOCKER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!isFinished) {
                process.destroyForcibly();
                closeQuietly(stdoutStream);
                closeQuietly(stderrStream);
                stdoutFuture.cancel(true);
                stderrFuture.cancel(true);
                throw new IllegalStateException("Docker 실행시간이 초과되었습니다.");
            }

            String stdout = readProcessOutput(stdoutFuture);
            String stderr = readProcessOutput(stderrFuture);
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Docker run failed. role={}, exitCode={}, stderr={}", spec.role(), exitCode, stderr);
                throw new IllegalStateException("Docker 실행에 실패했습니다. stderr=" + stderr);
            }

            return spec.resultReader().read(stdout, outputsDir, spec.stdins().size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Docker 실행에 실패했습니다.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Docker 실행에 실패했습니다.", e);
        } finally {
            if (workDir != null) {
                cleanup(workDir);
            }
        }
    }

    private void writeStdins(Path inputsDir, List<String> stdins) throws IOException {
        for (int i = 0; i < stdins.size(); i++) {
            String stdin = stdins.get(i);
            Files.writeString(inputsDir.resolve("input_" + i + ".txt"), stdin == null ? "" : stdin, StandardCharsets.UTF_8);
        }
    }

    private CompletableFuture<String> readProcessOutputAsync(InputStream stream) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Thread.startVirtualThread(() -> {
            try {
                future.complete(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                future.completeExceptionally(new CompletionException(e));
            }
        });
        return future;
    }

    private void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException ignored) {
        }
    }

    private String readProcessOutput(CompletableFuture<String> outputFuture) throws IOException, InterruptedException {
        try {
            return outputFuture.get(PROCESS_OUTPUT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CompletionException completionException
                    && completionException.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IllegalStateException("Docker 실행에 실패했습니다.", cause);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Docker 실행에 실패했습니다.", e);
        }
    }

    List<String> readCaseOutputs(Path outputsDir, int expectedCount) throws IOException {
        List<String> outputs = new ArrayList<>(expectedCount);
        for (int i = 0; i < expectedCount; i++) {
            Path outputFile = outputsDir.resolve("case_" + i + ".txt");
            if (!Files.exists(outputFile)) {
                throw new IllegalStateException("Docker 실행 결과 파일이 없습니다. file=" + outputFile);
            }
            outputs.add(Files.readString(outputFile, StandardCharsets.UTF_8));
        }
        return outputs;
    }

    List<Boolean> parseValidationResults(String stdout, Path outputsDir, int expectedCount) {
        String trimmed = stdout.trim();
        String[] tokens = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
        if (tokens.length != expectedCount) {
            throw new IllegalStateException("Validator 결과 개수가 일치하지 않습니다. expected="
                    + expectedCount
                    + " actual="
                    + tokens.length);
        }
        List<Boolean> results = new ArrayList<>(expectedCount);
        int errorLogCount = 0;
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            int code;
            try {
                code = Integer.parseInt(token);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Validator exit code 파싱에 실패했습니다. value=" + token, e);
            }
            boolean ok = code == 0;
            results.add(ok);
            if (!ok && errorLogCount < 5) {
                log.warn("Validator failed: index={}, exitCode={}", i, code);
                errorLogCount++;
            }
        }
        return results;
    }

    private record DockerExecutionSpec<T>(
            String role,
            String sourceCode,
            Language language,
            List<String> stdins,
            String script,
            String dockerImage,
            boolean copyTestlibHeader,
            DockerResultReader<T> resultReader
    ) {
    }

    @FunctionalInterface
    private interface DockerResultReader<T> {
        T read(String stdout, Path outputsDir, int expectedCount) throws IOException;
    }

    private void cleanup(Path workDir) {
        try (var paths = Files.walk(workDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private Path createWorkDir(String role) throws IOException {
        String workRoot = resolveWorkRoot();
        String prefix = "wayout-runner-" + role + "-";
        if (workRoot == null || workRoot.isBlank()) {
            return Files.createTempDirectory(prefix);
        }
        Path base = Path.of(workRoot).toAbsolutePath().normalize();
        Files.createDirectories(base);
        return Files.createTempDirectory(base, prefix);
    }

    private String resolveWorkRoot() {
        String fromEnv = System.getenv(WORK_ROOT_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromProp = System.getProperty(WORK_ROOT_PROP);
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp;
        }
        return null;
    }

    private void copyTestlibHeaderIfNeeded(String sourceCode, Path workDir) throws IOException {
        if (sourceCode == null || !sourceCode.contains(TESTLIB_HEADER)) {
            return;
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(TESTLIB_RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("testlib.h가 필요합니다. classpath에 include/testlib.h를 넣어주세요.");
            }
            Path destHeader = workDir.resolve(TESTLIB_RESOURCE_PATH);
            Files.createDirectories(destHeader.getParent());
            Files.copy(in, destHeader, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private double elapsedSeconds(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000_000.0;
    }

    private String buildGeneratorScript(List<String> seedArgs) {
        StringBuilder builder = new StringBuilder();
        builder.append("set -euo pipefail\n");
        builder.append("g++ -std=c++17 -O2 -I./include -o main main.cpp\n");
        builder.append("mkdir -p outputs\n");
        for (int i = 0; i < seedArgs.size(); i++) {
            builder.append("./main ")
                    .append(escapeShellArg(seedArgs.get(i)))
                    .append(" < \"inputs/input_")
                    .append(i)
                    .append(".txt\" > \"outputs/case_")
                    .append(i)
                    .append(".txt\"\n");
        }
        return builder.toString();
    }

    private String buildValidatorScript(int caseCount) {
        StringBuilder builder = new StringBuilder();
        builder.append("set -euo pipefail\n");
        builder.append("g++ -std=c++17 -O2 -I./include -o main main.cpp\n");
        builder.append("mkdir -p outputs\n");
        builder.append("set +e\n");
        for (int i = 0; i < caseCount; i++) {
            builder.append("./main < \"inputs/input_")
                    .append(i)
                    .append(".txt\" > /dev/null 2> \"outputs/err_")
                    .append(i)
                    .append(".txt\"\n");
            builder.append("echo $?\n");
        }
        return builder.toString();
    }

    private Path resolveSourceFile(Path workDir, Language language) {
        if (language == Language.CPP) {
            return workDir.resolve("main.cpp");
        }
        if (language == Language.JAVA) {
            return workDir.resolve("Solution.java");
        }
        if (language == Language.PYTHON) {
            return workDir.resolve("main.py");
        }
        throw new IllegalArgumentException("지원하지 않는 언어입니다: " + language);
    }

    private String resolveDockerImage(Language language) {
        if (language == Language.CPP) {
            return CPP_DOCKER_IMAGE;
        }
        if (language == Language.JAVA) {
            return JAVA_DOCKER_IMAGE;
        }
        if (language == Language.PYTHON) {
            return PYTHON_DOCKER_IMAGE;
        }
        throw new IllegalArgumentException("지원하지 않는 언어입니다: " + language);
    }

    private String buildProgramScript(Language language, int caseCount) {
        StringBuilder builder = new StringBuilder();
        builder.append("set -euo pipefail\n");
        if (language == Language.CPP) {
            builder.append("g++ -std=c++17 -O2 -I./include -o main main.cpp\n");
        } else if (language == Language.JAVA) {
            builder.append("javac Solution.java\n");
        }
        builder.append("mkdir -p outputs\n");
        builder.append("set +e\n");
        for (int i = 0; i < caseCount; i++) {
            if (language == Language.CPP) {
                builder.append("./main");
            } else if (language == Language.JAVA) {
                builder.append("java Solution");
            } else if (language == Language.PYTHON) {
                builder.append("python3 main.py");
            } else {
                throw new IllegalArgumentException("지원하지 않는 언어입니다: " + language);
            }
            builder.append(" < \"inputs/input_")
                    .append(i)
                    .append(".txt\" > \"outputs/case_")
                    .append(i)
                    .append(".txt\"\n");
        }
        builder.append("exit 0\n");
        return builder.toString();
    }

    private boolean outputsMatch(String submissionOutput, String solutionOutput) {
        return tokenizeOutput(submissionOutput).equals(tokenizeOutput(solutionOutput));
    }

    private List<String> tokenizeOutput(String output) {
        if (output == null) {
            return List.of();
        }
        String trimmed = output.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(trimmed.split("\\s+"));
    }

    private String wrapSingleCaseForProgram(String output) {
        String body = output == null ? "" : output;
        body = body.stripTrailing();
        if (body.isEmpty()) {
            return "1\n";
        }
        return "1\n" + body + "\n";
    }

    private String resolveSeedArg(String stdin, int fallbackIndex) {
        if (stdin == null) {
            return String.valueOf(fallbackIndex);
        }
        String trimmed = stdin.trim();
        if (trimmed.isEmpty()) {
            return String.valueOf(fallbackIndex);
        }
        String[] tokens = trimmed.split("\\s+");
        String token = tokens[tokens.length - 1];
        try {
            return Long.toString(Long.parseLong(token));
        } catch (NumberFormatException e) {
            return String.valueOf(fallbackIndex);
        }
    }

    private String escapeShellArg(String arg) {
        if (arg != null && arg.matches("^-?\\d+$")) {
            return arg;
        }
        throw new IllegalArgumentException("Invalid seed argument: " + arg);
    }

    private String truncateForLog(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= 500) {
            return trimmed;
        }
        return trimmed.substring(0, 500) + "...(truncated)";
    }

}
