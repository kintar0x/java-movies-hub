package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesStore {
    private final List<Movie> movies = new ArrayList<>();
    private int nextId = 1;

    public List<Movie> getAll() {
        return new ArrayList<>(movies);
    }

    public List<Movie> getByYear(int year) {
        return movies.stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public Movie add(Movie movie) {
        movie.setId(nextId++);
        movies.add(movie);
        return movie;
    }

    public Optional<Movie> getById(int id) {
        return movies.stream()
                .filter(movie -> movie.getId() == id)
                .findFirst();
    }

    public void delete(int id) {
        movies.removeIf(movie -> movie.getId() == id);
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }
}