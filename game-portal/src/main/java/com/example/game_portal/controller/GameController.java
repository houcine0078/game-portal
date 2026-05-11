package com.example.game_portal.controller;

import com.example.game_portal.entity.Game;
import com.example.game_portal.repository.GameRepository;
import com.example.game_portal.service.GameIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "http://localhost:4200")
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameIntegrationService gameIntegrationService;



    @GetMapping
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    @PostMapping("/import")
    public ResponseEntity<String> importGames() {
        gameIntegrationService.fetchGamesFromGameDistribution();
        return ResponseEntity.ok("Jeux importés avec succès depuis GameDistribution !");
    }
}