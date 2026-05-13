package com.example.game_portal.controller;

import com.example.game_portal.dto.AuthResponse;
import com.example.game_portal.dto.LoginRequest;
import com.example.game_portal.dto.RegisterRequest;
import com.example.game_portal.entity.User;
import com.example.game_portal.repository.UserRepository;
import com.example.game_portal.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Identifiants invalides"));
        }
    }

    // GET /api/auth/profile — requires a valid JWT token
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Non authentifié"));
        }
        return ResponseEntity.ok(Map.of(
                "username",  currentUser.getUsername(),
                "email",     currentUser.getEmail(),
                "role",      currentUser.getRole().name(),
                "createdAt", currentUser.getCreatedAt() != null
                        ? currentUser.getCreatedAt().toString() : ""
        ));
    }

    // GET /api/auth/admin/users — ADMIN only
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

/*
 * Without Spring MVC, each endpoint would be a separate Servlet class.
 * Here is what the /profile endpoint looks like as a Servlet:
 *
 *   @WebServlet("/api/auth/profile")
 *   public class ProfileServlet extends HttpServlet {
 *       protected void doGet(HttpServletRequest request, HttpServletResponse response)
 *               throws IOException {
 *           response.setContentType("application/json");
 *
 *           // Get current user from request attribute (set by JwtAuthFilter)
 *           // Spring uses @AuthenticationPrincipal which injects the user automatically
 *           User currentUser = (User) request.getAttribute("currentUser");
 *           if (currentUser == null) {
 *               response.setStatus(401);
 *               response.getWriter().write("{\"error\":\"Not authenticated\"}");
 *               return;
 *           }
 *
 *           // Build JSON manually — Spring returns an object and Jackson serialises it
 *           response.getWriter().write(
 *               "{\"username\":\"" + currentUser.getUsername() + "\"," +
 *               "\"email\":\""     + currentUser.getEmail()    + "\"}"
 *           );
 *       }
 *   }
 *   // Plus a separate entry in web.xml to register the Servlet.
 *
 * With Spring, one controller class handles all auth endpoints with @GetMapping/@PostMapping.
 */
