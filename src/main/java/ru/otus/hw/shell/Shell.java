package ru.otus.hw.shell;

import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import ru.otus.hw.service.TestRunnerService;

@RequiredArgsConstructor
@ShellComponent(value = "application commands")
public class Shell {

    private final TestRunnerService testService;

    @ShellMethod(key = {"start-test", "st"}, value = "Start student test")
    public void startTest() {
        testService.run();
    }
}
