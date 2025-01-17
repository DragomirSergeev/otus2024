package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Question;
import ru.otus.hw.domain.Student;
import ru.otus.hw.domain.TestResult;
import ru.otus.hw.exceptions.QuestionReadException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private static final String QUESTION_SEPARATOR = "__________________________";

    private final LocalizedIOService ioService;

    private final QuestionDao questionDao;

    private final LocalizedMessagesService messagesService;

    @Override
    public TestResult executeTestFor(Student student) {
        ioService.printLine("");
        ioService.printFormattedLine(messagesService.getMessage("TestService.answer.the.questions"));

        List<Question> questions = questionDao.findAll();
        var testResult = new TestResult(student);

        for (var question: questions) {
            ioService.printLine(messagesService.getMessage("title.question") + question.text());
            List<Answer> answers = question.answers();
            for (int i=0; i < answers.size(); i++ ) {
                ioService.printLine(messagesService.getMessage("title.answer") + (i + 1) + ": " + answers.get(i).text());
            }
            ioService.printLine(QUESTION_SEPARATOR);
            int value = ioService.readIntForRangeLocalized(1,4, "TestService.error.not.valid");

            var isAnswerValid = question.answers().get(value-1).isCorrect();

            testResult.applyAnswer(question, isAnswerValid);
        }
        return testResult;
    }
}
