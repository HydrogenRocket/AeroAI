package com.aeroai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class OllamaClient {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record Message(String role, String content) {}

    private static String baseUrl() {
        String url = Config.OLLAMA_URL.get().trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Sends a non-streaming chat request and returns the assistant's reply text. */
    public static CompletableFuture<String> chat(List<Message> messages) {
        JsonObject body = new JsonObject();
        body.addProperty("model", Config.MODEL.get());
        body.addProperty("stream", false);
        if (Config.DISABLE_THINKING.get()) {
            body.addProperty("think", false);
        }

        JsonArray msgs = new JsonArray();
        for (Message m : messages) {
            JsonObject o = new JsonObject();
            o.addProperty("role", m.role());
            o.addProperty("content", m.content());
            msgs.add(o);
        }
        body.add("messages", msgs);

        JsonObject options = new JsonObject();
        options.addProperty("temperature", Config.TEMPERATURE.get());
        options.addProperty("num_predict", Config.MAX_TOKENS.get());
        body.add("options", options);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/chat"))
                    .timeout(Duration.ofSeconds(Config.TIMEOUT_SECONDS.get()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(new RuntimeException("Invalid Ollama URL: " + baseUrl()));
        }

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(resp -> {
            JsonObject json = parse(resp.body());
            if (resp.statusCode() != 200) {
                String err = json != null && json.has("error") ? json.get("error").getAsString() : resp.body();
                throw new RuntimeException("Ollama HTTP " + resp.statusCode() + ": " + err);
            }
            if (json == null || !json.has("message")) {
                throw new RuntimeException("Unexpected Ollama response");
            }
            return json.getAsJsonObject("message").get("content").getAsString();
        });
    }

    /** Lists the models installed on the Ollama server. */
    public static CompletableFuture<List<String>> listModels() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + "/api/tags"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(resp -> {
            JsonObject json = parse(resp.body());
            List<String> names = new ArrayList<>();
            if (json != null && json.has("models")) {
                for (JsonElement e : json.getAsJsonArray("models")) {
                    names.add(e.getAsJsonObject().get("name").getAsString());
                }
            }
            return names;
        });
    }

    private static JsonObject parse(String s) {
        try {
            JsonElement e = JsonParser.parseString(s);
            return e.isJsonObject() ? e.getAsJsonObject() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private OllamaClient() {}
}
