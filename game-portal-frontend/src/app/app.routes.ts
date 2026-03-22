import { Routes } from '@angular/router';
import { LoginComponent } from './components/auth/login/login';
import { RegisterComponent } from './components/auth/register/register';
import { CatalogComponent } from './components/catalog/catalog';
import { MemoryMatchComponent } from './components/games/memory-match/memory-match';

export const routes: Routes = [
    { path: 'login', component: LoginComponent },
    { path: 'register', component: RegisterComponent },
    { path: '', redirectTo: '/login', pathMatch: 'full' },
    { path: 'catalog', component: CatalogComponent },
    { path: 'memory-match', component: MemoryMatchComponent },
    { path: '**', redirectTo: '/login' }
];
