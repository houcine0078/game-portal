package com.example.game_portal.service;

import com.example.game_portal.entity.Game;
import com.example.game_portal.repository.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameIntegrationService {

    private final GameRepository gameRepository;
    private final RestTemplate restTemplate;

    @Transactional
    public void fetchGamesFromGameDistribution() {
        String apiUrl = "https://catalog.api.gamedistribution.com/api/v2.0/rss/All/"
                + "?collection=all&categories=All&type=all&subType=all&amount=20&format=json";

        try {
            JsonNode root = restTemplate.getForObject(apiUrl, JsonNode.class);

            if (root != null && root.isArray()) {
                for (JsonNode node : root) {
                    Game game = new Game();
                    game.setTitle(node.path("Title").asText());
                    game.setDescription(node.path("Description").asText());

                    JsonNode assets = node.path("Asset");
                    if (assets.isArray() && assets.size() > 0) {
                        game.setThumbnailUrl(assets.get(0).asText());
                    }
                    game.setIframeUrl(node.path("Url").asText());
                    gameRepository.save(game);
                }
                log.info("Import réussi : {} jeux importés", root.size());
            }
        } catch (Exception e) {
            log.error("Erreur d'importation depuis GameDistribution : {}", e.getMessage());
        }
    }
}

/*
 * Without Spring, calling an external API requires using HttpURLConnection manually,
 * then parsing the JSON response by hand:
 *
 *   // Step 1: make the HTTP call
 *   URL url = new URL(apiUrl);
 *   HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 *   conn.setRequestMethod("GET");
 *   conn.setRequestProperty("Accept", "application/json");
 *   conn.setConnectTimeout(5000);
 *   conn.setReadTimeout(15000);
 *
 *   // Step 2: read the response body line by line
 *   BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
 *   StringBuilder body = new StringBuilder();
 *   String line;
 *   while ((line = reader.readLine()) != null) body.append(line);
 *   reader.close();
 *   conn.disconnect(); // must close manually
 *
 *   // Step 3: parse JSON and insert each game with JDBC
 *   JSONArray root = new JSONArray(body.toString());
 *   for (int i = 0; i < root.length(); i++) {
 *       JSONObject node = root.getJSONObject(i);
 *       PreparedStatement ps = conn.prepareStatement("INSERT INTO games (title, ...) VALUES (?, ...)");
 *       ps.setString(1, node.optString("Title"));
 *       ps.executeUpdate();
 *   }
 *
 * With Spring, RestTemplate.getForObject() replaces steps 1 and 2 (one line),
 * Jackson parses the JSON automatically, and gameRepository.save() replaces step 3.
 * @Transactional also means all inserts are rolled back if one fails.
 */
