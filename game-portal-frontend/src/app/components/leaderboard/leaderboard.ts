import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ScoreService } from '../../services/score';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './leaderboard.html'
})
export class LeaderboardComponent implements OnInit {
  memoryScores: any[] = [];
  typingScores: any[] = [];
  loadingMemory = true;
  loadingTyping = true;
  currentUser = '';

  constructor(
    private scoreService: ScoreService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getUsername() ?? '';

    this.scoreService.getLeaderboard('memory-match').subscribe({
      next: (data) => {
        this.memoryScores = data ?? [];
        this.loadingMemory = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Memory leaderboard error:', err);
        this.loadingMemory = false;
        this.cdr.detectChanges();
      }
    });

    this.scoreService.getLeaderboard('typing-test').subscribe({
      next: (data) => {
        this.typingScores = data ?? [];
        this.loadingTyping = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Typing leaderboard error:', err);
        this.loadingTyping = false;
        this.cdr.detectChanges();
      }
    });
  }

  rankLabel(index: number): string {
    if (index === 0) return '🥇';
    if (index === 1) return '🥈';
    if (index === 2) return '🥉';
    return String(index + 1);
  }
}
