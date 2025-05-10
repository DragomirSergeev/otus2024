package ru.otus.hw.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.otus.hw.dao.QuestionDao;
import ru.otus.hw.domain.Answer;
import ru.otus.hw.domain.Question;
import ru.otus.hw.domain.Student;
import ru.otus.hw.domain.TestResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class})
@SpringBootTest(properties = {"spring.shell.interactive.enabled=false",})
public class TestServiceImplTest {

    @MockBean
    LocalizedIOService ioService;
    @MockBean
    private QuestionDao questionDao;
    @MockBean
    private LocalizedMessagesService messagesService;

    @Autowired
    private TestServiceImpl testService;

    @TestConfiguration
    public static class testConfig {

        @Bean
        public TestServiceImpl testService() {
            return new TestServiceImpl();
        }
    }

    private final List<Question> testQuestions = List.of(
            new Question("1 + 1?", List.of(new Answer("2", true), new Answer("1", false))),
            new Question("2 + 2?", List.of(new Answer("4", true), new Answer("1", false))),
            new Question("3 + 3?", List.of(new Answer("4", false), new Answer("6", true)))
    );

    @DisplayName("Simple test.")
    @Test
    public void testExecuteSimpleTest() {
        when(questionDao.findAll()).thenReturn(testQuestions);
        when(ioService.readIntForRangeLocalized(anyInt(), anyInt(), anyString())).thenReturn(1);

        TestResult result = testService.executeTestFor(mock(Student.class));
        assertEquals(2, result.getRightAnswersCount());
    }
}
