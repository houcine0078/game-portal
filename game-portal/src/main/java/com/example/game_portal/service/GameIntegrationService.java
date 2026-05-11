package com.example.game_portal.service;

import com.example.game_portal.entity.Game;
import com.example.game_portal.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class GameIntegrationService {

    @Autowired
    private GameRepository gameRepository;

    /* * ==============================================================================
     * COMMENTAIRE REQUIS : Consommation API REST externe et Sauvegarde
     * SANS SPRING : Utilisation de HttpURLConnection, lecture de flux (InputStream),
     * parsing JSON manuel (org.json), et requêtes JDBC préparées manuelles.
     * AVEC SPRING : RestTemplate gère la requête et le parsing JSON automatiquement.
     * Spring Data JPA gère l'insertion SQL automatiquement via repository.save().
     * ==============================================================================
     */
    public void fetchGamesFromGameDistribution() {
        String apiUrl = "https://catalog.api.gamedistribution.com/api/v2.0/rss/All/?collection=all&categories=All&type=all&subType=all&amount=20&format=json";
        RestTemplate restTemplate = new RestTemplate();

        try {
            JsonNode root = restTemplate.getForObject(apiUrl, JsonNode.class);
            if (root != null && root.isArray()) {
                for (JsonNode node : root) {
                    Game game = new Game();
                    game.setTitle(node.path("Title").asText());
                    game.setDescription(node.path("Description").asText());
                    game.setThumbnailUrl(node.path("Asset").get(0).asText());
                    game.setIframeUrl(node.path("Url").asText());

                    gameRepository.save(game);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur d'importation : " + e.getMessage());
        }
    }
}