import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ScoreService } from '../../services/score';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './history.html'
})
export class HistoryComponent implements OnInit {
  allSessions: any[] = [];
  filter: 'all' | 'memory-match' | 'typing-test' = 'all';
  loading = true;

  constructor(private scoreService: ScoreService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.scoreService.getHistory().subscribe({
      next: (data) => {
        this.allSessions = data ?? [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  get sessions(): any[] {
    if (this.filter === 'all') return this.allSessions;
    return this.allSessions.filter(s => s.gameType === this.filter);
  }

  setFilter(f: 'all' | 'memory-match' | 'typing-test'): void {
    this.filter = f;
  }

  gameLabel(type: string): string {
    return type === 'memory-match' ? 'Memory Match' : 'Typing Test';
  }

  gameIcon(type: string): string {
    return type === 'memory-match' ? '🧩' : '⌨️';
  }

  statLabel(session: any): string {
    if (session.gameType === 'memory-match') return `${session.score} moves`;
    return `${session.score} WPM`;
  }
}
