import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './admin.html'
})
export class AdminComponent implements OnInit {

  activeTab: 'users' | 'games' = 'users';

  // ─── Users ────────────────────────────────────────────────
  users: any[] = [];
  newUser = { username: '', email: '', password: '', role: 'USER' };
  createMsg    = '';
  createErrors: string[] = [];
  userActionMsg = '';

  // ─── Games ────────────────────────────────────────────────
  games: any[] = [];
  gameActionMsg = '';

  private api = 'http://localhost:8080/api/admin';

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadUsers();
    this.loadGames();
  }

  setTab(tab: 'users' | 'games'): void {
    this.activeTab = tab;
  }

  // ─── Users ────────────────────────────────────────────────

  loadUsers(): void {
    this.http.get<any[]>(`${this.api}/users`).subscribe({
      next:  (data) => { this.users = data; this.cdr.detectChanges(); },
      error: ()     => this.cdr.detectChanges()
    });
  }

  createUser(): void {
    this.createMsg    = '';
    this.createErrors = [];

    this.http.post<any>(`${this.api}/users`, this.newUser).subscribe({
      next: (res) => {
        this.createMsg = res.message ?? 'User created successfully.';
        this.newUser   = { username: '', email: '', password: '', role: 'USER' };
        this.loadUsers();
        this.cdr.detectChanges();
      },
      error: (err) => {
        const body = err?.error;
        this.createErrors = (body && typeof body === 'object')
          ? Object.values(body) as string[]
          : ['Failed to create user.'];
        this.cdr.detectChanges();
      }
    });
  }

  deleteUser(id: number, username: string): void {
    if (!confirm(`Delete user "${username}"? This cannot be undone.`)) return;
    this.userActionMsg = '';

    this.http.delete<any>(`${this.api}/users/${id}`).subscribe({
      next: (res) => {
        this.userActionMsg = res.message ?? `User "${username}" deleted.`;
        this.users = this.users.filter(u => u.id !== id);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.userActionMsg = err?.error?.error ?? 'Failed to delete user.';
        this.cdr.detectChanges();
      }
    });
  }

  // ─── Games ────────────────────────────────────────────────

  loadGames(): void {
    this.http.get<any[]>(`${this.api}/games`).subscribe({
      next:  (data) => { this.games = data; this.cdr.detectChanges(); },
      error: ()     => this.cdr.detectChanges()
    });
  }

  deleteGame(id: number, title: string): void {
    if (!confirm(`Delete game "${title}"?`)) return;
    this.gameActionMsg = '';

    this.http.delete<any>(`${this.api}/games/${id}`).subscribe({
      next: (res) => {
        this.gameActionMsg = res.message ?? `Game "${title}" deleted.`;
        this.games = this.games.filter(g => g.id !== id);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.gameActionMsg = err?.error?.error ?? 'Failed to delete game.';
        this.cdr.detectChanges();
      }
    });
  }

  // ─── Auth ─────────────────────────────────────────────────

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
