import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

export interface Coordinate {
  x: number;
  y: number;
}

@Component({
  selector: 'app-snake',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './snake.html',
  styleUrls: ['./snake.css']
})
export class SnakeComponent implements OnInit, OnDestroy {
  gridSize = 20;
  board: Coordinate[] = [];
  
  snake: Coordinate[] = [{ x: 10, y: 10 }];
  food: Coordinate = { x: 5, y: 5 };
  direction: 'UP' | 'DOWN' | 'LEFT' | 'RIGHT' = 'UP';
  nextDirection: 'UP' | 'DOWN' | 'LEFT' | 'RIGHT' = 'UP'; 
  
  score = 0;
  gameOver = false;
  gameLoopInterval: any;
  speed = 150; 

  // NEW: Fixes for the movement bugs!
  isPaused = true;
  inputLocked = false; 

  constructor() {
    for (let y = 0; y < this.gridSize; y++) {
      for (let x = 0; x < this.gridSize; x++) {
        this.board.push({ x, y });
      }
    }
  }

  ngOnInit() {
    this.spawnFood();
    this.startGame();
  }

  ngOnDestroy() {
    if (this.gameLoopInterval) clearInterval(this.gameLoopInterval);
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent) {
    const key = event.key.toLowerCase();

    // Only react to standard movement keys
    if (!['arrowup', 'arrowdown', 'arrowleft', 'arrowright', 'w', 'a', 's', 'd'].includes(key)) {
      return;
    }

    event.preventDefault();

    if (this.gameOver) return;

    // Start the game loop on the very FIRST key press!
    if (this.isPaused) {
      this.isPaused = false;
      if (this.gameLoopInterval) clearInterval(this.gameLoopInterval);
      this.gameLoopInterval = setInterval(() => this.moveSnake(), this.speed);
    }

    // Stop them from rapid-firing keys and skipping turns
    if (this.inputLocked) return;

    let requestedDirection = null;

    // Allow turning anywhere if length is 1. Otherwise, prevent reversing.
    if ((key === 'arrowup' || key === 'w') && (this.snake.length === 1 || this.direction !== 'DOWN')) requestedDirection = 'UP';
    if ((key === 'arrowdown' || key === 's') && (this.snake.length === 1 || this.direction !== 'UP')) requestedDirection = 'DOWN';
    if ((key === 'arrowleft' || key === 'a') && (this.snake.length === 1 || this.direction !== 'RIGHT')) requestedDirection = 'LEFT';
    if ((key === 'arrowright' || key === 'd') && (this.snake.length === 1 || this.direction !== 'LEFT')) requestedDirection = 'RIGHT';

    // If a valid turn was requested, queue it up and lock the inputs until it moves
    if (requestedDirection) {
      this.nextDirection = requestedDirection as any;
      this.inputLocked = true; 
    }
  }

  startGame() {
    this.snake = [{ x: 10, y: 10 }];
    this.direction = 'UP';
    this.nextDirection = 'UP';
    this.score = 0;
    this.gameOver = false;
    this.speed = 150;
    
    // Reset our new variables
    this.isPaused = true; 
    this.inputLocked = false;
    
    this.spawnFood();
    if (this.gameLoopInterval) clearInterval(this.gameLoopInterval);
  }

  moveSnake() {
    if (this.gameOver || this.isPaused) return;

    this.direction = this.nextDirection;
    
    // The snake has officially moved, so unlock the keyboard for the next turn
    this.inputLocked = false; 

    const head = { ...this.snake[0] };

    switch (this.direction) {
      case 'UP': head.y -= 1; break;
      case 'DOWN': head.y += 1; break;
      case 'LEFT': head.x -= 1; break;
      case 'RIGHT': head.x += 1; break;
    }

    if (this.checkCollision(head)) {
      this.gameOver = true;
      clearInterval(this.gameLoopInterval);
      return;
    }

    this.snake.unshift(head);

    if (head.x === this.food.x && head.y === this.food.y) {
      this.score += 10;
      this.spawnFood();
      if (this.speed > 50) {
        this.speed -= 2;
        clearInterval(this.gameLoopInterval);
        this.gameLoopInterval = setInterval(() => this.moveSnake(), this.speed);
      }
    } else {
      this.snake.pop();
    }
  }

  checkCollision(head: Coordinate): boolean {
    if (head.x < 0 || head.x >= this.gridSize || head.y < 0 || head.y >= this.gridSize) return true;
    return this.snake.some(segment => segment.x === head.x && segment.y === head.y);
  }

  spawnFood() {
    let newFood: Coordinate;
    while (true) {
      newFood = {
        x: Math.floor(Math.random() * this.gridSize),
        y: Math.floor(Math.random() * this.gridSize)
      };
      if (!this.snake.some(segment => segment.x === newFood.x && segment.y === newFood.y)) break;
    }
    this.food = newFood;
  }

  isSnakeCell(x: number, y: number): boolean {
    return this.snake.some(segment => segment.x === x && segment.y === y);
  }

  isSnakeHead(x: number, y: number): boolean {
    return this.snake[0].x === x && this.snake[0].y === y;
  }

  isFoodCell(x: number, y: number): boolean {
    return this.food.x === x && this.food.y === y;
  }
}