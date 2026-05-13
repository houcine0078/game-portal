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

@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;

    // POST /api/scores — save a score for the logged-in user
    @PostMapping
    public ResponseEntity<?> saveScore(@RequestBody Map<String, Object> body,
                                       @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Non authentifié");
        }

        String  gameType = (String) body.get("gameType");
        Integer score    = ((Number) body.get("score")).intValue();

        Score s = new Score();
        s.setUser(currentUser);
        s.setGameType(gameType);
        s.setScore(score);
        if (body.get("accuracy") != null) s.setAccuracy(((Number) body.get("accuracy")).intValue());
        if (body.get("duration") != null) s.setDuration(((Number) body.get("duration")).intValue());

        scoreRepository.save(s);
        return ResponseEntity.ok("Score saved");
    }

    // GET /api/scores/leaderboard?game=... — public
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard(@RequestParam String game) {
        List<Score> allScores = scoreRepository.findByGameType(game);

        Map<String, Score> bestPerUser = new HashMap<>();
        for (Score s : allScores) {
            String uname    = s.getUser().getUsername();
            Score  existing = bestPerUser.get(uname);
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
                    entry.put("score",    s.getScore());
                    if (s.getAccuracy() != null) entry.put("accuracy", s.getAccuracy());
                    if (s.getDuration() != null) entry.put("duration", s.getDuration());
                    entry.put("playedAt", s.getPlayedAt() != null ? s.getPlayedAt().toString() : null);
                    return entry;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // GET /api/scores/history — scores for the logged-in user
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) return ResponseEntity.notFound().build();

        List<Score> scores = scoreRepository.findByUserOrderByPlayedAtDesc(currentUser);
        List<Map<String, Object>> result = scores.stream()
                .map(s -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("gameType", s.getGameType());
                    entry.put("score",    s.getScore());
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
 * Without Spring, saving a score would require a Servlet that manually reads the JSON body,
 * gets the current user from the session, and inserts the record with raw JDBC:
 *
 *   @WebServlet("/api/scores")
 *   public class ScoreSaveServlet extends HttpServlet {
 *       private ScoreDAO scoreDAO = new ScoreDAO();
 *
 *       protected void doPost(HttpServletRequest request, HttpServletResponse response)
 *               throws IOException {
 *           // Get current user from session — Spring uses @AuthenticationPrincipal
 *           User currentUser = (User) request.getAttribute("currentUser");
 *           if (currentUser == null) { response.setStatus(401); return; }
 *
 *           // Read JSON body — Spring uses @RequestBody Map<String, Object>
 *           StringBuilder body = new StringBuilder();
 *           BufferedReader reader = request.getReader();
 *           String line;
 *           while ((line = reader.readLine()) != null) body.append(line);
 *           JSONObject json = new JSONObject(body.toString());
 *
 *           // Insert with raw JDBC — Spring uses scoreRepository.save(s)
 *           PreparedStatement ps = conn.prepareStatement(
 *               "INSERT INTO scores (user_id, game_type, score) VALUES (?, ?, ?)");
 *           ps.setLong(1, currentUser.getId());
 *           ps.setString(2, json.getString("gameType"));
 *           ps.setInt(3, json.getInt("score"));
 *           ps.executeUpdate();
 *
 *           response.getWriter().write("{\"message\":\"Score saved\"}");
 *       }
 *   }
 *
 * With Spring, @AuthenticationPrincipal injects the user directly and
 * scoreRepository.save() handles the INSERT in one line.
 */
