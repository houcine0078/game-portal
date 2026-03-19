package com.example.game_portal.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data // Lombok automatically creates getters and setters!
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;
}