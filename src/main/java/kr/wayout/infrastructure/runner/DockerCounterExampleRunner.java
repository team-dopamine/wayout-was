package kr.wayout.infrastructure.runner;

import jakarta.persistence.EntityNotFoundException;
import kr.wayout.domain.generator.Generator;
import kr.wayout.domain.generator.GeneratorRepository;
import kr.wayout.domain.problem.Problem;
import kr.wayout.domain.problem.ProblemRepository;
import kr.wayout.domain.submission.Language;
import kr.wayout.domain.submission.runner.CounterExampleRunResult;
import kr.wayout.domain.submission.runner.CounterExampleRunner;
import kr.wayout.domain.validator.Validator;
import kr.wayout.domain.validator.ValidatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class DockerCounterExampleRunner implements CounterExampleRunner {

    private static final String CPP_DOCKER_IMAGE = "gcc:14";
    private static final int TOTAL_CASES = 100;
    private static final int GROUP1_CASES = 20;
    private static final int GROUP2_CASES = 50;
    private static final int GROUP3_CASES = 30;
    private static final int TARGET_PER_GROUP_DIVISOR = 5; // 20%
    private static final String TESTLIB_HEADER = "testlib.h";
    private static final int DOCKER_TIMEOUT_SECONDS = 60;

    private final ProblemRepository problemRepository;
    private final GeneratorRepository generatorRepository;
    private final ValidatorRepository validatorRepository;

    @Override
    public CounterExampleRunResult run(Long problemId) {
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

        long baseSeed = System.nanoTime();
        List<String> generatorInputs = buildGeneratorInputs(baseSeed);
        List<String> generatedOutputs = runGeneratorBatch(generator, generatorInputs);
        // for (int i = 0; i < generatedOutputs.size(); i++) {
        //     log.info("Generated case: index={}, input=\n{}", i, generatedOutputs.get(i));
        // }
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
        // log.info("Validator filter summary: total={}, valid={}, invalid={}, filtered={}",
        //         generatedOutputs.size(),
        //         filteredOutputs.size(),
        //         invalidCount,
        //         filteredOutputs.size());

        return CounterExampleRunResult.builder()
                .found(false)
                .executionTime(elapsedSeconds(startedAt))
                .message("VALIDATOR_EXECUTED")
                .generatedInputs(filteredOutputs)
                .validationResults(filteredResults)
                .build();
    }

    private List<String> runGeneratorBatch(Generator generator, List<String> stdins) {
        return executeInDockerForStdouts(
                "generator",
                generator.getSourceCode(),
                Language.CPP,
                stdins
        );
    }

    private List<Boolean> validateGeneratedOutputs(Validator validator, List<String> generatedOutputs) {
        List<String> validatorInputs = new ArrayList<>(generatedOutputs.size());
        for (String output : generatedOutputs) {
            validatorInputs.add(wrapSingleCaseForValidator(output));
        }
        return executeInDockerForValidationResults(
                "validator",
                validator.getSourceCode(),
                Language.CPP,
                validatorInputs
        );
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

    private List<String> executeInDockerForStdouts(String role, String sourceCode, Language language, List<String> stdins) {
        if (language != Language.CPP) {
            throw new IllegalArgumentException("현재 Docker 실행기는 C++만 지원합니다.");
        }
        if (stdins == null || stdins.isEmpty()) {
            return List.of();
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("wayout-runner-" + role + "-");
            Path sourceFile = workDir.resolve("main.cpp");
            Path inputsDir = workDir.resolve("inputs");
            Path outputsDir = workDir.resolve("outputs");
            Files.createDirectories(inputsDir);
            Files.createDirectories(outputsDir);

            Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);
            List<String> seedArgs = new ArrayList<>(stdins.size());
            for (int i = 0; i < stdins.size(); i++) {
                String stdin = stdins.get(i);
                Files.writeString(inputsDir.resolve("input_" + i + ".txt"), stdin == null ? "" : stdin, StandardCharsets.UTF_8);
                seedArgs.add(resolveSeedArg(stdin, i));
            }
            copyTestlibHeaderIfNeeded(sourceCode, workDir);

            String script = buildGeneratorScript(seedArgs);
            List<String> command = List.of(
                    "docker", "run", "--rm",
                    "-v", workDir.toAbsolutePath() + ":/work",
                    "-w", "/work",
                    CPP_DOCKER_IMAGE,
                    "bash", "-lc", script
            );

            Process process = new ProcessBuilder(command).start();
            process.getInputStream().readAllBytes(); // drain stdout to avoid blocking
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean isFinished = process.waitFor(DOCKER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!isFinished) {
                process.destroyForcibly();
                throw new IllegalStateException("Docker 실행시간이 초과되었습니다.");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Docker run failed. role={}, exitCode={}, stderr={}", role, exitCode, stderr);
                throw new IllegalStateException("Docker 실행에 실패했습니다. stderr=" + stderr);
            }

            List<String> outputs = new ArrayList<>(stdins.size());
            for (int i = 0; i < stdins.size(); i++) {
                Path outputFile = outputsDir.resolve("case_" + i + ".txt");
                if (!Files.exists(outputFile)) {
                    throw new IllegalStateException("Docker 실행 결과 파일이 없습니다. file=" + outputFile);
                }
                outputs.add(Files.readString(outputFile, StandardCharsets.UTF_8));
            }
            return outputs;
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

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("wayout-runner-" + role + "-");
            Path sourceFile = workDir.resolve("main.cpp");
            Path inputsDir = workDir.resolve("inputs");
            Path outputsDir = workDir.resolve("outputs");
            Files.createDirectories(inputsDir);
            Files.createDirectories(outputsDir);

            Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);
            for (int i = 0; i < stdins.size(); i++) {
                String stdin = stdins.get(i);
                Files.writeString(inputsDir.resolve("input_" + i + ".txt"), stdin == null ? "" : stdin, StandardCharsets.UTF_8);
            }
            copyTestlibHeaderIfNeeded(sourceCode, workDir);

            String script = buildValidatorScript(stdins.size());
            List<String> command = List.of(
                    "docker", "run", "--rm",
                    "-v", workDir.toAbsolutePath() + ":/work",
                    "-w", "/work",
                    CPP_DOCKER_IMAGE,
                    "bash", "-lc", script
            );

            Process process = new ProcessBuilder(command).start();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean isFinished = process.waitFor(DOCKER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!isFinished) {
                process.destroyForcibly();
                throw new IllegalStateException("Docker 실행시간이 초과되었습니다.");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Docker run failed. role={}, exitCode={}, stderr={}", role, exitCode, stderr);
                throw new IllegalStateException("Docker 실행에 실패했습니다. stderr=" + stderr);
            }

            String trimmed = stdout.trim();
            String[] tokens = trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
            if (tokens.length != stdins.size()) {
                throw new IllegalStateException("Validator 결과 개수가 일치하지 않습니다. expected="
                        + stdins.size()
                        + " actual="
                        + tokens.length);
            }
            List<Boolean> results = new ArrayList<>(stdins.size());
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
                    Path errFile = outputsDir.resolve("err_" + i + ".txt");
                    String errText = Files.exists(errFile)
                            ? Files.readString(errFile, StandardCharsets.UTF_8)
                            : "";
                    // log.warn("Validator failed: index={}, exitCode={}, stderr={}",
                    //         i,
                    //         code,
                    //         truncateForLog(errText));
                    errorLogCount++;
                }
            }
            return results;
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

    private void copyTestlibHeaderIfNeeded(String sourceCode, Path workDir) throws IOException {
        if (sourceCode == null || !sourceCode.contains(TESTLIB_HEADER)) {
            return;
        }

        Path localHeader = Path.of(TESTLIB_HEADER);
        if (!Files.exists(localHeader)) {
            throw new IllegalStateException("testlib.h가 필요합니다. 서버 실행 디렉터리(user.dir)에 testlib.h 파일을 두세요.");
        }

        Files.copy(localHeader, workDir.resolve(TESTLIB_HEADER));
    }

    private double elapsedSeconds(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000_000.0;
    }

    private String buildGeneratorScript(List<String> seedArgs) {
        StringBuilder builder = new StringBuilder();
        builder.append("set -euo pipefail\n");
        builder.append("g++ -std=c++17 -O2 -o main main.cpp\n");
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
        builder.append("g++ -std=c++17 -O2 -o main main.cpp\n");
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

    private String wrapSingleCaseForValidator(String output) {
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
