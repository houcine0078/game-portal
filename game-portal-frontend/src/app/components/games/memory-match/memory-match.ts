import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth';
import { ScoreService } from '../../../services/score';

export interface Card {
  id: number;
  icon: string;
  isFlipped: boolean;
  isMatched: boolean;
}

@Component({
  selector: 'app-memory-match',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './memory-match.html',
  styleUrls: ['./memory-match.css']
})
export class MemoryMatchComponent implements OnInit {
  icons = ['🎮', '🎲', '🎯', '🎳', '🕹️', '🎰', '🧩', '🏆'];
  cards: Card[] = [];
  flippedCards: Card[] = [];
  lockBoard = false;
  matchesFound = 0;
  moves = 0;
  gameWon = false;
  scoreSaved = false;

  constructor(
    private cdr: ChangeDetectorRef,
    private authService: AuthService,
    private scoreService: ScoreService
  ) {}

  ngOnInit() {
    this.setupGame();
  }

  setupGame() {
    const pairedIcons = [...this.icons, ...this.icons];
    pairedIcons.sort(() => Math.random() - 0.5);

    this.cards = pairedIcons.map((icon, index) => ({
      id: index,
      icon: icon,
      isFlipped: false,
      isMatched: false
    }));

    this.flippedCards = [];
    this.matchesFound = 0;
    this.moves = 0;
    this.lockBoard = false;
    this.gameWon = false;
    this.scoreSaved = false;
  }

  flipCard(card: Card) {
    if (this.lockBoard || card.isFlipped || card.isMatched) return;

    card.isFlipped = true;
    this.flippedCards.push(card);

    if (this.flippedCards.length === 2) {
      this.moves++;
      this.checkForMatch();
    }
  }

  checkForMatch() {
    const [card1, card2] = this.flippedCards;

    if (card1.icon === card2.icon) {
      card1.isMatched = true;
      card2.isMatched = true;
      this.matchesFound++;
      this.flippedCards = [];

      if (this.matchesFound === 8 && !this.gameWon) {
        this.gameWon = true;
        this.saveScore();
      }
    } else {
      this.lockBoard = true;

      setTimeout(() => {
        card1.isFlipped = false;
        card2.isFlipped = false;
        this.flippedCards = [];
        this.lockBoard = false;
        this.cdr.detectChanges();
      }, 1000);
    }
  }

  saveScore(): void {
    const username = this.authService.getUsername();
    if (!username) return;
    this.scoreService.saveScore({ username, gameType: 'memory-match', score: this.moves })
      .subscribe({
        next: () => this.scoreSaved = true,
        error: () => {}
      });
  }
}
