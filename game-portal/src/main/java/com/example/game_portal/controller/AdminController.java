package com.example.game_portal.controller;

import com.example.game_portal.dto.CreateUserRequest;
import com.example.game_portal.entity.Game;
import com.example.game_portal.entity.Role;
import com.example.game_portal.entity.User;
import com.example.game_portal.repository.GameRepository;
import com.example.game_portal.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// All routes require ADMIN role — enforced by @PreAuthorize on the class
// GET    /api/admin/users        — list all users
// POST   /api/admin/users        — create a user
// DELETE /api/admin/users/{id}   — delete a user
// GET    /api/admin/games        — list all games
// DELETE /api/admin/games/{id}   — delete a game
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository  userRepository;
    private final GameRepository  gameRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username already taken"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already registered"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .build();
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User created successfully",
                             "username", user.getUsername(),
                             "role",     user.getRole().name()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }
        if ("admin".equals(user.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot delete the admin account"));
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @GetMapping("/games")
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    @DeleteMapping("/games/{id}")
    public ResponseEntity<?> deleteGame(@PathVariable Long id) {
        if (!gameRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Game not found"));
        }
        gameRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Game deleted successfully"));
    }
}

/*
 * Without Spring, each of these 5 endpoints would be a separate Servlet class.
 * The ADMIN role check would also need to be repeated manually inside each one:
 *
 *   @WebServlet("/api/admin/users")
 *   public class AdminUsersServlet extends HttpServlet {
 *       protected void doGet(HttpServletRequest request, HttpServletResponse response)
 *               throws IOException {
 *           // Check role manually — Spring uses @PreAuthorize("hasRole('ADMIN')")
 *           String role = (String) request.getAttribute("currentRole");
 *           if (!"ROLE_ADMIN".equals(role)) {
 *               response.setStatus(403);
 *               response.getWriter().write("{\"error\":\"Access denied\"}");
 *               return;
 *           }
 *           // Then fetch users with JDBC and build JSON manually
 *       }
 *   }
 *   // Plus separate Servlets for POST, DELETE, and the games endpoints.
 *   // Plus entries in web.xml for every Servlet.
 *
 * With Spring, @PreAuthorize("hasRole('ADMIN')") on the class protects every
 * method automatically, and one controller handles all 5 endpoints.
 */
