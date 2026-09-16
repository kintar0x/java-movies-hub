package ru.practicum.moviehub.api;

import java.util.List;

public record ValidationErrorResponse(String error, List<String> details) {
}