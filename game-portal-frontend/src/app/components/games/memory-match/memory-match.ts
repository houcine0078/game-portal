import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router'

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

  // 1. Inject ChangeDetectorRef here!
  constructor(private cdr: ChangeDetectorRef) {}

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
      console.log("Match found! ", card1.icon);
      card1.isMatched = true;
      card2.isMatched = true;
      this.matchesFound++;
      this.flippedCards = []; 
    } else {
      console.log("No match. Locking board for 1 second...");
      this.lockBoard = true;
      
      setTimeout(() => {
        console.log("Timer finished! Flipping cards back...");
        card1.isFlipped = false;
        card2.isFlipped = false;
        this.flippedCards = [];
        this.lockBoard = false;
        
        // 2. FORCE ANGULAR TO REDRAW THE SCREEN!
        this.cdr.detectChanges(); 
      }, 1000); 
    }
  }
}