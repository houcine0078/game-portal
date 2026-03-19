package com.example.game_portal.repository;

import com.example.game_portal.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
    // JpaRepository gives us findAll() for free!
}