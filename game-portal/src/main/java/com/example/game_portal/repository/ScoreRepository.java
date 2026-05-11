package com.example.game_portal.repository;

import com.example.game_portal.entity.Score;
import com.example.game_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    List<Score> findByGameType(String gameType);
    List<Score> findByUserOrderByPlayedAtDesc(User user);
}
