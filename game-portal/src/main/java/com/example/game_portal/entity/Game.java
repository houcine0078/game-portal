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
    private String thumbnailUrl;
    private String gameUrl;
    private String iframeUrl;

    public void setIframeUrl(String iframeUrl) {
        this.iframeUrl = iframeUrl;
    }
}
