package com.example.game_portal.repository;

import com.example.game_portal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}

/*
 * Without Spring, you would write a DAO class with raw JDBC for every operation.
 * Here is what findByUsername() looks like without Spring:
 *
 *   public Optional<User> findByUsername(String username) {
 *       String sql = "SELECT * FROM users WHERE username = ?";
 *       try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
 *            PreparedStatement ps = conn.prepareStatement(sql)) {
 *           ps.setString(1, username);
 *           ResultSet rs = ps.executeQuery();
 *           if (rs.next()) {
 *               User u = new User();
 *               u.setId(rs.getLong("id"));
 *               u.setUsername(rs.getString("username"));
 *               u.setEmail(rs.getString("email"));
 *               u.setPassword(rs.getString("password"));
 *               return Optional.of(u);
 *           }
 *       } catch (SQLException e) { throw new RuntimeException(e); }
 *       return Optional.empty();
 *   }
 *
 * Spring Data JPA generates this SQL automatically just from the method name —
 * no SQL, no connection management, no ResultSet mapping needed.
 */
