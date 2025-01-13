package ru.otus.hw.service;

import lombok.RequiredArgsConstructor;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Question;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private static final String QUESTION_SEPARATOR = "__________________________";

    private final IOService ioService;

    private final QuestionDao questionDao;

    @Override
    public void executeTest() {
        ioService.printLine("");
        ioService.printFormattedLine("Please answer the questions below%n");
        // Получить вопросы из дао и вывести их с вариантами ответов
        List<Question> questions = questionDao.findAll();

        for (Question question : questions) {
            ioService.printLine("Question: " + question.text());
            AtomicInteger questionCounter = new AtomicInteger(1);
            question.answers().forEach(a -> ioService.printLine("Answer " + questionCounter.getAndIncrement() + ": " + a.text()));
            ioService.printLine(QUESTION_SEPARATOR);
        }
    }
}
