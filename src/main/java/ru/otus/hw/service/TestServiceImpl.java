package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Student;
import ru.otus.hw.domain.TestResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private static final String QUESTION_SEPARATOR = "__________________________";

    private final IOService ioService;

    private final QuestionDao questionDao;

    @Override
    public TestResult executeTestFor(Student student) {
        ioService.printLine("");
        ioService.printFormattedLine("Please answer the questions below%n");
        var questions = questionDao.findAll();
        var testResult = new TestResult(student);

        for (var question: questions) {
            ioService.printLine("Question: " + question.text());
            List<Answer> answers = question.answers();
            for (int i=0; i < answers.size(); i++ ) {
                ioService.printLine("Answer " + (i + 1) + ": " + answers.get(i).text());
            }
            ioService.printLine(QUESTION_SEPARATOR);
            int value = readValidValue();

            var isAnswerValid = question.answers().get(value-1).isCorrect();

            testResult.applyAnswer(question, isAnswerValid);

        }
        return testResult;
    }

    private int readValidValue () {
        boolean valueValid = false;
        int result = 0;
        do {
            try {
                result = Integer.parseInt(ioService.readString());
                valueValid = true;
            } catch (NumberFormatException ex) {
                ioService.printLine("Enter valid value!!!");
            }
        } while (!valueValid);
        return result;
    }
}
