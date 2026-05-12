package com.example.game_portal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la requête de connexion.
 *
 * =========================================================
 * SANS SPRING :
 * =========================================================
 *   Dans une Servlet de login, la récupération des credentials se fait ainsi :
 *
 *     // LoginServlet.doPost() — authentification par session HTTP
 *     String username = request.getParameter("username");
 *     String password = request.getParameter("password");
 *
 *     User user = userDAO.findByUsername(username);
 *     // Vérification du mot de passe haché (MD5/SHA sans sel = mauvaise pratique)
 *     String hashedInput = DigestUtils.md5Hex(password);
 *     if (user != null && hashedInput.equals(user.getPassword())) {
 *         HttpSession session = request.getSession();
 *         session.setAttribute("user", user);        // stockage en session
 *         session.setAttribute("role", user.getRole());
 *         session.setMaxInactiveInterval(30 * 60);   // 30 minutes
 *         response.sendRedirect("/dashboard");
 *     } else {
 *         request.setAttribute("error", "Identifiants invalides");
 *         request.getRequestDispatcher("/login.jsp").forward(request, response);
 *     }
 *
 * =========================================================
 * AVEC SPRING (JWT stateless) :
 * =========================================================
 *   - @RequestBody LoginRequest  →  Jackson désérialise automatiquement
 *   - BCryptPasswordEncoder.matches()  →  comparaison sécurisée avec sel
 *   - Réponse : token JWT (pas de session serveur)
 *   - Le client envoie "Authorization: Bearer <token>" à chaque requête
 *
 * =========================================================
 * DIFFÉRENCES CLÉS session vs JWT :
 * =========================================================
 *   Session (sans Spring) :
 *     + Simple à implémenter
 *     - État serveur → problème de scalabilité horizontale
 *     - Vulnerable CSRF (session cookie)
 *     - Délai d'expiration côté serveur à gérer
 *
 *   JWT (avec Spring) :
 *     + Stateless → scalable (microservices, cloud)
 *     + Contient les informations (username, rôle) dans le token lui-même
 *     + Compatible CORS et APIs REST
 *     - Token non révocable avant expiration (sans blacklist)
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE / SERVLET — LoginServlet avec session HTTP
 * =====================================================================
 *
 * En J2EE classique, la connexion crée une session HTTP côté serveur.
 * Il n'y a pas de DTO ni de JWT : le JSESSIONID cookie identifie l'utilisateur.
 *
 *   import jakarta.servlet.*;
 *   import jakarta.servlet.http.*;
 *   import jakarta.servlet.annotation.WebServlet;
 *   import java.io.*;
 *   import java.security.MessageDigest;
 *   import java.util.Base64;
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
 *           // --- Lecture des credentials (formulaire HTML ou JSON) ---
 *           // Depuis un formulaire HTML :
 *           String username = request.getParameter("username");
 *           String password = request.getParameter("password");
 *
 *           // Depuis un corps JSON (API REST sans Spring) :
 *           // StringBuilder body = new StringBuilder();
 *           // BufferedReader reader = request.getReader();
 *           // String line;
 *           // while ((line = reader.readLine()) != null) body.append(line);
 *           // org.json.JSONObject json = new org.json.JSONObject(body.toString());
 *           // username = json.getString("username");
 *           // password = json.getString("password");
 *
 *           // --- Validation basique ---
 *           if (username == null || username.isEmpty() ||
 *               password == null || password.isEmpty()) {
 *               response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *               response.getWriter().write("{\"error\":\"Champs obligatoires manquants\"}");
 *               return;
 *           }
 *
 *           // --- Recherche en base ---
 *           User user = userDAO.findByUsername(username).orElse(null);
 *           if (user == null) {
 *               response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *               response.getWriter().write("{\"error\":\"Identifiants invalides\"}");
 *               return;
 *           }
 *
 *           // --- Vérification du mot de passe (SHA-256 SANS sel — moins sécurisé) ---
 *           // Spring utilise BCrypt avec sel automatique
 *           try {
 *               MessageDigest md = MessageDigest.getInstance("SHA-256");
 *               String hashedInput = Base64.getEncoder().encodeToString(
 *                   md.digest(password.getBytes("UTF-8")));
 *               if (!hashedInput.equals(user.getPassword())) {
 *                   response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
 *                   response.getWriter().write("{\"error\":\"Identifiants invalides\"}");
 *                   return;
 *               }
 *           } catch (Exception e) {
 *               response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 *               return;
 *           }
 *
 *           // --- Création de la session HTTP (stateful — stockage côté serveur) ---
 *           // Spring JWT : stateless — token envoyé au client, rien stocké serveur
 *           HttpSession session = request.getSession(true);      // crée la session
 *           session.setAttribute("username", user.getUsername());
 *           session.setAttribute("role",     user.getRole());
 *           session.setMaxInactiveInterval(30 * 60);             // expire après 30 min
 *
 *           // --- Réponse ---
 *           // Avec session : pas de token à retourner, le cookie JSESSIONID est auto
 *           // Ici on retourne le username pour info
 *           response.setStatus(HttpServletResponse.SC_OK);
 *           response.getWriter().write(
 *               "{\"username\":\"" + user.getUsername() + "\"," +
 *               "\"role\":\""     + user.getRole()     + "\"}"
 *           );
 *       }
 *   }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring JWT  : AuthenticationManager.authenticate() → BCrypt → JWT token (stateless)
 *   J2EE Session: MessageDigest SHA-256 manual → HttpSession (stateful, non scalable)
 *   Scalabilité : Session = sticky session ou Redis nécessaire ; JWT = n'importe quel serveur
 * =====================================================================
 */
