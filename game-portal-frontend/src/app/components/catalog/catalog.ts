import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameService } from '../../services/game';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth';

interface AvatarColor {
  id: string;
  bg: string;   // main color (border + text)
  dark: string; // deep background shade
}

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './catalog.html'
})
export class CatalogComponent implements OnInit {
  games: any[] = [];
  showProfile = false;
  showLogoutConfirm = false;
  userProfile: { username: string; createdAt: string | null } | null = null;

  readonly avatarColors: AvatarColor[] = [
    { id: 'blue',   bg: '#53a6eb', dark: '#0c2a42' },
    { id: 'purple', bg: '#a855f7', dark: '#2d1052' },
    { id: 'green',  bg: '#22c55e', dark: '#052e16' },
    { id: 'orange', bg: '#f97316', dark: '#431407' },
    { id: 'red',    bg: '#ef4444', dark: '#450a0a' },
    { id: 'pink',   bg: '#ec4899', dark: '#500724' },
    { id: 'yellow', bg: '#eab308', dark: '#422006' },
    { id: 'cyan',   bg: '#06b6d4', dark: '#083344' },
  ];

  selectedColor: AvatarColor = this.avatarColors[0];

  constructor(
    private gameService: GameService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const savedId = this.authService.getAvatarColor();
    this.selectedColor = this.avatarColors.find(c => c.id === savedId) ?? this.avatarColors[0];

    this.gameService.getGames().subscribe({
      next: (data) => { this.games = data; this.cdr.detectChanges(); },
      error: (err) => console.error('Failed to load games', err)
    });

    this.authService.getProfile().subscribe({
      next: (profile: any) => { this.userProfile = profile; this.cdr.detectChanges(); },
      error: () => {
        const username = this.authService.getUsername() ?? '';
        this.userProfile = { username, createdAt: null };
        this.cdr.detectChanges();
      }
    });
  }

  pickColor(color: AvatarColor): void {
    this.selectedColor = color;
    this.authService.saveAvatarColor(color.id);
  }

  toggleProfile(): void {
    this.showProfile = !this.showProfile;
  }

  closeProfile(): void {
    this.showProfile = false;
  }

  logout(): void {
    this.showProfile = false;
    this.showLogoutConfirm = true;
  }

  confirmLogout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  cancelLogout(): void {
    this.showLogoutConfirm = false;
  }

  get initials(): string {
    return this.userProfile?.username?.charAt(0).toUpperCase() ?? 'P';
  }
}
