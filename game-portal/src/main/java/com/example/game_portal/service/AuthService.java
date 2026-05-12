package com.example.game_portal.service;

import com.example.game_portal.dto.AuthResponse;
import com.example.game_portal.dto.LoginRequest;
import com.example.game_portal.dto.RegisterRequest;
import com.example.game_portal.entity.Role;
import com.example.game_portal.entity.User;
import com.example.game_portal.repository.UserRepository;
import com.example.game_portal.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service métier gérant l'inscription et la connexion des utilisateurs.
 *
 * =========================================================
 * SANS SPRING (logique dans la Servlet) :
 * =========================================================
 *   Toute cette logique serait dispersée dans des Servlets :
 *
 *   RegisterServlet.doPost() :
 *     1. Lire les paramètres : request.getParameter("username")
 *     2. Validation manuelle : if (username.isEmpty()) { ... }
 *     3. Hachage MD5/SHA sans sel : MessageDigest.getInstance("SHA-256")
 *     4. Insertion JDBC : PreparedStatement ps = conn.prepareStatement("INSERT...")
 *     5. Gestion exceptions SQL à la main : catch (SQLException e) { ... }
 *     6. Réponse : response.sendRedirect("/login") ou JSON manuel
 *
 *   LoginServlet.doPost() :
 *     1. Lire username/password
 *     2. SELECT FROM users WHERE username = ?
 *     3. Comparer hash manuellement : SHA256(input).equals(stored)
 *     4. Créer session : session.setAttribute("user", user)
 *     5. OU générer JWT manuellement et le retourner en JSON
 *
 * =========================================================
 * AVEC SPRING (Service + Repository + AuthenticationManager) :
 * =========================================================
 *   @Service  → bean Spring géré par le conteneur IoC
 *   @Transactional → transaction de base de données automatique
 *   AuthenticationManager → délègue la vérification username/password à Spring Security
 *   PasswordEncoder → BCrypt avec sel automatique
 *   JpaRepository → pas de SQL manuel
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. Séparation des responsabilités : Controller → Service → Repository
 *   2. @Transactional : rollback automatique en cas d'exception
 *   3. AuthenticationManager centralise la logique d'authentification
 *   4. BCryptPasswordEncoder : salage automatique, bien supérieur à SHA/MD5
 *   5. Injection de dépendances (@RequiredArgsConstructor) : testabilité
 *   6. Exceptions métier lancées proprement (IllegalArgumentException)
 *      et gérées par @ExceptionHandler global
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    /**
     * Inscription d'un nouvel utilisateur.
     *
     * SANS SPRING :
     *   SQL manuel : "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)"
     *   Hachage : MessageDigest.getInstance("SHA-256").digest(password.getBytes())
     *   Pas de transaction → si l'INSERT échoue à mi-chemin, état incohérent possible.
     *
     * AVEC SPRING :
     *   userRepository.save(user) → Hibernate génère l'INSERT automatiquement.
     *   @Transactional → si une exception est levée, la transaction est annulée.
     *   BCrypt → sel aléatoire par hachage, impossible de retrouver le mot de passe original.
     *
     * @param request contient username, email, password (validés par @Valid dans le contrôleur)
     * @return AuthResponse avec le token JWT et les infos de l'utilisateur créé
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Vérification unicité username
        // SANS SPRING : SELECT COUNT(*) FROM users WHERE username = ? → if (count > 0) error
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Ce nom d'utilisateur est déjà pris");
        }

        // Vérification unicité email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà enregistré");
        }

        // Construction de l'entité User
        // SANS SPRING : new User() + setters manuels + INSERT JDBC
        // AVEC SPRING  : @Builder rend la construction fluide et lisible
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                // Hachage BCrypt avec sel automatique
                // SANS SPRING : SHA-256 sans sel (vulnérable aux rainbow tables)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // rôle par défaut
                .build();

        // Sauvegarde en base — INSERT généré par Hibernate
        // SANS SPRING : PreparedStatement + executeUpdate() + conn.commit()
        userRepository.save(user);

        // Génération du token JWT
        // SANS SPRING : création de session HTTP et stockage des attributs
        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message("Inscription réussie")
                .build();
    }

    /**
     * Connexion d'un utilisateur existant.
     *
     * SANS SPRING :
     *   1. SELECT * FROM users WHERE username = ?
     *   2. SHA256(inputPassword).equals(storedHash)
     *   3. session.setAttribute("user", user); session.setAttribute("role", role);
     *   4. response.sendRedirect("/dashboard");
     *
     * AVEC SPRING :
     *   authenticationManager.authenticate() gère tout :
     *     → appelle userDetailsService.loadUserByUsername()
     *     → vérifie le mot de passe avec BCryptPasswordEncoder.matches()
     *     → lève BadCredentialsException si échec (→ réponse 401 automatique)
     *
     * @param request contient username et password
     * @return AuthResponse avec token JWT valide 24h
     */
    public AuthResponse login(LoginRequest request) {
        // AuthenticationManager vérifie username + password via DaoAuthenticationProvider
        // SANS SPRING : logique de vérification manuelle (requête SQL + comparaison hash)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Si on arrive ici, l'authentification a réussi
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        // Génération du token JWT contenant username et rôle
        // SANS SPRING : session.setAttribute("user", user); session.setAttribute("role", ...)
        String token = jwtUtil.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .message("Connexion réussie")
                .build();
    }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE — RegisterServlet + LoginServlet (sans Service, sans Spring)
 * =====================================================================
 *
 * En J2EE, il n'y a pas de @Service ni d'injection de dépendances.
 * La logique métier de register() et login() est directement dans les Servlets.
 *
 * ── RegisterServlet (équivalent de AuthService.register()) ───────────
 *
 *   package com.example.servlet;
 *
 *   import com.example.dao.UserDAO;
 *   import com.example.model.User;
 *   import jakarta.servlet.*;
 *   import jakarta.servlet.http.*;
 *   import jakarta.servlet.annotation.WebServlet;
 *   import java.io.*;
 *   import java.security.MessageDigest;
 *   import java.util.Base64;
 *
 *   @WebServlet("/api/auth/register")
 *   public class RegisterServlet extends HttpServlet {
 *
 *       private UserDAO userDAO = new UserDAO();  // instanciation manuelle (pas d'injection)
 *
 *       @Override
 *       protected void doPost(HttpServletRequest request, HttpServletResponse response)
 *               throws ServletException, IOException {
 *
 *           response.setContentType("application/json");
 *           response.setCharacterEncoding("UTF-8");
 *
 *           // Lecture du JSON (Spring fait cela avec @RequestBody automatiquement)
 *           StringBuilder body = new StringBuilder();
 *           try (BufferedReader reader = request.getReader()) {
 *               String line;
 *               while ((line = reader.readLine()) != null) body.append(line);
 *           }
 *           org.json.JSONObject json = new org.json.JSONObject(body.toString());
 *           String username = json.getString("username").trim();
 *           String email    = json.getString("email").trim();
 *           String password = json.getString("password");
 *
 *           // Validation manuelle (Spring fait cela avec @Valid + @NotBlank)
 *           if (username.length() < 3 || username.length() > 50) {
 *               response.setStatus(400);
 *               response.getWriter().write("{\"error\":\"Nom invalide (3-50 chars)\"}");
 *               return;
 *           }
 *           if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
 *               response.setStatus(400);
 *               response.getWriter().write("{\"error\":\"Email invalide\"}");
 *               return;
 *           }
 *           if (password.length() < 6) {
 *               response.setStatus(400);
 *               response.getWriter().write("{\"error\":\"Mot de passe trop court\"}");
 *               return;
 *           }
 *
 *           // Vérification unicité (Spring : userRepository.existsByUsername())
 *           if (userDAO.existsByUsername(username)) {
 *               response.setStatus(400);
 *               response.getWriter().write("{\"error\":\"Nom déjà pris\"}");
 *               return;
 *           }
 *           if (userDAO.existsByEmail(email)) {
 *               response.setStatus(400);
 *               response.getWriter().write("{\"error\":\"Email déjà enregistré\"}");
 *               return;
 *           }
 *
 *           // Hachage SHA-256 SANS sel (Spring : BCryptPasswordEncoder.encode() avec sel)
 *           String hashedPassword;
 *           try {
 *               MessageDigest md = MessageDigest.getInstance("SHA-256");
 *               hashedPassword = Base64.getEncoder().encodeToString(
 *                   md.digest(password.getBytes("UTF-8")));
 *           } catch (Exception e) {
 *               response.setStatus(500);
 *               return;
 *           }
 *
 *           // Création et insertion manuelle (Spring : userRepository.save(user))
 *           User user = new User();
 *           user.setUsername(username);
 *           user.setEmail(email);
 *           user.setPassword(hashedPassword);
 *           user.setRole("USER");
 *           userDAO.save(user);
 *
 *           // Génération du JWT (même bibliothèque JJWT)
 *           String token = new JwtUtilJ2EE().generateToken(username, "ROLE_USER");
 *
 *           // Réponse JSON manuelle (Spring : retourner AuthResponse → Jackson sérialise)
 *           response.setStatus(201);
 *           response.getWriter().write(
 *               "{\"token\":\""    + token                + "\","
 *             + "\"username\":\"" + user.getUsername()   + "\","
 *             + "\"email\":\""    + user.getEmail()      + "\","
 *             + "\"role\":\""     + user.getRole()       + "\","
 *             + "\"message\":\"Inscription réussie\"}"
 *           );
 *       }
 *   }
 *
 * ── LoginServlet (équivalent de AuthService.login()) ─────────────────
 *
 *   @WebServlet("/api/auth/login")
 *   public class LoginServlet extends HttpServlet {
 *
 *       private UserDAO userDAO = new UserDAO();
 *
 *       @Override
 *       protected void doPost(HttpServletRequest request, HttpServletResponse response)
 *               throws ServletException, IOException {
 *
 *           response.setContentType("application/json");
 *           response.setCharacterEncoding("UTF-8");
 *
 *           StringBuilder body = new StringBuilder();
 *           try (BufferedReader reader = request.getReader()) {
 *               String line;
 *               while ((line = reader.readLine()) != null) body.append(line);
 *           }
 *           org.json.JSONObject json = new org.json.JSONObject(body.toString());
 *           String username = json.getString("username");
 *           String password = json.getString("password");
 *
 *           // Recherche en base (Spring : authenticationManager.authenticate() fait tout)
 *           User user = userDAO.findByUsername(username).orElse(null);
 *           if (user == null) {
 *               response.setStatus(401);
 *               response.getWriter().write("{\"error\":\"Identifiants invalides\"}");
 *               return;
 *           }
 *
 *           // Vérification SHA-256 (Spring : BCryptPasswordEncoder.matches())
 *           try {
 *               MessageDigest md = MessageDigest.getInstance("SHA-256");
 *               String hashedInput = Base64.getEncoder().encodeToString(
 *                   md.digest(password.getBytes("UTF-8")));
 *               if (!hashedInput.equals(user.getPassword())) {
 *                   response.setStatus(401);
 *                   response.getWriter().write("{\"error\":\"Identifiants invalides\"}");
 *                   return;
 *               }
 *           } catch (Exception e) {
 *               response.setStatus(500);
 *               return;
 *           }
 *
 *           // Génération JWT (Spring : jwtUtil.generateToken(user))
 *           String token = new JwtUtilJ2EE().generateToken(username, "ROLE_" + user.getRole());
 *
 *           // Réponse JSON manuelle
 *           response.setStatus(200);
 *           response.getWriter().write(
 *               "{\"token\":\""    + token              + "\","
 *             + "\"username\":\"" + user.getUsername() + "\","
 *             + "\"email\":\""    + user.getEmail()    + "\","
 *             + "\"role\":\""     + user.getRole()     + "\","
 *             + "\"message\":\"Connexion réussie\"}"
 *           );
 *       }
 *   }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring AuthService      : 2 méthodes claires (register, login), ~60 lignes avec commentaires
 *   J2EE RegisterServlet    : ~80 lignes de lecture, validation, hachage, SQL, JSON manuel
 *   J2EE LoginServlet       : ~60 lignes du même type
 *   @Transactional Spring   : rollback automatique si exception pendant register()
 *   J2EE                    : pas de transaction automatique → état incohérent possible
 *   BCrypt Spring           : résistant rainbow tables ; SHA-256 J2EE : vulnérable
 * =====================================================================
 */
