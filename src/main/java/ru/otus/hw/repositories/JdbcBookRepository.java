package ru.otus.hw.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.otus.hw.exceptions.EntityNotFoundException;
import ru.otus.hw.models.Author;
import ru.otus.hw.models.Book;
import ru.otus.hw.models.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JdbcBookRepository implements BookRepository {

    @Autowired
    NamedParameterJdbcTemplate jdbc;

    @Autowired
    private final GenreRepository genreRepository;

    @Override
    public Optional<Book> findById(long id) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        return Optional.ofNullable(jdbc.query("select b.id as id, title, author_id, a.full_name, g.id as genre_id, g.name from books b join authors a on b.author_id = a.id join books_genres bg on bg.book_id = b.id join genres g on g.id = bg.genre_id where b.id = :id", params, new BookResultSetExtractor()));
    }

    @Override
    public List<Book> findAll() {
        var genres = genreRepository.findAll();
        var relations = getAllGenreRelations();
        var books = getAllBooksWithoutGenres();
        mergeBooksInfo(books, genres, relations);
        return books;
    }

    @Override
    public Book save(Book book) {
        if (book.getId() == 0) {
            return insert(book);
        }
        return update(book);
    }

    @Override
    public void deleteById(long id) {
        MapSqlParameterSource deleteParams = new MapSqlParameterSource();
        deleteParams.addValue("id", id);
        jdbc.update("delete from books where id = :id", deleteParams);
    }

    private List<Book> getAllBooksWithoutGenres() {
        return jdbc.query("select b.id, title, author_id, a.full_name from books b join authors a on b.author_id = a.id ", new BookRowMapper());
    }

    private List<BookGenreRelation> getAllGenreRelations() {
        return jdbc.query("select * from books_genres", new BookGeneresRowMapper());
    }

    private void mergeBooksInfo(List<Book> booksWithoutGenres, List<Genre> genres,
                                List<BookGenreRelation> relations) {
        // Добавить книгам (booksWithoutGenres) жанры (genres) в соответствии со связями (relations)
        for (Book book : booksWithoutGenres) {
            Set<Long> bookGeneres = relations.stream().filter(r -> r.bookId == book.getId()).map( i -> i.genreId).collect(Collectors.toSet());
            book.setGenres(
                    genres.stream().filter(g -> bookGeneres.contains(g.getId())).collect(Collectors.toList())
            );
        }
    }

    private Book insert(Book book) {
        var keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource parameterSourceBook = new MapSqlParameterSource();
        parameterSourceBook.addValue("title", book.getTitle());
        parameterSourceBook.addValue("author", book.getAuthor().getId());
        jdbc.update("insert into books (title, author_id ) values (:title, :author)", parameterSourceBook, keyHolder, new String[]{"id"});

        //noinspection DataFlowIssue
        book.setId(keyHolder.getKeyAs(Long.class));
        batchInsertGenresRelationsFor(book);
        return book;
    }

    private Book update(Book book) {
        MapSqlParameterSource parameterSourceBook = new MapSqlParameterSource();
        parameterSourceBook.addValue("id", book.getId());
        parameterSourceBook.addValue("title", book.getTitle());
        parameterSourceBook.addValue("author", book.getAuthor().getId());
        jdbc.update("update books set title = :title, author_id = :author where id = :id", parameterSourceBook);
        // Выбросить EntityNotFoundException если не обновлено ни одной записи в БД
        removeGenresRelationsFor(book);
        batchInsertGenresRelationsFor(book);

        return book;
    }

    private void batchInsertGenresRelationsFor(Book book) {
        // Использовать метод batchUpdate
        List<Genre> genres = book.getGenres();
        SqlParameterSource[] batchArgs = new SqlParameterSource[genres.size()];
        for (int i = 0; i < genres.size(); i++) {
            MapSqlParameterSource parameterSourceGenres = new MapSqlParameterSource();
            parameterSourceGenres.addValue("book_id", book.getId());
            parameterSourceGenres.addValue("genre_id", genres.get(i).getId());
            batchArgs[i] = parameterSourceGenres;
        }

        int[] updated = jdbc.batchUpdate("insert into books_genres (book_id, genre_id) values (:book_id, :genre_id)", batchArgs);
        if (updated.length == 0) {
            throw new EntityNotFoundException("No records was updated.");
        }
    }

    private void removeGenresRelationsFor(Book book) {
//        MapSqlParameterSource parameterSourceBook = new MapSqlParameterSource();//для неполного удаления, но тогда придется переделать несколько методов.
//        parameterSourceBook.addValue("book_id", book.getId());
//        Set<Long> oldGenres = jdbc.queryForList("select book_id, genre_id from books_genres where book_id = :book_id", parameterSourceBook).stream().map(r -> r.get("GENRE_ID")).map(r -> (Long) r).collect(Collectors.toSet());
//        Set<Long> newGenres = book.getGenres().stream().map(Genre::getId).collect(Collectors.toSet());
//        oldGenres.removeAll(newGenres);
        MapSqlParameterSource deleteParams = new MapSqlParameterSource();
        deleteParams.addValue("book_id", book.getId());
//        deleteParams.addValue("genreIds", oldGenres);
        deleteParams.addValue("genreIds", book.getGenres().stream().map(Genre::getId).collect(Collectors.toSet()));
        jdbc.update("delete from books_genres where book_id = :book_id and genre_id not in (:genreIds)", deleteParams);
    }

    private static class BookRowMapper implements RowMapper<Book> {

        @Override
        public Book mapRow(ResultSet rs, int rowNum) throws SQLException {
            Book book = new Book();
            book.setId(rs.getLong("id"));
            book.setTitle(rs.getString("title"));
            Author author = new Author();
            author.setId(rs.getLong("author_id"));
            author.setFullName(rs.getString("full_name"));
            book.setAuthor(author);
            return book;
        }
    }

    private static class BookGeneresRowMapper implements RowMapper<BookGenreRelation> {

        @Override
        public BookGenreRelation mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new BookGenreRelation(rs.getLong("book_id"), rs.getLong("genre_id"));
        }
    }

    // Использовать для findById
    @SuppressWarnings("ClassCanBeRecord")
    @RequiredArgsConstructor
    private static class BookResultSetExtractor implements ResultSetExtractor<Book> {

        @Override
        public Book extractData(ResultSet rs) throws SQLException, DataAccessException {
            boolean hasData = rs.next();
            if (hasData) {
                Book book = new Book();
                book.setId(rs.getLong("id"));
                book.setTitle(rs.getString("title"));
                Author author = new Author();
                author.setId(rs.getLong("author_id"));
                author.setFullName(rs.getString("full_name"));
                book.setAuthor(author);

                List<Genre> genres = new ArrayList<>();
                Genre genre1 = new Genre();
                genre1.setId(rs.getLong("genre_id"));
                genre1.setName(rs.getString("name"));
                genres.add(genre1);
                while (rs.next()) {
                    Genre genre = new Genre();
                    genre.setId(rs.getLong("genre_id"));
                    genre.setName(rs.getString("name"));
                    genres.add(genre);
                }
                book.setGenres(genres);
                return book;
            } else {
                return null;
            }
        }
    }

    private record BookGenreRelation(long bookId, long genreId) {
    }
}
