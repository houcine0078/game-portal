package com.example.game_portal.service;

import com.example.game_portal.entity.Game;
import com.example.game_portal.repository.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Service d'intégration avec l'API externe GameDistribution.
 *
 * =========================================================
 * SANS SPRING (HttpURLConnection + JDBC) :
 * =========================================================
 *   // Appel HTTP manuel
 *   URL url = new URL("https://catalog.api.gamedistribution.com/...");
 *   HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 *   conn.setRequestMethod("GET");
 *   conn.setRequestProperty("Accept", "application/json");
 *
 *   BufferedReader reader = new BufferedReader(
 *       new InputStreamReader(conn.getInputStream()));
 *   StringBuilder body = new StringBuilder();
 *   String line;
 *   while ((line = reader.readLine()) != null) body.append(line);
 *   reader.close(); conn.disconnect();
 *
 *   // Parsing JSON manuel avec org.json
 *   JSONArray jsonArray = new JSONArray(body.toString());
 *   for (int i = 0; i < jsonArray.length(); i++) {
 *       JSONObject obj = jsonArray.getJSONObject(i);
 *       String title = obj.getString("Title");
 *       // INSERT JDBC manuel...
 *   }
 *
 * =========================================================
 * AVEC SPRING (RestTemplate + Spring Data JPA) :
 * =========================================================
 *   JsonNode root = restTemplate.getForObject(apiUrl, JsonNode.class);
 *   // Jackson désérialise automatiquement la réponse JSON
 *   // gameRepository.save(game) → INSERT SQL généré par Hibernate
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. RestTemplate : appel HTTP en une ligne + désérialisation JSON automatique
 *   2. @Transactional : si un save() échoue, tout le batch est annulé (cohérence)
 *   3. @Slf4j (Lombok) : logging professionnel sans instanciation manuelle de Logger
 *   4. Injection du bean RestTemplate (configuré dans WebConfig) → testable
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameIntegrationService {

    private final GameRepository gameRepository;

    // Bean RestTemplate injecté depuis WebConfig (singleton, configurable)
    // SANS SPRING : new HttpURLConnection() dans chaque méthode
    private final RestTemplate restTemplate;

    @Transactional
    public void fetchGamesFromGameDistribution() {
        String apiUrl = "https://catalog.api.gamedistribution.com/api/v2.0/rss/All/"
                + "?collection=all&categories=All&type=all&subType=all&amount=20&format=json";

        try {
            // SANS SPRING : HttpURLConnection + lecture de flux + parsing org.json (20+ lignes)
            // AVEC SPRING : une seule ligne, Jackson gère la désérialisation
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

                    // SANS SPRING : PreparedStatement + executeUpdate() + conn.commit()
                    // AVEC SPRING : Hibernate génère et exécute l'INSERT automatiquement
                    gameRepository.save(game);
                }
                log.info("Import réussi : {} jeux importés", root.size());
            }
        } catch (Exception e) {
            // SANS SPRING : System.err.println() — pas de niveau de log, pas de contexte
            // AVEC SPRING : @Slf4j → log.error() avec niveau, timestamp, thread, classe
            log.error("Erreur d'importation depuis GameDistribution : {}", e.getMessage());
        }
    }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE — Appel API externe via HttpURLConnection + JDBC
 * =====================================================================
 *
 * En J2EE, il n'y a pas de RestTemplate ni de @Transactional automatique.
 * L'appel HTTP, le parsing JSON et l'insertion en base sont tous manuels.
 *
 *   package com.example.service;
 *
 *   import com.example.dao.GameDAO;
 *   import com.example.model.Game;
 *   import org.json.JSONArray;
 *   import org.json.JSONObject;
 *   import java.io.*;
 *   import java.net.*;
 *   import java.nio.charset.StandardCharsets;
 *
 *   public class GameIntegrationServiceJ2EE {
 *
 *       // Instanciation manuelle (Spring : @RequiredArgsConstructor + injection automatique)
 *       private GameDAO gameDAO = new GameDAO();
 *
 *       // Pas de @Transactional — si la boucle d'insertions échoue à mi-chemin,
 *       // les jeux déjà insérés restent en base (état incohérent)
 *       // Spring @Transactional : rollback automatique de toutes les insertions si exception
 *       public void fetchGamesFromGameDistribution() {
 *           String apiUrl = "https://catalog.api.gamedistribution.com/api/v2.0/rss/All/"
 *                   + "?collection=all&categories=All&type=all&subType=all&amount=20&format=json";
 *
 *           // === ÉTAPE 1 : Appel HTTP via HttpURLConnection ===
 *           // Spring : restTemplate.getForObject(apiUrl, JsonNode.class) — 1 ligne
 *           // J2EE   : ~25 lignes de configuration + lecture + fermeture
 *           String responseBody;
 *           try {
 *               URL url = new URL(apiUrl);
 *               HttpURLConnection conn = (HttpURLConnection) url.openConnection();
 *               conn.setRequestMethod("GET");
 *               conn.setRequestProperty("Accept",         "application/json");
 *               conn.setRequestProperty("Accept-Charset", "UTF-8");
 *               conn.setConnectTimeout(5000);
 *               conn.setReadTimeout(15000);
 *
 *               int statusCode = conn.getResponseCode();
 *               if (statusCode < 200 || statusCode >= 300) {
 *                   System.err.println("HTTP " + statusCode + " — import échoué");
 *                   conn.disconnect();
 *                   return;
 *               }
 *
 *               StringBuilder body = new StringBuilder();
 *               try (BufferedReader reader = new BufferedReader(
 *                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
 *                   String line;
 *                   while ((line = reader.readLine()) != null) body.append(line);
 *               }
 *               conn.disconnect(); // fermeture manuelle obligatoire
 *               responseBody = body.toString();
 *
 *           } catch (IOException e) {
 *               // Spring : log.error("...", e) avec niveau, timestamp, thread
 *               // J2EE  : System.err.println() sans contexte
 *               System.err.println("Erreur HTTP : " + e.getMessage());
 *               return;
 *           }
 *
 *           // === ÉTAPE 2 : Parsing JSON manuel ===
 *           // Spring : Jackson désérialise automatiquement en JsonNode
 *           //          node.path("Title").asText() — null-safe
 *           // J2EE   : org.json.JSONArray — lève JSONException si mal formé
 *           try {
 *               JSONArray root = new JSONArray(responseBody);
 *               int count = 0;
 *
 *               for (int i = 0; i < root.length(); i++) {
 *                   JSONObject node = root.getJSONObject(i);
 *
 *                   Game game = new Game();
 *                   game.setTitle(node.optString("Title", ""));
 *                   game.setDescription(node.optString("Description", ""));
 *                   game.setIframeUrl(node.optString("Url", ""));
 *
 *                   // Lecture d'un tableau imbriqué (Spring : node.path("Asset") — null-safe)
 *                   JSONArray assets = node.optJSONArray("Asset");
 *                   if (assets != null && assets.length() > 0) {
 *                       game.setThumbnailUrl(assets.optString(0, ""));
 *                   }
 *
 *                   // === ÉTAPE 3 : Insertion JDBC manuelle ===
 *                   // Spring : gameRepository.save(game) — 1 ligne, Hibernate génère l'INSERT
 *                   // J2EE   : gameDAO.save(game) — ~15 lignes JDBC dans GameDAO
 *                   gameDAO.save(game);
 *                   count++;
 *               }
 *
 *               // Spring : log.info("Import réussi : {} jeux", count) — formaté automatiquement
 *               System.out.println("[INFO] Import réussi : " + count + " jeux importés");
 *
 *           } catch (org.json.JSONException e) {
 *               System.err.println("[ERREUR] JSON invalide : " + e.getMessage());
 *           } catch (Exception e) {
 *               System.err.println("[ERREUR] Importation : " + e.getMessage());
 *               // Pas de rollback — les jeux déjà insérés restent en base
 *               // Spring @Transactional : annule toutes les insertions si exception
 *           }
 *       }
 *   }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring fetchGames()         : ~30 lignes — RestTemplate + Jackson + Hibernate + log.info
 *   J2EE fetchGamesJ2EE()       : ~80 lignes — HttpURLConnection + org.json + JDBC + System.err
 *   @Transactional Spring       : rollback automatique si une insertion échoue
 *   J2EE sans transaction       : état incohérent possible (certains jeux insérés, d'autres non)
 *   RestTemplate Spring         : désérialisation JSON directe en JsonNode (null-safe)
 *   HttpURLConnection J2EE      : gestion manuelle des flux, timeout, statut HTTP, fermeture
 *   @Slf4j Spring               : log.info/error avec niveau, timestamp, classe, thread
 *   System.out/err J2EE         : aucun niveau de log, difficile à filtrer, pas de contexte
 * =====================================================================
 */
