package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer(int port) throws IOException {
        this.store = new MoviesStore();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext("/movies", new MoviesHandler(store));
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен на порту " + server.getAddress().getPort());
    }

    public void stop(int delay) {
        server.stop(delay);
    }

    public MoviesStore getStore() {
        return store;
    }
}