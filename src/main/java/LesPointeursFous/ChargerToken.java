package LesPointeursFous;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ChargerToken {

    public void refreshAccessToken(String apiUrl, String password) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // Préparation des données du formulaire (x-www-form-urlencoded)
        Map<String, String> formData = Map.of(
            "client_id", "vaissals-backend",
            "username", "les-pointeurs-fou",
            "password", password, // Utilisation du password lu dans le .env
            "grant_type", "password"
        );

        String formBody = formData.entrySet().stream()
            .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                      URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // On suppose que le token est dans le champ "access_token" du JSON de réponse
            String body = response.body();
            String newToken = body.replaceAll(".*\"access_token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
            
            // Sauvegarde dans le .env (par exemple sous la clé API_KEY)
            updateEnvFile("API_KEY", newToken);
        } else {
            throw new RuntimeException("Échec de l'authentification : " + response.statusCode() + " - " + response.body());
        }
    }

    private Optional<String> getEnvValue(String key) throws IOException {
        Path path = Paths.get(".env");
        if (!Files.exists(path)) return Optional.empty();
        return Files.readAllLines(path).stream()
                .filter(line -> line.startsWith(key + "="))
                .map(line -> line.substring(line.indexOf("=") + 1).trim())
                .findFirst();
    }

    private void updateEnvFile(String key, String value) throws IOException {
        Path path = Paths.get(".env");
        if (!Files.exists(path)) Files.createFile(path);

        List<String> lines = Files.readAllLines(path);
        List<String> updatedLines = new ArrayList<>();
        boolean found = false;

        for (String line : lines) {
            if (line.trim().startsWith(key + "=")) {
                updatedLines.add(key + "=" + value);
                found = true;
            } else {
                updatedLines.add(line);
            }
        }
        if (!found) updatedLines.add(key + "=" + value);

        Files.write(path, updatedLines, StandardOpenOption.TRUNCATE_EXISTING);
    }
}