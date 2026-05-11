import { Routes } from '@angular/router';
import { LoginComponent } from './components/auth/login/login';
import { RegisterComponent } from './components/auth/register/register';
import { CatalogComponent } from './components/catalog/catalog';
import { MemoryMatchComponent } from './components/games/memory-match/memory-match';
import { TypingTestComponent } from './components/games/typing-test/typing-test';
import { LeaderboardComponent } from './components/leaderboard/leaderboard';
import { HistoryComponent } from './components/history/history';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
    { path: 'login', component: LoginComponent },
    { path: 'register', component: RegisterComponent },
    { path: '', redirectTo: '/login', pathMatch: 'full' },
    { path: 'catalog', component: CatalogComponent, canActivate: [authGuard] },
    { path: 'memory-match', component: MemoryMatchComponent, canActivate: [authGuard] },
    { path: 'typing-test', component: TypingTestComponent, canActivate: [authGuard] },
    { path: 'leaderboard', component: LeaderboardComponent, canActivate: [authGuard] },
    { path: 'history', component: HistoryComponent, canActivate: [authGuard] },
    { path: '**', redirectTo: '/login' }
];
