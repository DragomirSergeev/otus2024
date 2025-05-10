package ru.otus.hw.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Question;
import ru.otus.hw.domain.Student;

import java.util.List;

import static org.mockito.Mockito.*;

public class TestServiceImplTest {

    @Mock
    LocalizedIOService ioService;
    @Mock
    private QuestionDao questionDao;

    @InjectMocks
    private TestServiceImpl testService;

    private List<Question> testQuestions;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        testQuestions = getTestData();
    }

    @DisplayName("Simple test.")
    @Test
    public void testExecuteSimpleTest() {
        when(questionDao.findAll()).thenReturn(testQuestions);
        when(ioService.readString()).thenReturn("1");

        testService.executeTestFor(mock(Student.class));

        verify(ioService).printFormattedLine("Please answer the questions below%n");
        verify(ioService, times(1)).printFormattedLine(anyString());
    }

    private List<Question> getTestData() {
        List<Question> testQuestions = List.of(
                new Question("1 + 1?", List.of(new Answer("2", true), new Answer("1", false))),
                new Question("2 + 2?", List.of(new Answer("4", true), new Answer("1", false)))
        );
        return testQuestions;
    }
}
