package com.example.game_portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse après authentification réussie.
 *
 * =========================================================
 * SANS SPRING :
 * =========================================================
 *   La réponse serait construite manuellement en JSON :
 *
 *     // Dans la Servlet de login (approche session) :
 *     // Pas de token — la session est implicite via le cookie JSESSIONID
 *     response.sendRedirect("/dashboard");
 *
 *     // Ou en API REST sans Spring, avec JSONObject :
 *     JSONObject json = new JSONObject();
 *     json.put("token", generatedToken);
 *     json.put("username", user.getUsername());
 *     json.put("role", user.getRole().toString());
 *     response.setContentType("application/json");
 *     response.setCharacterEncoding("UTF-8");
 *     response.getWriter().write(json.toString());
 *
 * =========================================================
 * AVEC SPRING :
 * =========================================================
 *   @RestController retourne l'objet Java directement.
 *   Jackson le sérialise automatiquement en JSON.
 *   Le Content-Type "application/json" est défini automatiquement.
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. Sérialisation JSON automatique (Jackson intégré)
 *   2. Structure typée → moins d'erreurs que HashMap<String, Object>
 *   3. @Builder permet une construction fluide et lisible
 *   4. Facilement extensible (ajouter un champ = une ligne)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String username;
    private String email;
    private String role;
    private String message;
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE / SERVLET — Construction manuelle de la réponse JSON
 * =====================================================================
 *
 * En J2EE, il n'existe pas de DTO de réponse sérialisé automatiquement.
 * La réponse JSON est construite manuellement dans la Servlet
 * via StringBuilder, JSONObject (org.json), ou Gson.
 *
 * MÉTHODE 1 — StringBuilder (sans bibliothèque externe)
 * ---------------------------------------------------------------------
 *
 *   // Dans LoginServlet.doPost() après authentification réussie :
 *   response.setContentType("application/json");
 *   response.setCharacterEncoding("UTF-8");
 *   response.setStatus(HttpServletResponse.SC_OK);
 *
 *   // Construction JSON manuelle — risque d'erreur de syntaxe, pas de typage
 *   String json = "{"
 *       + "\"token\":\""    + jwtToken          + "\","
 *       + "\"username\":\"" + user.getUsername() + "\","
 *       + "\"email\":\""    + user.getEmail()    + "\","
 *       + "\"role\":\""     + user.getRole()     + "\","
 *       + "\"message\":\"Connexion réussie\""
 *       + "}";
 *   response.getWriter().write(json);
 *
 * MÉTHODE 2 — org.json.JSONObject (plus sûre)
 * ---------------------------------------------------------------------
 *
 *   import org.json.JSONObject;
 *
 *   JSONObject jsonResponse = new JSONObject();
 *   jsonResponse.put("token",    jwtToken);
 *   jsonResponse.put("username", user.getUsername());
 *   jsonResponse.put("email",    user.getEmail());
 *   jsonResponse.put("role",     user.getRole());
 *   jsonResponse.put("message",  "Connexion réussie");
 *
 *   response.setContentType("application/json");
 *   response.setCharacterEncoding("UTF-8");
 *   response.getWriter().write(jsonResponse.toString());
 *
 * MÉTHODE 3 — Session HTTP (sans token JWT)
 * ---------------------------------------------------------------------
 *
 *   // Pas de réponse JSON avec token — le cookie JSESSIONID est implicite
 *   // Le serveur stocke l'état en mémoire, le client n'a rien à stocker
 *   HttpSession session = request.getSession(true);
 *   session.setAttribute("username", user.getUsername());
 *   session.setAttribute("role",     user.getRole());
 *   // Le navigateur reçoit automatiquement Set-Cookie: JSESSIONID=abc123
 *   response.sendRedirect("/dashboard"); // ou réponse JSON minimaliste
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring @RestController : retourner AuthResponse → Jackson sérialise en JSON auto
 *   J2EE                   : construction JSON manuelle → risque de bugs de syntaxe
 *   @Builder Spring        : construction fluide et typée → impossible en J2EE pur
 *   Content-Type           : Spring le set automatiquement ; J2EE : response.setContentType()
 * =====================================================================
 */
