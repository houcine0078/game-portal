package com.example.game_portal.controller;

import com.example.game_portal.entity.Game;
import com.example.game_portal.repository.GameRepository;
import com.example.game_portal.service.GameIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des jeux.
 *
 * =========================================================
 * SANS SPRING (Servlet) :
 * =========================================================
 *   @WebServlet("/api/games")
 *   public class GameServlet extends HttpServlet {
 *       protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
 *           // Vérification auth manuelle
 *           // SELECT * FROM games → mapping ResultSet → List<Game> manuel
 *           // Sérialisation JSON manuelle avec JSONArray
 *           resp.setContentType("application/json");
 *           resp.getWriter().write(jsonArray.toString());
 *       }
 *   }
 *
 * =========================================================
 * AVEC SPRING :
 * =========================================================
 *   @RestController + @GetMapping → Spring MVC route automatiquement les requêtes GET
 *   gameRepository.findAll() → SELECT * FROM games (SQL généré par Hibernate)
 *   Jackson sérialise la List<Game> en JSON automatiquement
 *   @PreAuthorize → contrôle d'accès déclaratif basé sur le rôle
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. Routing déclaratif via annotations (pas de web.xml)
 *   2. Aucun SQL d'écrire pour findAll()
 *   3. Sérialisation JSON automatique de n'importe quel objet Java
 *   4. @PreAuthorize centralise la sécurité sans polluer le code métier
 *
 * Note : @CrossOrigin retiré — CORS géré globalement dans SecurityConfig
 */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameRepository gameRepository;
    private final GameIntegrationService gameIntegrationService;

    /**
     * Retourne tous les jeux disponibles.
     * Route publique — accessible sans authentification (configuré dans SecurityConfig).
     *
     * SANS SPRING : SELECT * FROM games via JDBC + mapping ResultSet → Game manuellement.
     * AVEC SPRING : gameRepository.findAll() → SELECT * FROM games (Hibernate).
     *
     * GET /api/games
     */
    @GetMapping
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    /**
     * Import de jeux depuis l'API externe GameDistribution.
     * Réservé aux administrateurs — @PreAuthorize vérifie le rôle ADMIN.
     *
     * SANS SPRING :
     *   String role = (String) session.getAttribute("role");
     *   if (!"ADMIN".equals(role)) { response.sendError(403); return; }
     *
     * AVEC SPRING :
     *   @PreAuthorize("hasRole('ADMIN')") → Spring Security intercepte avant l'appel
     *   et lève AccessDeniedException si le rôle est insuffisant → 403 automatique.
     *
     * POST /api/games/import
     * Header: Authorization: Bearer <token-admin>
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importGames() {
        gameIntegrationService.fetchGamesFromGameDistribution();
        return ResponseEntity.ok("Jeux importés avec succès depuis GameDistribution !");
    }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE — GameServlet + ImportGameServlet (sans Spring MVC)
 * =====================================================================
 *
 * En J2EE, chaque endpoint est une Servlet séparée.
 * Pas de @RestController ni de routing @GetMapping/@PostMapping.
 *
 * ── GameServlet (équivalent de GET /api/games) ────────────────────────
 *
 *   package com.example.servlet;
 *
 *   import com.example.dao.GameDAO;
 *   import com.example.model.Game;
 *   import jakarta.servlet.http.*;
 *   import jakarta.servlet.annotation.WebServlet;
 *   import java.io.IOException;
 *   import java.util.List;
 *
 *   @WebServlet("/api/games")
 *   public class GameServlet extends HttpServlet {
 *
 *       private GameDAO gameDAO = new GameDAO(); // instanciation manuelle
 *
 *       @Override
 *       protected void doGet(HttpServletRequest request, HttpServletResponse response)
 *               throws IOException {
 *
 *           response.setContentType("application/json");
 *           response.setCharacterEncoding("UTF-8");
 *
 *           // CORS manuels (Spring : CorsConfiguration dans SecurityConfig)
 *           response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
 *
 *           // Récupération depuis la base (Spring : gameRepository.findAll())
 *           // Ici gameDAO.findAll() retourne List<Game> via JDBC
 *           List<Game> games = gameDAO.findAll();
 *
 *           // Sérialisation JSON manuelle (Spring : Jackson sérialise automatiquement)
 *           StringBuilder sb = new StringBuilder("[");
 *           for (int i = 0; i < games.size(); i++) {
 *               Game g = games.get(i);
 *               sb.append("{")
 *                 .append("\"id\":").append(g.getId()).append(",")
 *                 .append("\"title\":\"").append(escapeJson(g.getTitle())).append("\",")
 *                 .append("\"description\":\"").append(escapeJson(g.getDescription())).append("\",")
 *                 .append("\"category\":\"").append(escapeJson(g.getCategory())).append("\",")
 *                 .append("\"thumbnailUrl\":\"").append(escapeJson(g.getThumbnailUrl())).append("\",")
 *                 .append("\"iframeUrl\":\"").append(escapeJson(g.getIframeUrl())).append("\"")
 *                 .append("}");
 *               if (i < games.size() - 1) sb.append(",");
 *           }
 *           sb.append("]");
 *           response.getWriter().write(sb.toString());
 *       }
 *
 *       // Encodage JSON minimal — Jackson gère tous les cas edge automatiquement
 *       private String escapeJson(String s) {
 *           if (s == null) return "";
 *           return s.replace("\\", "\\\\").replace("\"", "\\\"")
 *                   .replace("\n", "\\n").replace("\r", "\\r");
 *       }
 *   }
 *
 * ── ImportGameServlet (équivalent de POST /api/games/import + @PreAuthorize ADMIN) ──
 *
 *   @WebServlet("/api/games/import")
 *   public class ImportGameServlet extends HttpServlet {
 *
 *       private GameDAO gameDAO = new GameDAO();
 *
 *       @Override
 *       protected void doPost(HttpServletRequest request, HttpServletResponse response)
 *               throws IOException {
 *
 *           response.setContentType("application/json");
 *           response.setCharacterEncoding("UTF-8");
 *
 *           // Vérification du rôle ADMIN (Spring : @PreAuthorize("hasRole('ADMIN')"))
 *           // Ce code DOIT être répété dans CHAQUE Servlet admin
 *           String role = (String) request.getAttribute("currentRole");
 *           if (!"ROLE_ADMIN".equals(role)) {
 *               response.setStatus(HttpServletResponse.SC_FORBIDDEN);
 *               response.getWriter().write("{\"error\":\"Réservé aux administrateurs\"}");
 *               return;
 *           }
 *
 *           // Appel HTTP externe via HttpURLConnection (Spring : restTemplate.getForObject())
 *           String apiUrl = "https://catalog.api.gamedistribution.com/api/v2.0/rss/All/?amount=20&format=json";
 *           try {
 *               String jsonResponse = HttpClientJ2EE.get(apiUrl);
 *
 *               // Parsing JSON manuel (Spring : Jackson désérialise automatiquement en JsonNode)
 *               org.json.JSONArray root = new org.json.JSONArray(jsonResponse);
 *               for (int i = 0; i < root.length(); i++) {
 *                   org.json.JSONObject node = root.getJSONObject(i);
 *                   Game game = new Game();
 *                   game.setTitle(node.optString("Title"));
 *                   game.setDescription(node.optString("Description"));
 *                   game.setIframeUrl(node.optString("Url"));
 *                   org.json.JSONArray assets = node.optJSONArray("Asset");
 *                   if (assets != null && assets.length() > 0) {
 *                       game.setThumbnailUrl(assets.getString(0));
 *                   }
 *                   gameDAO.save(game); // INSERT JDBC manuel
 *               }
 *               response.setStatus(HttpServletResponse.SC_OK);
 *               response.getWriter().write("{\"message\":\"Jeux importés\"}");
 *           } catch (Exception e) {
 *               response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 *               response.getWriter().write("{\"error\":\"Erreur d'import\"}");
 *           }
 *       }
 *   }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring GameController     : 1 classe, 2 endpoints, Jackson sérialise auto
 *   J2EE                      : 2 Servlets + GameDAO complet + sérialisation JSON manuelle
 *   @PreAuthorize Spring      : AOP — 1 annotation ; J2EE : if/else répété dans chaque Servlet
 *   gameRepository.findAll()  : 1 ligne ; GameDAO.findAll() : ~20 lignes JDBC
 *   Jackson Spring            : sérialise List<Game> automatiquement
 *   J2EE StringBuilder JSON   : fragile, gestion des caractères spéciaux manuelle
 * =====================================================================
 */
