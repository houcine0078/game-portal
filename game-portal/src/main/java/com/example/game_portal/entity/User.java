package com.example.game_portal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entité JPA représentant un utilisateur du portail.
 *
 * =========================================================
 * SANS SPRING (JDBC pur) :
 * =========================================================
 *   Il n'y a pas d'annotation @Entity ni de mapping automatique.
 *   On crée la table manuellement avec un script SQL :
 *
 *     CREATE TABLE users (
 *       id         BIGINT AUTO_INCREMENT PRIMARY KEY,
 *       username   VARCHAR(50)  UNIQUE NOT NULL,
 *       email      VARCHAR(100) UNIQUE NOT NULL,
 *       password   VARCHAR(255) NOT NULL,
 *       role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
 *       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
 *     );
 *
 *   La lecture d'un utilisateur ressemble à ceci :
 *
 *     Connection conn = DriverManager.getConnection(url, user, pass);
 *     PreparedStatement ps = conn.prepareStatement(
 *         "SELECT * FROM users WHERE username = ?");
 *     ps.setString(1, username);
 *     ResultSet rs = ps.executeQuery();
 *     User u = null;
 *     if (rs.next()) {
 *         u = new User();
 *         u.setId(rs.getLong("id"));
 *         u.setUsername(rs.getString("username"));
 *         u.setPassword(rs.getString("password"));
 *         u.setRole(rs.getString("role"));
 *         // ...mapping manuel colonne par colonne
 *     }
 *     rs.close(); ps.close(); conn.close(); // gestion manuelle des ressources
 *
 * =========================================================
 * AVEC SPRING DATA JPA :
 * =========================================================
 *   @Entity  →  Hibernate génère et maintient la table (ddl-auto=update)
 *   @Column  →  contraintes DDL (unique, nullable, length) générées automatiquement
 *   Le mapping ResultSet → objet est géré par Hibernate : zéro code de mapping.
 *   UserRepository.findByUsername() génère le SQL automatiquement.
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. Aucun SQL de création de table à écrire (ddl-auto=update)
 *   2. Pas de mapping ResultSet → objet à maintenir
 *   3. Transactions automatiques via @Transactional (pas de conn.commit())
 *   4. Lombok (@Data, @Builder) élimine les getters/setters/constructeurs
 *   5. UserDetails intégré → Spring Security reconnaît l'objet directement
 *
 * =========================================================
 * UserDetails (Spring Security) :
 * =========================================================
 *   SANS SPRING : la session HTTP stocke l'utilisateur manuellement :
 *     session.setAttribute("user", user);
 *     session.setAttribute("role", user.getRole());
 *   Puis chaque Servlet vérifie :
 *     if (session.getAttribute("user") == null) { response.sendRedirect("/login"); }
 *
 *   AVEC SPRING : cette classe implémente UserDetails. Spring Security
 *   charge l'utilisateur une seule fois (via UserDetailsService) et gère
 *   le SecurityContext pour toutes les requêtes suivantes.
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    /*
     * SANS JPA : id géré manuellement (AUTO_INCREMENT SQL + getGeneratedKeys()).
     * AVEC JPA  : @GeneratedValue délègue la génération à la base de données.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * unique=true → Hibernate ajoute la contrainte UNIQUE dans le DDL généré.
     * SANS SPRING : contrainte écrite manuellement dans le script SQL CREATE TABLE.
     */
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    /*
     * Le mot de passe est TOUJOURS stocké haché (BCrypt).
     * SANS SPRING SECURITY : hachage via MessageDigest (MD5/SHA) sans salage
     *   → vulnérable aux attaques par tables arc-en-ciel.
     * AVEC SPRING : BCryptPasswordEncoder génère un sel aléatoire à chaque hachage,
     *   rendant chaque hash unique même pour des mots de passe identiques.
     */
    @Column(nullable = false)
    private String password;

    /*
     * @Enumerated(STRING) : stocké comme "USER" ou "ADMIN" (lisible en base).
     * SANS SPRING : colonne VARCHAR comparée via "ADMIN".equals(role).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    /*
     * @CreationTimestamp : Hibernate renseigne ce champ automatiquement à l'INSERT.
     * SANS SPRING : NOW() dans le SQL INSERT ou new Date() côté Java.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // =========================================================
    // Implémentation UserDetails — contrat Spring Security
    // =========================================================
    // SANS SPRING : ces informations seraient stockées en session HTTP
    //   et vérifiées manuellement dans chaque Servlet/Filtre.
    // AVEC SPRING : Spring Security appelle ces méthodes automatiquement
    //   à chaque requête via le SecurityContextHolder.

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Le préfixe "ROLE_" est requis par Spring Security pour hasRole()
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE / SERVLET — Sans Spring, Sans JPA, Sans Lombok
 * =====================================================================
 *
 * 1. SCRIPT SQL DE CRÉATION DE TABLE (init.sql — exécuté manuellement)
 * ---------------------------------------------------------------------
 *
 *   CREATE TABLE users (
 *     id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
 *     username   VARCHAR(50)  UNIQUE NOT NULL,
 *     email      VARCHAR(100) UNIQUE NOT NULL,
 *     password   VARCHAR(255) NOT NULL,
 *     role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
 *     created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
 *   );
 *
 * 2. POJO JAVA SIMPLE (sans annotations JPA ni Lombok)
 * ---------------------------------------------------------------------
 *
 *   package com.example.model;
 *
 *   import java.time.LocalDateTime;
 *
 *   public class User {
 *       private Long          id;
 *       private String        username;
 *       private String        email;
 *       private String        password;
 *       private String        role;         // "USER" ou "ADMIN" — chaîne brute
 *       private LocalDateTime createdAt;
 *
 *       // ---- Constructeurs ----
 *       public User() {}
 *
 *       public User(String username, String email, String password, String role) {
 *           this.username  = username;
 *           this.email     = email;
 *           this.password  = password;
 *           this.role      = role;
 *           this.createdAt = LocalDateTime.now();
 *       }
 *
 *       // ---- Getters / Setters (écrits à la main — Lombok les génère) ----
 *       public Long          getId()          { return id; }
 *       public void          setId(Long id)   { this.id = id; }
 *       public String        getUsername()    { return username; }
 *       public void          setUsername(String username) { this.username = username; }
 *       public String        getEmail()       { return email; }
 *       public void          setEmail(String email) { this.email = email; }
 *       public String        getPassword()    { return password; }
 *       public void          setPassword(String password) { this.password = password; }
 *       public String        getRole()        { return role; }
 *       public void          setRole(String role) { this.role = role; }
 *       public LocalDateTime getCreatedAt()   { return createdAt; }
 *       public void          setCreatedAt(LocalDateTime dt) { this.createdAt = dt; }
 *   }
 *
 * 3. MAPPING ResultSet → User DANS LE DAO (à répéter pour chaque requête)
 * ---------------------------------------------------------------------
 *
 *   // Méthode utilitaire dans UserDAO :
 *   private User mapRow(ResultSet rs) throws SQLException {
 *       User u = new User();
 *       u.setId(rs.getLong("id"));
 *       u.setUsername(rs.getString("username"));
 *       u.setEmail(rs.getString("email"));
 *       u.setPassword(rs.getString("password"));
 *       u.setRole(rs.getString("role"));
 *       Timestamp ts = rs.getTimestamp("created_at");
 *       if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
 *       return u;
 *   }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring JPA : @Entity + @Column → table créée automatiquement, mapping auto
 *   J2EE       : script SQL manuel + POJO sans annotations + mapping ResultSet ligne par ligne
 *   Lombok     : génère constructeurs/getters/setters → sans lui, ~40 lignes de boilerplate
 * =====================================================================
 */
