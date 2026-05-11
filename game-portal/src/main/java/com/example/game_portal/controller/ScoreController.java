package com.example.game_portal.controller;

import com.example.game_portal.entity.Score;
import com.example.game_portal.entity.User;
import com.example.game_portal.repository.ScoreRepository;
import com.example.game_portal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    @Autowired
    private ScoreRepository scoreRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> saveScore(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String gameType = (String) body.get("gameType");
        Integer score = ((Number) body.get("score")).intValue();

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        Score s = new Score();
        s.setUser(user);
        s.setGameType(gameType);
        s.setScore(score);

        if (body.get("accuracy") != null) s.setAccuracy(((Number) body.get("accuracy")).intValue());
        if (body.get("duration") != null) s.setDuration(((Number) body.get("duration")).intValue());

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
}
