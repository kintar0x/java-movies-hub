package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ValidationErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private static final int EARLIEST_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        try {
            switch (method) {
                case "GET" -> handleGet(exchange, path, query);
                case "POST" -> handlePost(exchange, path);
                case "DELETE" -> handleDelete(exchange, path);
                default -> sendError(exchange, 405, "Метод не поддерживается");
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Внутренняя ошибка сервера");
        }
    }

    private void handleGet(HttpExchange exchange, String path, String query) throws IOException {
        if (path.equals("/movies")) {
            if (query != null && query.startsWith("year=")) {
                handleFilterByYear(exchange, query);
                return;
            }
            sendJson(exchange, store.getAll(), 200);
            return;
        }

        Integer id = extractId(path);
        if (id == null) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        Optional<Movie> movie = store.getById(id);
        if (movie.isEmpty()) {
            sendError(exchange, 404, "Фильм не найден");
            return;
        }

        sendJson(exchange, movie.get(), 200);
    }

    private void handleFilterByYear(HttpExchange exchange, String query) throws IOException {
        String yearValue = query.substring("year=".length());

        int year;
        try {
            year = Integer.parseInt(yearValue);
        } catch (NumberFormatException e) {
            sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
            return;
        }

        if (year < EARLIEST_YEAR || year > Year.now().getValue() + 1) {
            sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
            return;
        }

        sendJson(exchange, store.getByYear(year), 200);
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        if (!path.equals("/movies")) {
            sendError(exchange, 404, "Ресурс не найден");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("application/json")) {
            sendError(exchange, 415, "Неподдерживаемый тип данных");
            return;
        }

        String body = readBody(exchange);
        Movie movie;

        try {
            movie = gson.fromJson(body, Movie.class);
        } catch (Exception e) {
            sendError(exchange, 400, "Некорректный JSON");
            return;
        }

        List<String> details = validate(movie);
        if (!details.isEmpty()) {
            sendJson(exchange, new ValidationErrorResponse("Ошибка валидации", details), 422);
            return;
        }

        Movie saved = store.add(movie);
        sendJson(exchange, saved, 201);
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        Integer id = extractId(path);
        if (id == null) {
            sendError(exchange, 400, "Некорректный ID");
            return;
        }

        Optional<Movie> movie = store.getById(id);
        if (movie.isEmpty()) {
            sendError(exchange, 404, "Фильм не найден");
            return;
        }

        store.delete(id);
        sendNoContent(exchange);
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Integer extractId(String path) {
        String[] parts = path.split("/");
        if (parts.length != 3 || !parts[1].equals("movies")) {
            return null;
        }

        try {
            return Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String> validate(Movie movie) {
        List<String> details = new ArrayList<>();

        if (movie == null) {
            details.add("тело запроса не может быть пустым");
            return details;
        }

        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            details.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
            details.add("название не должно превышать " + MAX_TITLE_LENGTH + " символов");
        }

        int currentYear = Year.now().getValue() + 1;
        if (movie.getYear() < EARLIEST_YEAR || movie.getYear() > currentYear) {
            details.add("год должен быть между " + EARLIEST_YEAR + " и " + currentYear);
        }

        return details;
    }
}