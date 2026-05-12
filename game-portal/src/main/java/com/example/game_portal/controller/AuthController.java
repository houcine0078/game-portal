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

/**
 * Contrôleur REST gérant l'authentification et la gestion des utilisateurs.
 *
 * =========================================================
 * SANS SPRING (Servlet HttpServlet) :
 * =========================================================
 *   Ce contrôleur serait divisé en plusieurs Servlets déclarées dans web.xml :
 *
 *     @WebServlet("/api/auth/register")
 *     public class RegisterServlet extends HttpServlet {
 *         @Override
 *         protected void doPost(HttpServletRequest req, HttpServletResponse resp)
 *                 throws ServletException, IOException {
 *             // Lecture du JSON manuel
 *             BufferedReader reader = req.getReader();
 *             StringBuilder body = new StringBuilder();
 *             String line;
 *             while ((line = reader.readLine()) != null) body.append(line);
 *             JSONObject json = new JSONObject(body.toString());
 *             String username = json.getString("username");
 *             // Validation manuelle, insertion JDBC, réponse JSON manuelle...
 *         }
 *     }
 *
 *   Chaque endpoint = une Servlet = un fichier Java = mapping dans web.xml.
 *
 * =========================================================
 * AVEC SPRING MVC :
 * =========================================================
 *   @RestController  → combine @Controller + @ResponseBody (sérialisation JSON auto)
 *   @RequestMapping  → un seul préfixe pour toutes les routes du contrôleur
 *   @PostMapping     → mappe POST /api/auth/register automatiquement
 *   @RequestBody     → Jackson désérialise le JSON en objet Java automatiquement
 *   @Valid           → Bean Validation déclenche les contraintes (@NotBlank, @Email...)
 *   @AuthenticationPrincipal → injecte l'utilisateur connecté depuis le SecurityContext
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. Un contrôleur = tous les endpoints d'un domaine fonctionnel
 *   2. Pas de web.xml : mapping déclaratif via annotations
 *   3. JSON auto-sérialisé/désérialisé par Jackson
 *   4. Validation automatique avec @Valid + messages d'erreur structurés
 *   5. Principal/Authentication injecté sans accès à HttpSession
 *   6. @ExceptionHandler pour gérer les erreurs en un seul endroit
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * Inscription d'un nouvel utilisateur.
     * Route publique (permitAll dans SecurityConfig).
     *
     * SANS SPRING :
     *   @WebServlet("/api/auth/register") + doPost() + JDBC INSERT manuel
     *   Validation : if/else manuels sur chaque champ
     *   Réponse : response.getWriter().write("{\"message\":\"...\"}")
     *
     * AVEC SPRING :
     *   @Valid → validation automatique, 400 Bad Request si contraintes non respectées
     *   authService.register() → logique métier séparée (principe SRP)
     *   ResponseEntity → contrôle précis du statut HTTP et du corps
     *
     * POST /api/auth/register
     * Body: { "username": "alice", "email": "alice@mail.com", "password": "secret123" }
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Connexion d'un utilisateur existant.
     * Route publique (permitAll dans SecurityConfig).
     *
     * SANS SPRING :
     *   SELECT * FROM users WHERE username = ?
     *   Comparaison SHA256 manuelle (sans sel → vulnérable)
     *   session.setAttribute("user", user) → cookie JSESSIONID envoyé
     *   Pas de JWT : la session est gérée côté serveur
     *
     * AVEC SPRING :
     *   AuthenticationManager (via AuthService) → vérification BCrypt automatique
     *   JWT généré → stateless, scalable, pas de session serveur
     *   Réponse HTTP 200 avec { token, username, role }
     *
     * POST /api/auth/login
     * Body: { "username": "alice", "password": "secret123" }
     */
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

    /**
     * Profil de l'utilisateur connecté.
     * Route protégée : requiert un token JWT valide.
     *
     * SANS SPRING :
     *   HttpSession session = request.getSession(false);
     *   if (session == null) { response.sendError(401); return; }
     *   String username = (String) session.getAttribute("username");
     *   User user = userDAO.findByUsername(username);
     *   // Sérialisation JSON manuelle
     *
     * AVEC SPRING :
     *   @AuthenticationPrincipal injecte directement l'objet User
     *   depuis le SecurityContextHolder (peuplé par JwtAuthFilter).
     *   Aucun accès à HttpSession ni à la base de données nécessaire.
     *
     * GET /api/auth/profile
     * Header: Authorization: Bearer <token>
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Non authentifié"));
        }
        return ResponseEntity.ok(Map.of(
                "username", currentUser.getUsername(),
                "email", currentUser.getEmail(),
                "role", currentUser.getRole().name(),
                "createdAt", currentUser.getCreatedAt() != null
                        ? currentUser.getCreatedAt().toString() : ""
        ));
    }

    /**
     * Liste de tous les utilisateurs — réservé aux administrateurs.
     *
     * SANS SPRING :
     *   Vérification manuelle dans la Servlet :
     *     String role = (String) session.getAttribute("role");
     *     if (!"ADMIN".equals(role)) { response.sendError(403); return; }
     *
     * AVEC SPRING :
     *   @PreAuthorize("hasRole('ADMIN')") → Spring Security intercepte la méthode
     *   via AOP et vérifie le rôle dans le SecurityContext AVANT d'entrer dans la méthode.
     *   Si l'utilisateur n'est pas ADMIN → 403 Forbidden automatique.
     *
     * GET /api/auth/admin/users
     * Header: Authorization: Bearer <token-admin>
     */
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * Gestionnaire d'exceptions global pour ce contrôleur.
     *
     * SANS SPRING : try/catch dans chaque Servlet, réponse JSON manuelle.
     * AVEC SPRING : @ExceptionHandler centralise la gestion d'erreurs.
     *
     * Note : pour une portée globale, utiliser @ControllerAdvice dans une classe dédiée.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE — Servlets séparées pour chaque endpoint d'authentification
 * =====================================================================
 *
 * En J2EE, chaque endpoint = une Servlet = un fichier Java + mapping web.xml.
 * Il n'y a pas de @RestController ni de routing centralisé.
 *
 * ── ProfileServlet (équivalent de GET /api/auth/profile) ─────────────
 *
 *   package com.example.servlet;
 *
 *   import com.example.dao.UserDAO;
 *   import com.example.model.User;
 *   import jakarta.servlet.http.*;
 *   import jakarta.servlet.annotation.WebServlet;
 *   import java.io.IOException;
 *
 *   @WebServlet("/api/auth/profile")
 *   public class ProfileServlet extends HttpServlet {
 *
 *       private UserDAO userDAO = new UserDAO();
 *
 *       @Override
 *       protected void doGet(HttpServletRequest request, HttpServletResponse response)
 *               throws IOException {
 *
 *           response.setContentType("application/json");
 *           response.setCharacterEncoding("UTF-8");
 *
 *           // Récupération de l'utilisateur courant — propagé par JwtAuthFilterJ2EE
 *           // Spring : @AuthenticationPrincipal User currentUser → injecté automatiquement
 *           User currentUser = (User) request.getAttribute("currentUser");
 *           if (currentUser == null) {
 *               response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *               response.getWriter().write("{\"error\":\"Non authentifié\"}");
 *               return;
 *           }
 *
 *           // Sérialisation JSON manuelle (Spring : Jackson sérialise automatiquement)
 *           String createdAt = currentUser.getCreatedAt() != null
 *               ? currentUser.getCreatedAt().toString() : "";
 *           response.setStatus(HttpServletResponse.SC_OK);
 *           response.getWriter().write(
 *               "{"
 *             + "\"username\":\""  + currentUser.getUsername() + "\","
 *             + "\"email\":\""     + currentUser.getEmail()    + "\","
 *             + "\"role\":\""      + currentUser.getRole()     + "\","
 *             + "\"createdAt\":\"" + createdAt                 + "\""
 *             + "}"
 *           );
 *       }
 *   }
 *
 * ── AdminUsersServlet (équivalent de GET /api/auth/admin/users) ───────
 *
 *   @WebServlet("/api/auth/admin/users")
 *   public class AdminUsersServlet extends HttpServlet {
 *
 *       private UserDAO userDAO = new UserDAO();
 *
 *       @Override
 *       protected void doGet(HttpServletRequest request, HttpServletResponse response)
 *               throws IOException {
 *
 *           response.setContentType("application/json");
 *           response.setCharacterEncoding("UTF-8");
 *
 *           // Vérification du rôle ADMIN (Spring : @PreAuthorize("hasRole('ADMIN')"))
 *           // Ce code doit être répété dans CHAQUE Servlet admin
 *           String role = (String) request.getAttribute("currentRole");
 *           if (!"ROLE_ADMIN".equals(role)) {
 *               response.setStatus(HttpServletResponse.SC_FORBIDDEN);
 *               response.getWriter().write("{\"error\":\"Accès refusé\"}");
 *               return;
 *           }
 *
 *           // Récupération de tous les utilisateurs
 *           // Spring : userRepository.findAll() — 1 ligne, Hibernate génère le SQL
 *           java.util.List<User> users = userDAO.findAll();
 *
 *           // Sérialisation JSON manuelle d'une liste
 *           // Spring : Jackson sérialise automatiquement List<User>
 *           StringBuilder sb = new StringBuilder("[");
 *           for (int i = 0; i < users.size(); i++) {
 *               User u = users.get(i);
 *               sb.append("{")
 *                 .append("\"id\":").append(u.getId()).append(",")
 *                 .append("\"username\":\"").append(u.getUsername()).append("\",")
 *                 .append("\"email\":\"").append(u.getEmail()).append("\",")
 *                 .append("\"role\":\"").append(u.getRole()).append("\"")
 *                 .append("}");
 *               if (i < users.size() - 1) sb.append(",");
 *           }
 *           sb.append("]");
 *           response.setStatus(HttpServletResponse.SC_OK);
 *           response.getWriter().write(sb.toString());
 *       }
 *   }
 *
 * ── Déclaration dans web.xml (obligatoire sans @WebServlet) ──────────
 *
 *   <servlet>
 *     <servlet-name>ProfileServlet</servlet-name>
 *     <servlet-class>com.example.servlet.ProfileServlet</servlet-class>
 *   </servlet>
 *   <servlet-mapping>
 *     <servlet-name>ProfileServlet</servlet-name>
 *     <url-pattern>/api/auth/profile</url-pattern>
 *   </servlet-mapping>
 *
 *   <servlet>
 *     <servlet-name>AdminUsersServlet</servlet-name>
 *     <servlet-class>com.example.servlet.AdminUsersServlet</servlet-class>
 *   </servlet>
 *   <servlet-mapping>
 *     <servlet-name>AdminUsersServlet</servlet-name>
 *     <url-pattern>/api/auth/admin/users</url-pattern>
 *   </servlet-mapping>
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring AuthController   : 1 classe, 5 endpoints, routing @GetMapping/@PostMapping
 *   J2EE                    : 5 classes Servlet séparées + 5 entrées web.xml
 *   @AuthenticationPrincipal: injection automatique depuis SecurityContext
 *   J2EE currentUser        : request.getAttribute("currentUser") — propagation manuelle
 *   @PreAuthorize           : AOP — vérification avant l'entrée dans la méthode
 *   J2EE ADMIN check        : if/else répété dans chaque Servlet admin
 *   Jackson Spring          : sérialisation JSON automatique de n'importe quel objet
 *   J2EE                    : StringBuilder ou JSONObject — manuel, verbeux, fragile
 * =====================================================================
 */

