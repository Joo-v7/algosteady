package com.algosteady.judging.runner.language;

import lombok.Getter;

import java.util.List;

/** 언어별 파일명 및 컴파일·실행 명령 */
@Getter
public enum LanguageSpec {

    C("c", "submission.c", "harness.c",
            List.of("gcc", "-std=c17", "-Wall", "-Wextra", "/input/harness.c", "-o", "/work/program"),
            List.of("/work/program"));

    private final String id;

    private final String submissionFilename;

    private final String harnessFilename;

    private final List<String> preparationCommand;

    private final List<String> executionCommand;

    LanguageSpec(String id, String submissionFilename, String harnessFilename, List<String> preparationCommand, List<String> executionCommand) {
        this.id = id;
        this.submissionFilename = submissionFilename;
        this.harnessFilename = harnessFilename;
        this.preparationCommand = List.copyOf(preparationCommand);
        this.executionCommand = List.copyOf(executionCommand);
    }

}
