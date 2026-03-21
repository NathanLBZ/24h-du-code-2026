package LesPointeursFous.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {

    private final String url;
    private final String token;
    private final HttpClient client;

    public ApiClient(String url, String token) {
        this.url = url;
        this.token = token;
        this.client = HttpClient.newHttpClient();
    }

    public String get(String path) throws Exception {
        String fullUrl = url + path;
        System.out.println("[ApiClient] GET " + fullUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("[ApiClient] Status: " + response.statusCode());
        System.out.println("[ApiClient] Response length: " + response.body().length() + " chars");

        return response.body();
    }

    public String post(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        System.out.println(url + path + "\n" + json);
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    public String patch(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    public void delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + path))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();

        client.send(request, HttpResponse.BodyHandlers.discarding());
    }

    public String put(String path, String json) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url + path))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();

    return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
}
}