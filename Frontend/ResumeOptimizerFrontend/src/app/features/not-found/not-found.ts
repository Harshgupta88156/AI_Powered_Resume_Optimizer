import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TokenStorageService } from '../../core/services/token-storage.service';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './not-found.html',
  styleUrl: './not-found.css'
})
export class NotFound {

  private tokenStorage = inject(TokenStorageService);

  /** Send signed-in users to their dashboard, everyone else to the landing page. */
  get homeLink(): string {
    return this.tokenStorage.isAuthenticated() ? '/dashboard' : '/';
  }
}
