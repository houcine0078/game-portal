import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router'; // Import Router here
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
  message = '';
  showDescription = false;

  // Inject both AuthService and Router into the constructor
  constructor(private authService: AuthService, private router: Router) {}
  
  toggleDescription() {
    this.showDescription = !this.showDescription;
  }

  onSubmit() {
    const credentials = { username: this.username, password: this.password };
    
    this.authService.login(credentials).subscribe({
      next: (res) => {
        this.message = "Success! " + res;
        
        // Navigate to the catalog page after a short 800ms delay 
        // so the user can actually see the success message
        setTimeout(() => {
          this.router.navigate(['/catalog']);
        }, 800);
      },
      error: (err) => {
        this.message = "Error: Invalid credentials";
      }
    });
  }
}