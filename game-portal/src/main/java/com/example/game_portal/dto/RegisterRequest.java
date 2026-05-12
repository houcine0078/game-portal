package com.example.game_portal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO (Data Transfer Object) pour la requête d'inscription.
 *
 * =========================================================
 * SANS SPRING :
 * =========================================================
 *   Les données d'inscription proviendraient d'un formulaire HTML (POST)
 *   ou d'un corps JSON lu manuellement dans une Servlet :
 *
 *     // Dans RegisterServlet.doPost() :
 *     String username = request.getParameter("username");  // formulaire HTML
 *     String email    = request.getParameter("email");
 *     String password = request.getParameter("password");
 *
 *     // Ou avec JSON brut (sans bibliothèque) :
 *     StringBuilder sb = new StringBuilder();
 *     BufferedReader reader = request.getReader();
 *     String line;
 *     while ((line = reader.readLine()) != null) sb.append(line);
 *     // Puis parsing manuel ou via JSONObject de org.json
 *
 *     // Validation manuelle :
 *     if (username == null || username.isEmpty()) {
 *         response.sendError(400, "Username requis");
 *         return;
 *     }
 *     if (!email.contains("@")) {
 *         response.sendError(400, "Email invalide");
 *         return;
 *     }
 *
 * =========================================================
 * AVEC SPRING :
 * =========================================================
 *   @RequestBody RegisterRequest req  →  Jackson désérialise le JSON automatiquement
 *   @Valid                           →  Bean Validation vérifie les contraintes
 *   @NotBlank, @Email, @Size        →  annotations de validation standard (JSR-380)
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. Désérialisation JSON automatique par Jackson (pas de parsing manuel)
 *   2. Validation déclarative via annotations (pas de if/else de validation)
 *   3. Messages d'erreur structurés automatiquement (BindingResult / @ExceptionHandler)
 *   4. Le DTO isole l'API de l'entité interne (on n'expose pas User directement)
 *   5. Typage fort : pas de NullPointerException sur request.getParameter()
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom doit contenir entre 3 et 50 caractères")
    private String username;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE / SERVLET — Lecture et validation manuelle des données
 * =====================================================================
 *
 * En J2EE, il n'existe pas de DTO avec validation automatique.
 * Les données sont lues depuis HttpServletRequest et validées à la main
 * directement dans la Servlet, sans classe dédiée.
 *
 * EXTRACTION ET VALIDATION DANS RegisterServlet.doPost() :
 * ---------------------------------------------------------------------
 *
 *   import jakarta.servlet.*;
 *   import jakarta.servlet.http.*;
 *   import jakarta.servlet.annotation.WebServlet;
 *   import java.io.*;
 *   import java.util.regex.Pattern;
 *
 *   @WebServlet("/api/auth/register")
 *   public class RegisterServlet extends HttpServlet {
 *
 *       private static final Pattern EMAIL_PATTERN =
 *           Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
 *
 *       @Override
 *       protected void doPost(HttpServletRequest request, HttpServletResponse response)
 *               throws ServletException, IOException {
 *
 *           response.setContentType("application/json");
 *           response.setCharacterEncoding("UTF-8");
 *
 *           // --- Lecture du corps JSON brut ---
 *           // Spring fait cela automatiquement avec @RequestBody
 *           StringBuilder body = new StringBuilder();
 *           try (BufferedReader reader = request.getReader()) {
 *               String line;
 *               while ((line = reader.readLine()) != null) body.append(line);
 *           }
 *
 *           // --- Parsing JSON manuel (avec org.json) ---
 *           // Spring utilise Jackson automatiquement
 *           String username, email, password;
 *           try {
 *               org.json.JSONObject json = new org.json.JSONObject(body.toString());
 *               username = json.optString("username", "").trim();
 *               email    = json.optString("email",    "").trim();
 *               password = json.optString("password", "").trim();
 *           } catch (org.json.JSONException e) {
 *               response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *               response.getWriter().write("{\"error\":\"JSON invalide\"}");
 *               return;
 *           }
 *
 *           // --- Validation manuelle (Spring fait cela avec @Valid + @NotBlank) ---
 *           if (username.isEmpty()) {
 *               response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *               response.getWriter().write("{\"error\":\"Le nom d'utilisateur est obligatoire\"}");
 *               return;
 *           }
 *           if (username.length() < 3 || username.length() > 50) {
 *               response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *               response.getWriter().write("{\"error\":\"Le nom doit contenir entre 3 et 50 caractères\"}");
 *               return;
 *           }
 *           if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
 *               response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *               response.getWriter().write("{\"error\":\"Format d'email invalide\"}");
 *               return;
 *           }
 *           if (password.length() < 6) {
 *               response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
 *               response.getWriter().write("{\"error\":\"Le mot de passe doit avoir au moins 6 caractères\"}");
 *               return;
 *           }
 *
 *           // --- Ici commence la logique d'insertion (voir RegisterServlet complet) ---
 *           // ...
 *       }
 *   }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring @RequestBody + @Valid : 3 lignes dans le contrôleur → validation + désérialisation auto
 *   J2EE                        : ~50 lignes de lecture/parsing/validation dans chaque Servlet
 *   Le DTO Spring centralise la structure ; J2EE répète ce code dans chaque Servlet concernée
 * =====================================================================
 */
