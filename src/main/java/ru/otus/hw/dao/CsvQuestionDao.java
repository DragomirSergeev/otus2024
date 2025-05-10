package ru.otus.hw.dao;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.otus.hw.config.TestFileNameProvider;
import ru.otus.hw.dao.dto.QuestionDto;
import ru.otus.hw.domain.Question;
import ru.otus.hw.exceptions.QuestionReadException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class CsvQuestionDao implements QuestionDao {
    private final TestFileNameProvider fileNameProvider;

    @Override
    public List<Question> findAll() {
        InputStream inputStream = getClass().getResourceAsStream("/" + fileNameProvider.getTestFileName());

        if (inputStream == null) {
            throw new QuestionReadException("Файл с вопросами не найден: " + fileNameProvider.getTestFileName());
        }

        try (Reader reader = new InputStreamReader(inputStream)) {
            CsvToBean<QuestionDto> csvToBean = new CsvToBeanBuilder<QuestionDto>(reader)
                    .withType(QuestionDto.class)
                    .withSkipLines(1)
                    .withSeparator(';')
                    .build();;
            List<QuestionDto> dtos = csvToBean.parse();
            return dtos.stream()
                    .map(dto -> new Question(dto.getText(), dto.getAnswers()))
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new QuestionReadException(ex.getMessage(), ex);
        }
    }
}
