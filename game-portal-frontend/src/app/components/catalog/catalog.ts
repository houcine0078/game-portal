import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game'; 
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './catalog.html'
})
export class CatalogComponent implements OnInit {
  games: any[] = [];

  constructor(private gameService: GameService) {}

  // This runs automatically when the component loads
  ngOnInit(): void {
    this.gameService.getGames().subscribe({
      next: (data) => this.games = data,
      error: (err) => console.error("Failed to load games", err)
    });
  }
}