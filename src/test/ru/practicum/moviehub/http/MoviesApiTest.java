package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static MoviesServer server;
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();
    private static final String BASE = "http://localhost:8080";

    @BeforeAll
    static void start() throws Exception {
        server = new MoviesServer(8080);
        server.start();
    }

    @AfterAll
    static void stop() {
        server.stop(0);
    }

    @BeforeEach
    void clear() {
        server.getStore().clear();
    }

    @Test
    void getMovies_empty_returnsEmptyArray() throws Exception {
        HttpResponse<String> response = get("/movies");
        assertEquals(200, response.statusCode());

        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        assertNotNull(movies);
        assertEquals(0, movies.size());
    }

    @Test
    void getMovies_returnsAddedMovies() throws Exception {
        server.getStore().add(new Movie("Матрица", 1999));
        server.getStore().add(new Movie("Начало", 2010));

        HttpResponse<String> response = get("/movies");
        assertEquals(200, response.statusCode());

        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
    }

    @Test
    void getMovie_returnsAddedMovie() throws Exception {
        server.getStore().add(new Movie("Матрица", 1999));

        HttpResponse<String> response = get("/movies/1");
        assertEquals(200, response.statusCode());

        Movie movie = gson.fromJson(response.body(), Movie.class);
        assertEquals("Матрица", movie.getTitle());
        assertEquals(1999, movie.getYear());
    }

    @Test
    void getMovie_wrongId_returns400() throws Exception {
        HttpResponse<String> response = get("/movies/abc");
        assertEquals(400, response.statusCode());
    }

    @Test
    void getUnknownMovie_returns404() throws Exception {
        HttpResponse<String> response = get("/movies/999");
        assertEquals(404, response.statusCode());
    }

    @Test
    void postMovie_addsMovie() throws Exception {
        Movie movie = new Movie("Интерстеллар", 2014);
        HttpResponse<String> response = post(gson.toJson(movie));

        assertEquals(201, response.statusCode());

        Movie created = gson.fromJson(response.body(), Movie.class);
        assertNotNull(created);
        assertEquals("Интерстеллар", created.getTitle());
        assertEquals(1, created.getId());
    }

    @Test
    void postMovie_invalidTitleAndYear_returns422() throws Exception {
        Movie movie = new Movie("", 1800);
        HttpResponse<String> response = post(gson.toJson(movie));

        assertEquals(422, response.statusCode());
        assertTrue(response.body().contains("Ошибка валидации"));
        assertTrue(response.body().contains("название не должно быть пустым"));
        assertTrue(response.body().contains("год должен быть между"));
    }

    @Test
    void postMovie_invalidJson_returns400() throws Exception {
        HttpResponse<String> response = post("{ это не json }");
        assertEquals(400, response.statusCode());
    }

    @Test
    void postMovie_wrongContentType_returns415() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(415, response.statusCode());
    }

    @Test
    void getMoviesByYear_returnsFilteredList() throws Exception {
        server.getStore().add(new Movie("Матрица", 1999));
        server.getStore().add(new Movie("Начало", 2010));
        server.getStore().add(new Movie("Интерстеллар", 2014));

        HttpResponse<String> response = get("/movies?year=1999");
        assertEquals(200, response.statusCode());

        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(1, movies.size());
        assertEquals("Матрица", movies.getFirst().getTitle());
    }

    @Test
    void getMoviesByYear_invalidYear_returns400() throws Exception {
        HttpResponse<String> response = get("/movies?year=abc");
        assertEquals(400, response.statusCode());
    }

    @Test
    void deleteMovie_removesMovie() throws Exception {
        server.getStore().add(new Movie("Начало", 2010));

        HttpResponse<String> response = delete("/movies/1");
        assertEquals(204, response.statusCode());
        assertEquals(0, server.getStore().getAll().size());
    }

    @Test
    void deleteMovie_wrongId_returns400() throws Exception {
        HttpResponse<String> response = delete("/movies/abc");
        assertEquals(400, response.statusCode());
    }

    @Test
    void deleteUnknownMovie_returns404() throws Exception {
        HttpResponse<String> response = delete("/movies/999");
        assertEquals(404, response.statusCode());
    }

    @Test
    void wrongMethod_returns405() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .DELETE()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}