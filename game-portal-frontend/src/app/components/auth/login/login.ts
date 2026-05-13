import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  username = '';
  password = '';
  message  = '';
  showDescription = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  toggleDescription() {
    this.showDescription = !this.showDescription;
  }

  onSubmit() {
    const credentials = { username: this.username, password: this.password };

    this.authService.login(credentials).subscribe({
      next: (res: any) => {
        this.message = 'Login successful!';
        this.authService.saveToken(res.token);
        this.authService.saveUsername(res.username);
        this.authService.saveRole(res.role);
        this.cdr.detectChanges();
        setTimeout(() => this.router.navigate(['/catalog']), 800);
      },
      error: () => {
        this.message = 'Error: Invalid credentials';
        this.cdr.detectChanges();
      }
    });
  }
}