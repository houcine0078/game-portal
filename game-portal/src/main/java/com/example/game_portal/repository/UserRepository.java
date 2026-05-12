package com.example.game_portal.repository;

import com.example.game_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository Spring Data JPA pour l'entité User.
 *
 * =========================================================
 * SANS SPRING (DAO JDBC manuel) :
 * =========================================================
 *   On écrit une classe UserDAO avec toutes les requêtes SQL :
 *
 *     public class UserDAO {
 *         private Connection getConnection() throws SQLException {
 *             return DriverManager.getConnection(URL, USER, PASS);
 *         }
 *
 *         public Optional<User> findByUsername(String username) throws SQLException {
 *             String sql = "SELECT * FROM users WHERE username = ?";
 *             try (Connection c = getConnection();
 *                  PreparedStatement ps = c.prepareStatement(sql)) {
 *                 ps.setString(1, username);
 *                 ResultSet rs = ps.executeQuery();
 *                 if (rs.next()) {
 *                     User u = new User();
 *                     u.setId(rs.getLong("id"));
 *                     u.setUsername(rs.getString("username"));
 *                     u.setPassword(rs.getString("password"));
 *                     u.setRole(Role.valueOf(rs.getString("role")));
 *                     return Optional.of(u);
 *                 }
 *                 return Optional.empty();
 *             }
 *         }
 *
 *         public void save(User u) throws SQLException {
 *             String sql = "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)";
 *             try (Connection c = getConnection();
 *                  PreparedStatement ps = c.prepareStatement(sql)) {
 *                 ps.setString(1, u.getUsername());
 *                 ps.setString(2, u.getEmail());
 *                 ps.setString(3, u.getPassword());
 *                 ps.setString(4, u.getRole().name());
 *                 ps.executeUpdate();
 *             }
 *         }
 *         // + findAll(), findById(), update(), delete()... — tout à la main
 *     }
 *
 * =========================================================
 * AVEC SPRING DATA JPA :
 * =========================================================
 *   - JpaRepository<User, Long> fournit save(), findById(), findAll(),
 *     deleteById(), count(), existsById()... sans écrire une ligne de SQL.
 *   - Les méthodes dérivées (findByUsername, findByEmail) sont générées
 *     automatiquement par Spring à partir du nom de la méthode.
 *   - Toute la gestion des connexions et transactions est transparente.
 *
 * =========================================================
 * AVANTAGES SPRING :
 * =========================================================
 *   1. Zéro SQL pour les opérations CRUD standard
 *   2. Requêtes dérivées du nom de méthode (findBy*, existsBy*, deleteBy*)
 *   3. @Query pour les cas complexes (JPQL ou SQL natif)
 *   4. Pagination et tri inclus (Pageable, Sort)
 *   5. Pool de connexions HikariCP géré automatiquement
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Recherche un utilisateur par son nom d'utilisateur.
     * SQL généré : SELECT * FROM users WHERE username = ?
     */
    Optional<User> findByUsername(String username);

    /**
     * Recherche un utilisateur par son adresse email.
     * SQL généré : SELECT * FROM users WHERE email = ?
     */
    Optional<User> findByEmail(String email);

    /**
     * Vérifie si un nom d'utilisateur est déjà pris.
     * SQL généré : SELECT COUNT(*) > 0 FROM users WHERE username = ?
     */
    boolean existsByUsername(String username);

    /**
     * Vérifie si un email est déjà enregistré.
     * SQL généré : SELECT COUNT(*) > 0 FROM users WHERE email = ?
     */
    boolean existsByEmail(String email);
}

/*
 * =====================================================================
 * ÉQUIVALENT J2EE / JDBC — UserDAO sans Spring Data JPA
 * =====================================================================
 *
 *   package com.example.dao;
 *
 *   import com.example.model.User;
 *   import com.example.model.RoleConstants;
 *   import java.sql.*;
 *   import java.util.ArrayList;
 *   import java.util.List;
 *   import java.util.Optional;
 *
 *   public class UserDAO {
 *
 *       // Paramètres de connexion codés en dur (ou lus depuis un fichier .properties)
 *       private static final String URL  = "jdbc:mysql://localhost:3306/game_portal";
 *       private static final String USER = "root";
 *       private static final String PASS = "root";
 *
 *       static {
 *           try {
 *               // Enregistrement du driver JDBC (obligatoire avant Java 6)
 *               Class.forName("com.mysql.cj.jdbc.Driver");
 *           } catch (ClassNotFoundException e) {
 *               throw new RuntimeException("Driver MySQL introuvable", e);
 *           }
 *       }
 *
 *       private Connection getConnection() throws SQLException {
 *           // Nouvelle connexion à chaque appel — pas de pool → risque d'épuisement
 *           // Spring utilise HikariCP (pool) pour réutiliser les connexions
 *           return DriverManager.getConnection(URL, USER, PASS);
 *       }
 *
 *       // Équivalent de findByUsername()
 *       public Optional<User> findByUsername(String username) {
 *           String sql = "SELECT * FROM users WHERE username = ?";
 *           try (Connection conn = getConnection();
 *                PreparedStatement ps = conn.prepareStatement(sql)) {
 *               ps.setString(1, username);
 *               try (ResultSet rs = ps.executeQuery()) {
 *                   if (rs.next()) return Optional.of(mapRow(rs));
 *               }
 *           } catch (SQLException e) {
 *               throw new RuntimeException("Erreur findByUsername", e);
 *           }
 *           return Optional.empty();
 *       }
 *
 *       // Équivalent de findByEmail()
 *       public Optional<User> findByEmail(String email) {
 *           String sql = "SELECT * FROM users WHERE email = ?";
 *           try (Connection conn = getConnection();
 *                PreparedStatement ps = conn.prepareStatement(sql)) {
 *               ps.setString(1, email);
 *               try (ResultSet rs = ps.executeQuery()) {
 *                   if (rs.next()) return Optional.of(mapRow(rs));
 *               }
 *           } catch (SQLException e) {
 *               throw new RuntimeException("Erreur findByEmail", e);
 *           }
 *           return Optional.empty();
 *       }
 *
 *       // Équivalent de existsByUsername()
 *       public boolean existsByUsername(String username) {
 *           String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
 *           try (Connection conn = getConnection();
 *                PreparedStatement ps = conn.prepareStatement(sql)) {
 *               ps.setString(1, username);
 *               try (ResultSet rs = ps.executeQuery()) {
 *                   return rs.next() && rs.getInt(1) > 0;
 *               }
 *           } catch (SQLException e) {
 *               throw new RuntimeException("Erreur existsByUsername", e);
 *           }
 *       }
 *
 *       // Équivalent de existsByEmail()
 *       public boolean existsByEmail(String email) {
 *           String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
 *           try (Connection conn = getConnection();
 *                PreparedStatement ps = conn.prepareStatement(sql)) {
 *               ps.setString(1, email);
 *               try (ResultSet rs = ps.executeQuery()) {
 *                   return rs.next() && rs.getInt(1) > 0;
 *               }
 *           } catch (SQLException e) {
 *               throw new RuntimeException("Erreur existsByEmail", e);
 *           }
 *       }
 *
 *       // Équivalent de save() — INSERT ou UPDATE selon présence de l'id
 *       public void save(User user) {
 *           if (user.getId() == null) {
 *               String sql = "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)";
 *               try (Connection conn = getConnection();
 *                    PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
 *                   ps.setString(1, user.getUsername());
 *                   ps.setString(2, user.getEmail());
 *                   ps.setString(3, user.getPassword());
 *                   ps.setString(4, user.getRole());
 *                   ps.executeUpdate();
 *                   // Récupération de l'id auto-généré
 *                   try (ResultSet keys = ps.getGeneratedKeys()) {
 *                       if (keys.next()) user.setId(keys.getLong(1));
 *                   }
 *               } catch (SQLException e) {
 *                   throw new RuntimeException("Erreur INSERT user", e);
 *               }
 *           } else {
 *               String sql = "UPDATE users SET username=?, email=?, password=?, role=? WHERE id=?";
 *               try (Connection conn = getConnection();
 *                    PreparedStatement ps = conn.prepareStatement(sql)) {
 *                   ps.setString(1, user.getUsername());
 *                   ps.setString(2, user.getEmail());
 *                   ps.setString(3, user.getPassword());
 *                   ps.setString(4, user.getRole());
 *                   ps.setLong(5, user.getId());
 *                   ps.executeUpdate();
 *               } catch (SQLException e) {
 *                   throw new RuntimeException("Erreur UPDATE user", e);
 *               }
 *           }
 *       }
 *
 *       // Équivalent de findAll()
 *       public List<User> findAll() {
 *           String sql = "SELECT * FROM users";
 *           List<User> users = new ArrayList<>();
 *           try (Connection conn = getConnection();
 *                PreparedStatement ps = conn.prepareStatement(sql);
 *                ResultSet rs = ps.executeQuery()) {
 *               while (rs.next()) users.add(mapRow(rs));
 *           } catch (SQLException e) {
 *               throw new RuntimeException("Erreur findAll users", e);
 *           }
 *           return users;
 *       }
 *
 *       // Mapping ResultSet → User (répété dans chaque méthode sans ce helper)
 *       private User mapRow(ResultSet rs) throws SQLException {
 *           User u = new User();
 *           u.setId(rs.getLong("id"));
 *           u.setUsername(rs.getString("username"));
 *           u.setEmail(rs.getString("email"));
 *           u.setPassword(rs.getString("password"));
 *           u.setRole(rs.getString("role"));
 *           Timestamp ts = rs.getTimestamp("created_at");
 *           if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
 *           return u;
 *       }
 *   }
 *
 * RÉSUMÉ DES DIFFÉRENCES :
 *   Spring Data JPA : interface avec 4 méthodes → ~10 lignes
 *   J2EE JDBC       : classe avec les mêmes 4 méthodes → ~120 lignes
 *   Chaque méthode Spring génère automatiquement le SQL + le mapping + la gestion connexion
 * =====================================================================
 */
