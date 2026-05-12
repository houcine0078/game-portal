package com.example.game_portal.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire d'exceptions global pour toute l'application.
 *
 * =========================================================
 * SANS SPRING :
 * =========================================================
 *   Chaque Servlet gère ses propres exceptions avec try/catch :
 *
 *     try {
 *         // logique métier
 *     } catch (ValidationException e) {
 *         response.setStatus(400);
 *         response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
 *     } catch (AuthException e) {
 *         response.setStatus(401);
 *         response.getWriter().write("{\"error\":\"Non authentifié\"}");
 *     } catch (Exception e) {
 *         response.setStatus(500);
 *         response.getWriter().write("{\"error\":\"Erreur interne\"}");
 *     }
 *
 *   Problèmes : code dupliqué dans toutes les Servlets, format de réponse incohérent,
 *   oubli d'un cas d'exception = réponse HTML non structurée au client.
 *
 * =========================================================
 * AVEC SPRING (@RestControllerAdvice) :
 * =========================================================
 *   @RestControllerAdvice intercepte toutes les exceptions non gérées
 *   dans les @RestController et retourne des réponses JSON structurées.
 *
 *   Flux : Exception levée dans Controller → Spring AOP → @ExceptionHandler ici
 *   → réponse JSON avec bon statut HTTP → client Angular.
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. Gestion centralisée : un seul endroit pour toutes les erreurs
 *   2. Format de réponse cohérent pour toute l'API
 *   3. Pas de try/catch dans les contrôleurs → code plus lisible
 *   4. @Valid déclenche MethodArgumentNotValidException → 400 avec détails automatique
 *   5. AccessDeniedException → 403 automatique (Spring Security)
 *   6. AuthenticationException → 401 automatique (Spring Security)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gère les erreurs de validation Bean (@Valid).
     *
     * SANS SPRING : if (username == null || username.isEmpty()) { ... } dans chaque Servlet
     * AVEC SPRING : @Valid lance cette exception, Spring retourne les messages d'erreur
     *
     * Exemple de réponse :
     *   { "username": "3 à 50 caractères requis", "email": "Format invalide" }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    /**
     * Gère l'accès refusé (rôle insuffisant).
     *
     * SANS SPRING : if (!"ADMIN".equals(role)) { response.sendError(403); }
     * AVEC SPRING : @PreAuthorize lance AccessDeniedException → 403 automatique
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Accès refusé : droits insuffisants"));
    }

    /**
     * Gère les erreurs d'authentification.
     *
     * SANS SPRING : if (session == null) { response.sendError(401); }
     * AVEC SPRING : AuthenticationException → 401 automatique
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(
            AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Authentification requise"));
    }

    /**
     * Gère les erreurs métier (utilisateur introuvable, doublon...).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Filet de sécurité pour toutes les autres exceptions non gérées.
     *
     * SANS SPRING : Tomcat retourne une page HTML d'erreur 500 illisible par le client JSON.
     * AVEC SPRING : réponse JSON structurée, log de l'erreur, pas d'exposition des stacktraces.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Une erreur interne est survenue"));
    }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE — Gestion des erreurs dispersée dans chaque Servlet
 * =====================================================================
 *
 * En J2EE, il n'existe pas de @ControllerAdvice ni de gestionnaire global.
 * Chaque Servlet gère ses propres erreurs avec try/catch + sendError().
 *
 * ── GESTION D'ERREURS DANS CHAQUE SERVLET (code dupliqué) ────────────
 *
 *   // Dans RegisterServlet.doPost() :
 *   try {
 *
 *       // --- Validation manuelle (Spring : @Valid lance MethodArgumentNotValidException) ---
 *       if (username.isEmpty()) {
 *           response.setStatus(HttpServletResponse.SC_BAD_REQUEST);           // 400
 *           response.setContentType("application/json");
 *           response.getWriter().write("{\"error\":\"Username obligatoire\"}");
 *           return;
 *       }
 *
 *       // --- Erreur métier (Spring : throw new IllegalArgumentException("...")) ---
 *       if (userDAO.existsByUsername(username)) {
 *           response.setStatus(HttpServletResponse.SC_BAD_REQUEST);           // 400
 *           response.getWriter().write("{\"error\":\"Username déjà pris\"}");
 *           return;
 *       }
 *
 *       // --- Erreur SQL (Spring : @Transactional + DataAccessException) ---
 *       userDAO.save(user); // peut lever SQLException
 *
 *   } catch (java.sql.SQLException e) {
 *       response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);     // 500
 *       response.setContentType("application/json");
 *       response.getWriter().write("{\"error\":\"Erreur base de données\"}");
 *       // En J2EE : la stacktrace peut fuiter si mal gérée !
 *       e.printStackTrace();
 *   } catch (Exception e) {
 *       response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
 *       response.getWriter().write("{\"error\":\"Erreur interne\"}");
 *   }
 *
 * ── GESTION DES 403 DANS CHAQUE SERVLET (Spring : @PreAuthorize) ─────
 *
 *   // Dans toute Servlet protégée par un rôle ADMIN :
 *   String role = (String) request.getAttribute("currentRole");
 *   if (!"ROLE_ADMIN".equals(role)) {
 *       response.setStatus(HttpServletResponse.SC_FORBIDDEN);                 // 403
 *       response.setContentType("application/json");
 *       response.getWriter().write("{\"error\":\"Accès refusé\"}");
 *       return;
 *   }
 *
 * ── GESTION DES 401 (Spring : ExceptionTranslationFilter automatique) ─
 *
 *   // Dans chaque Servlet ou filtre :
 *   HttpSession session = request.getSession(false);
 *   if (session == null || session.getAttribute("username") == null) {
 *       response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);              // 401
 *       response.setContentType("application/json");
 *       response.getWriter().write("{\"error\":\"Authentification requise\"}");
 *       return;
 *   }
 *
 * ── ERREUR 500 PAR DÉFAUT DE TOMCAT (HTML) — Spring retourne JSON ─────
 *
 *   <!-- web.xml — page d'erreur HTML par défaut de Tomcat (illisible par Angular) -->
 *   <error-page>
 *     <error-code>500</error-code>
 *     <location>/error500.jsp</location>  <!-- page HTML, pas JSON -->
 *   </error-page>
 *   <error-page>
 *     <error-code>404</error-code>
 *     <location>/error404.jsp</location>
 *   </error-page>
 *
 *   <!-- Pour retourner du JSON en cas d'erreur, il faut une ErrorServlet : -->
 *   @WebServlet("/error")
 *   public class ErrorServlet extends HttpServlet {
 *       protected void service(HttpServletRequest req, HttpServletResponse resp)
 *               throws IOException {
 *           Integer code = (Integer) req.getAttribute("jakarta.servlet.error.status_code");
 *           String message = (String) req.getAttribute("jakarta.servlet.error.message");
 *           resp.setContentType("application/json");
 *           resp.setStatus(code != null ? code : 500);
 *           resp.getWriter().write("{\"error\":\"" + (message != null ? message : "Erreur") + "\"}");
 *       }
 *   }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring @ControllerAdvice : 1 classe → gère TOUS les types d'erreurs, format JSON uniforme
 *   J2EE                     : try/catch répété dans chaque Servlet (~10-20 lignes par Servlet)
 *                              Risque d'incohérence dans les formats de réponse d'erreur
 *   @Valid Spring             : MethodArgumentNotValidException → 400 avec détails par champ
 *   J2EE                     : if/else manuels, message d'erreur écrit à la main
 *   Spring 500 par défaut     : JSON structuré (GlobalExceptionHandler)
 *   Tomcat 500 par défaut     : page HTML avec stacktrace → fuite d'information !
 * =====================================================================
 */
