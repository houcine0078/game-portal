package com.example.game_portal.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    private String category;

    // This will hold the URL for the game's cover art/thumbnail
    private String thumbnailUrl;

    // This would be the link to the actual HTML5 game file or external link
    private String gameUrl;
}