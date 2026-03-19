import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html' // make sure this matches your HTML file name!
})
export class RegisterComponent {
  username = '';
  password = '';
  message = '';

  constructor(private authService: AuthService) {}

  showDescription = false;
  toggleDescription() {
    this.showDescription = !this.showDescription;
  }

  onSubmit() {
    const newUser = { username: this.username, password: this.password };
    
    this.authService.register(newUser).subscribe({
      next: (res) => this.message = "Player Created! You can now login.",
      error: (err) => this.message = "Error: Username might already be taken."
    });
  }
}