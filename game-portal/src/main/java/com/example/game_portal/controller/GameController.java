package com.example.game_portal.controller;

import com.example.game_portal.entity.Game;
import com.example.game_portal.repository.GameRepository;
import com.example.game_portal.service.GameIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameRepository gameRepository;
    private final GameIntegrationService gameIntegrationService;

    // GET /api/games — public, no authentication needed
    @GetMapping
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    // POST /api/games/import — ADMIN only
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> importGames() {
        gameIntegrationService.fetchGamesFromGameDistribution();
        return ResponseEntity.ok("Jeux importés avec succès depuis GameDistribution !");
    }
}

/*
 * Without Spring MVC, the GET /api/games endpoint would be a Servlet that manually
 * queries the database and builds the JSON response character by character:
 *
 *   @WebServlet("/api/games")
 *   public class GameServlet extends HttpServlet {
 *       private GameDAO gameDAO = new GameDAO();
 *
 *       protected void doGet(HttpServletRequest request, HttpServletResponse response)
 *               throws IOException {
 *           response.setContentType("application/json");
 *           response.setHeader("Access-Control-Allow-Origin", "http://localhost:4200");
 *
 *           List<Game> games = gameDAO.findAll(); // raw JDBC query
 *
 *           // Build JSON manually — Spring + Jackson serialises List<Game> automatically
 *           StringBuilder sb = new StringBuilder("[");
 *           for (int i = 0; i < games.size(); i++) {
 *               Game g = games.get(i);
 *               sb.append("{\"id\":").append(g.getId())
 *                 .append(",\"title\":\"").append(g.getTitle()).append("\"}");
 *               if (i < games.size() - 1) sb.append(",");
 *           }
 *           sb.append("]");
 *           response.getWriter().write(sb.toString());
 *       }
 *   }
 *
 * With Spring, getAllGames() is two lines — the framework handles JSON serialisation
 * and the repository handles the SQL query.
 */
