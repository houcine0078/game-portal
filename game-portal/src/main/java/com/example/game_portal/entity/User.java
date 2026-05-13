package com.example.game_portal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @JsonIgnore @Override public boolean isAccountNonExpired()     { return true; }
    @JsonIgnore @Override public boolean isAccountNonLocked()      { return true; }
    @JsonIgnore @Override public boolean isCredentialsNonExpired() { return true; }
    @JsonIgnore @Override public boolean isEnabled()               { return true; }
}

/*
 * Without Spring, you would create this table in SQL manually and write a plain Java class:
 *
 *   CREATE TABLE users (
 *     id         BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     username   VARCHAR(50)  UNIQUE NOT NULL,
 *     email      VARCHAR(100) UNIQUE NOT NULL,
 *     password   VARCHAR(255) NOT NULL,
 *     role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
 *     created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
 *   );
 *
 *   public class User {
 *       private Long id;
 *       private String username, email, password, role;
 *       private LocalDateTime createdAt;
 *       // ~40 lines of getters and setters written by hand (Lombok's @Data generates these)
 *   }
 *
 * Spring creates the table automatically from @Entity + ddl-auto=update,
 * and Lombok generates all getters/setters/constructors with @Data and @Builder.
 * The UserDetails interface lets Spring Security recognise this object directly.
 */
