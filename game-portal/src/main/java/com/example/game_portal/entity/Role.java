package com.example.game_portal.entity;

/**
 * Enumération des rôles utilisateur.
 *
 * SANS SPRING :
 *   Les rôles seraient stockés en base (table "roles" ou colonne "role" VARCHAR)
 *   et comparés manuellement dans chaque Servlet via :
 *     String role = (String) session.getAttribute("role");
 *     if (!"ADMIN".equals(role)) { response.sendError(403); return; }
 *
 * AVEC SPRING SECURITY :
 *   Les rôles sont préfixés "ROLE_" automatiquement.
 *   La vérification se fait de façon déclarative :
 *     @PreAuthorize("hasRole('ADMIN')")   → sur un contrôleur
 *     .hasRole("ADMIN")                  → dans SecurityConfig
 *
 * AVANTAGES SPRING :
 *   - Vérification centralisée dans SecurityConfig (une seule source de vérité)
 *   - Pas de code répété dans chaque route/Servlet
 *   - AOP via @PreAuthorize évite le couplage entre logique métier et sécurité
 */
public enum Role {
    USER,
    ADMIN
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE / SERVLET — Sans Spring, Sans Enum typé
 * =====================================================================
 *
 * En J2EE classique, les rôles sont soit des constantes String,
 * soit des entrées dans une table SQL séparée.
 *
 * APPROCHE 1 — Constantes statiques (la plus courante sans framework)
 * ---------------------------------------------------------------------
 *
 *   package com.example.model;
 *
 *   public class RoleConstants {
 *       public static final String USER  = "USER";
 *       public static final String ADMIN = "ADMIN";
 *
 *       // Vérification dans une Servlet :
 *       //   String role = (String) session.getAttribute("role");
 *       //   if (RoleConstants.ADMIN.equals(role)) { ... }
 *   }
 *
 * APPROCHE 2 — Table SQL séparée (gestion des droits plus fine)
 * ---------------------------------------------------------------------
 *
 *   CREATE TABLE roles (
 *     id   INT         AUTO_INCREMENT PRIMARY KEY,
 *     name VARCHAR(20) UNIQUE NOT NULL   -- 'USER', 'ADMIN'
 *   );
 *
 *   CREATE TABLE user_roles (
 *     user_id INT NOT NULL,
 *     role_id INT NOT NULL,
 *     PRIMARY KEY (user_id, role_id),
 *     FOREIGN KEY (user_id) REFERENCES users(id),
 *     FOREIGN KEY (role_id) REFERENCES roles(id)
 *   );
 *
 *   -- Vérification dans le DAO :
 *   SELECT r.name FROM roles r
 *   JOIN user_roles ur ON ur.role_id = r.id
 *   WHERE ur.user_id = ?
 *
 *   -- Vérification dans la Servlet :
 *   List<String> roles = userDAO.getRolesForUser(userId);
 *   if (!roles.contains("ADMIN")) {
 *       response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès refusé");
 *       return;
 *   }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring : @Enumerated(STRING) → Hibernate stocke "USER"/"ADMIN" directement
 *            @PreAuthorize("hasRole('ADMIN')") → vérification AOP transparente
 *   J2EE   : String brute en base + comparaison manuelle if/else dans chaque Servlet
 *            Aucune centralisation → risque d'oubli de vérification
 * =====================================================================
 */
