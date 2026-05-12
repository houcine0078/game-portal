package com.example.game_portal.controller;

import com.example.game_portal.entity.Score;
import com.example.game_portal.entity.User;
import com.example.game_portal.repository.ScoreRepository;
import com.example.game_portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour les scores et le classement (leaderboard).
 *
 * =========================================================
 * SANS SPRING (Servlet + JDBC) :
 * =========================================================
 *   @WebServlet("/api/scores")
 *   public class ScoreServlet extends HttpServlet {
 *       protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
 *           // Vérification session auth manuelle
 *           HttpSession session = req.getSession(false);
 *           if (session == null) { resp.sendError(401); return; }
 *
 *           // Lecture JSON manuel + parsing
 *           // INSERT INTO scores (user_id, game_type, score) VALUES (?, ?, ?)
 *           // Gestion Connection, PreparedStatement, SQLException manuelle
 *       }
 *   }
 *
 * =========================================================
 * AVEC SPRING :
 * =========================================================
 *   @AuthenticationPrincipal User currentUser → utilisateur injecté depuis le SecurityContext
 *   (peuplé par JwtAuthFilter — aucun accès à HttpSession)
 *   scoreRepository.save() → INSERT SQL généré par Hibernate
 *   @Transactional (hérité du Repository) → rollback automatique si erreur
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. @AuthenticationPrincipal évite de passer username en body (plus sécurisé)
 *   2. findByUserOrderByPlayedAtDesc() → SQL ORDER BY généré automatiquement
 *   3. Stream API + Jackson : transformation et sérialisation sans boilerplate
 */
@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;

    /**
     * Enregistre un score pour l'utilisateur connecté.
     *
     * SANS SPRING :
     *   session.getAttribute("user") pour récupérer l'utilisateur
     *   INSERT INTO scores (user_id, game_type, score, accuracy, duration) VALUES (...)
     *   avec PreparedStatement et gestion manuelle de la connexion.
     *
     * AVEC SPRING :
     *   @AuthenticationPrincipal injecte directement l'objet User du SecurityContext.
     *   scoreRepository.save() → Hibernate génère l'INSERT.
     *
     * POST /api/scores
     * Header: Authorization: Bearer <token>
     * Body: { "gameType": "snake", "score": 1500, "accuracy": 85, "duration": 120 }
     */
    @PostMapping
    public ResponseEntity<?> saveScore(@RequestBody Map<String, Object> body,
                                       @AuthenticationPrincipal User currentUser) {
        // currentUser est injecté depuis le SecurityContext (peuplé par JwtAuthFilter)
        // SANS SPRING : String username = (String) session.getAttribute("username");
        //               User user = userDAO.findByUsername(username);
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Non authentifié");
        }

        String gameType = (String) body.get("gameType");
        Integer score = ((Number) body.get("score")).intValue();

        Score s = new Score();
        s.setUser(currentUser);
        s.setGameType(gameType);
        s.setScore(score);

        if (body.get("accuracy") != null) s.setAccuracy(((Number) body.get("accuracy")).intValue());
        if (body.get("duration") != null) s.setDuration(((Number) body.get("duration")).intValue());

        // SANS SPRING : PreparedStatement INSERT + executeUpdate() + conn.commit()
        // AVEC SPRING : une seule ligne, transaction gérée par @Transactional du Repository
        scoreRepository.save(s);
        return ResponseEntity.ok("Score saved");
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(@RequestParam String game) {
        List<Score> allScores = scoreRepository.findByGameType(game);

        // Keep only the best score per user
        Map<String, Score> bestPerUser = new HashMap<>();
        for (Score s : allScores) {
            String uname = s.getUser().getUsername();
            Score existing = bestPerUser.get(uname);
            if (existing == null) {
                bestPerUser.put(uname, s);
            } else {
                boolean isBetter = "memory-match".equals(game)
                        ? s.getScore() < existing.getScore()
                        : s.getScore() > existing.getScore();
                if (isBetter) bestPerUser.put(uname, s);
            }
        }

        List<Score> best = new ArrayList<>(bestPerUser.values());
        if ("memory-match".equals(game)) {
            best.sort(Comparator.comparingInt(Score::getScore));
        } else {
            best.sort(Comparator.comparingInt(Score::getScore).reversed());
        }

        List<Map<String, Object>> result = best.stream()
                .limit(10)
                .map(s -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("username", s.getUser().getUsername());
                    entry.put("score", s.getScore());
                    if (s.getAccuracy() != null) entry.put("accuracy", s.getAccuracy());
                    if (s.getDuration() != null) entry.put("duration", s.getDuration());
                    entry.put("playedAt", s.getPlayedAt() != null ? s.getPlayedAt().toString() : null);
                    return entry;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Historique des scores de l'utilisateur connecté.
     *
     * SANS SPRING :
     *   String username = (String) session.getAttribute("username");
     *   SELECT * FROM scores WHERE user_id = ? ORDER BY played_at DESC
     *   Mapping ResultSet → Score manuel
     *
     * AVEC SPRING :
     *   @AuthenticationPrincipal → utilisateur depuis SecurityContext
     *   findByUserOrderByPlayedAtDesc() → SQL généré avec ORDER BY
     *
     * GET /api/scores/history
     * Header: Authorization: Bearer <token>
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@AuthenticationPrincipal User currentUser) {
        // SANS SPRING : session.getAttribute("username") + requête SQL manuelle
        // AVEC SPRING : @AuthenticationPrincipal injecte l'utilisateur directement
        User user = currentUser != null
                ? currentUser
                : null;
        if (user == null) return ResponseEntity.notFound().build();

        List<Score> scores = scoreRepository.findByUserOrderByPlayedAtDesc(user);
        List<Map<String, Object>> result = scores.stream()
                .map(s -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("gameType", s.getGameType());
                    entry.put("score", s.getScore());
                    if (s.getAccuracy() != null) entry.put("accuracy", s.getAccuracy());
                    if (s.getDuration() != null) entry.put("duration", s.getDuration());
                    entry.put("playedAt", s.getPlayedAt() != null ? s.getPlayedAt().toString() : null);
                    return entry;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE — ScoreServlet + LeaderboardServlet + HistoryServlet
 * =====================================================================
 *
 * En J2EE, chaque endpoint du ScoreController est une Servlet séparée.
 *
 * ── ScoreSaveServlet (équivalent de POST /api/scores) ─────────────────
 *
 *   package com.example.servlet;
 *
 *   import com.example.dao.ScoreDAO;
 *   import com.example.dao.UserDAO;
 *   import com.example.model.Score;
 *   import com.example.model.User;
 *   import jakarta.servlet.http.*;
 *   import jakarta.servlet.annotation.WebServlet;
 *   import java.io.*;
 *
 *   @WebServlet("/api/scores")
 *   public class ScoreSaveServlet extends HttpServlet {
 *
 *       private ScoreDAO scoreDAO = new ScoreDAO();
 *       private UserDAO  userDAO  = new UserDAO();
 *
 *       @Override
 *       protected void doPost(HttpServletRequest request, HttpServletResponse response)
 *               throws IOException {
 *
 *           response.setContentType("application/json");
 *           response.setCharacterEncoding("UTF-8");
 *
 *           // Récupération de l'utilisateur courant depuis l'attribut de requête
 *           // (propagé par JwtAuthFilterJ2EE via request.setAttribute())
 *           // Spring : @AuthenticationPrincipal User currentUser → injection automatique
 *           User currentUser = (User) request.getAttribute("currentUser");
 *           if (currentUser == null) {
 *               response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *               response.getWriter().write("{\"error\":\"Non authentifié\"}");
 *               return;
 *           }
 *
 *           // Lecture du JSON (Spring : @RequestBody Map<String, Object> body)
 *           StringBuilder body = new StringBuilder();
 *           try (BufferedReader reader = request.getReader()) {
 *               String line;
 *               while ((line = reader.readLine()) != null) body.append(line);
 *           }
 *           org.json.JSONObject json = new org.json.JSONObject(body.toString());
 *           String  gameType = json.getString("gameType");
 *           int     score    = json.getInt("score");
 *           Integer accuracy = json.has("accuracy") ? json.getInt("accuracy") : null;
 *           Integer duration = json.has("duration") ? json.getInt("duration") : null;
 *
 *           // Création et sauvegarde du score (Spring : new Score() + scoreRepository.save())
 *           Score s = new Score();
 *           s.setUser(currentUser);
 *           s.setGameType(gameType);
 *           s.setScore(score);
 *           s.setAccuracy(accuracy);
 *           s.setDuration(duration);
 *           s.setPlayedAt(java.time.LocalDateTime.now());
 *
 *           // INSERT JDBC manuel (Spring : scoreRepository.save() → Hibernate génère l'INSERT)
 *           scoreDAO.save(s);
 *
 *           response.setStatus(HttpServletResponse.SC_OK);
 *           response.getWriter().write("{\"message\":\"Score saved\"}");
 *       }
 *   }
 *
 * ── LeaderboardServlet (équivalent de GET /api/scores/leaderboard) ────
 *
 *   @WebServlet("/api/scores/leaderboard")
 *   public class LeaderboardServlet extends HttpServlet {
 *
 *       private ScoreDAO scoreDAO = new ScoreDAO();
 *
 *       @Override
 *       protected void doGet(HttpServletRequest request, HttpServletResponse response)
 *               throws IOException {
 *
 *           response.setContentType("application/json");
 *           response.setCharacterEncoding("UTF-8");
 *
 *           // Lecture du paramètre de requête (Spring : @RequestParam String game)
 *           String game = request.getParameter("game");
 *           if (game == null || game.isEmpty()) {
 *               response.setStatus(400);
 *               response.getWriter().write("{\"error\":\"Paramètre 'game' requis\"}");
 *               return;
 *           }
 *
 *           // SELECT depuis la base JDBC (Spring : scoreRepository.findByGameType(game))
 *           java.util.List<Score> allScores = scoreDAO.findByGameType(game);
 *
 *           // Logique métier : meilleur score par utilisateur
 *           java.util.Map<String, Score> bestPerUser = new java.util.HashMap<>();
 *           for (Score s : allScores) {
 *               String uname = s.getUser().getUsername();
 *               Score existing = bestPerUser.get(uname);
 *               if (existing == null) {
 *                   bestPerUser.put(uname, s);
 *               } else {
 *                   boolean isBetter = "memory-match".equals(game)
 *                       ? s.getScore() < existing.getScore()
 *                       : s.getScore() > existing.getScore();
 *                   if (isBetter) bestPerUser.put(uname, s);
 *               }
 *           }
 *
 *           java.util.List<Score> best = new java.util.ArrayList<>(bestPerUser.values());
 *           if ("memory-match".equals(game)) {
 *               best.sort(java.util.Comparator.comparingInt(Score::getScore));
 *           } else {
 *               best.sort(java.util.Comparator.comparingInt(Score::getScore).reversed());
 *           }
 *
 *           // Sérialisation JSON manuelle du top 10 (Spring : Jackson sérialise automatiquement)
 *           StringBuilder sb = new StringBuilder("[");
 *           int limit = Math.min(10, best.size());
 *           for (int i = 0; i < limit; i++) {
 *               Score s = best.get(i);
 *               sb.append("{")
 *                 .append("\"username\":\"").append(s.getUser().getUsername()).append("\",")
 *                 .append("\"score\":").append(s.getScore());
 *               if (s.getAccuracy() != null) sb.append(",\"accuracy\":").append(s.getAccuracy());
 *               if (s.getDuration() != null) sb.append(",\"duration\":").append(s.getDuration());
 *               sb.append(",\"playedAt\":\"").append(s.getPlayedAt()).append("\"")
 *                 .append("}");
 *               if (i < limit - 1) sb.append(",");
 *           }
 *           sb.append("]");
 *           response.getWriter().write(sb.toString());
 *       }
 *   }
 *
 * ── HistoryServlet (équivalent de GET /api/scores/history) ────────────
 *
 *   @WebServlet("/api/scores/history")
 *   public class HistoryServlet extends HttpServlet {
 *
 *       private ScoreDAO scoreDAO = new ScoreDAO();
 *
 *       @Override
 *       protected void doGet(HttpServletRequest request, HttpServletResponse response)
 *               throws IOException {
 *
 *           response.setContentType("application/json");
 *           response.setCharacterEncoding("UTF-8");
 *
 *           // Récupération de l'utilisateur (Spring : @AuthenticationPrincipal)
 *           User currentUser = (User) request.getAttribute("currentUser");
 *           if (currentUser == null) {
 *               response.setStatus(404);
 *               response.getWriter().write("{}");
 *               return;
 *           }
 *
 *           // SQL : SELECT * FROM scores WHERE user_id = ? ORDER BY played_at DESC
 *           // Spring : scoreRepository.findByUserOrderByPlayedAtDesc(user) — 1 ligne
 *           java.util.List<Score> scores = scoreDAO.findByUserOrderByPlayedAtDesc(currentUser);
 *
 *           // Sérialisation JSON manuelle
 *           StringBuilder sb = new StringBuilder("[");
 *           for (int i = 0; i < scores.size(); i++) {
 *               Score s = scores.get(i);
 *               sb.append("{")
 *                 .append("\"gameType\":\"").append(s.getGameType()).append("\",")
 *                 .append("\"score\":").append(s.getScore());
 *               if (s.getAccuracy() != null) sb.append(",\"accuracy\":").append(s.getAccuracy());
 *               if (s.getDuration() != null) sb.append(",\"duration\":").append(s.getDuration());
 *               sb.append(",\"playedAt\":\"").append(s.getPlayedAt()).append("\"").append("}");
 *               if (i < scores.size() - 1) sb.append(",");
 *           }
 *           sb.append("]");
 *           response.getWriter().write(sb.toString());
 *       }
 *   }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring ScoreController             : 1 classe, 3 endpoints, ~60 lignes utiles
 *   J2EE                               : 3 Servlets + ScoreDAO complet ≈ 250+ lignes
 *   @AuthenticationPrincipal Spring    : injection directe — sécurisée, zéro code
 *   J2EE currentUser                   : request.getAttribute() — propagation manuelle,
 *                                        risque de NullPointerException si filtre mal configuré
 *   scoreRepository.findByGameType()   : SQL généré par Spring ; ScoreDAO : SELECT JDBC manuel
 *   Jackson Spring                     : sérialise List<Map> automatiquement en JSON
 *   J2EE StringBuilder JSON            : fragile, verbeux, aucun échappement automatique
 * =====================================================================
 */
