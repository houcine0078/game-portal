import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html'
})
export class RegisterComponent {
  username       = '';
  email          = '';
  password       = '';
  successMessage = '';
  errors: string[] = [];
  showDescription  = false;

  constructor(
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  toggleDescription() {
    this.showDescription = !this.showDescription;
  }

  onSubmit() {
    this.errors         = [];
    this.successMessage = '';

    const newUser = { username: this.username, email: this.email, password: this.password };

    this.authService.register(newUser).subscribe({
      next: () => {
        this.successMessage = 'Player Created! You can now login.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        const body = err?.error;
        if (body && typeof body === 'object') {
          // Validation errors: { password: "...", username: "...", email: "..." }
          // Business errors:   { error: "Username already taken" }
          this.errors = Object.values(body) as string[];
        } else {
          this.errors = ['Registration failed. Please try again.'];
        }
        this.cdr.detectChanges();
      }
    });
  }
}