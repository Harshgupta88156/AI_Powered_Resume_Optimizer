import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../../core/services/dashboard.service';
import { AuthService } from '../../core/services/auth.service';
import { DashboardResponse } from '../../core/models/dashboard.model';
import { extractErrorMessage } from '../../core/utils/error.util';
import { Spinner } from '../../shared/components/spinner/spinner';
import { EmptyState } from '../../shared/components/empty-state/empty-state';

@Component({

  selector: 'app-dashboard',

  standalone: true,

  imports: [CommonModule, RouterLink, Spinner, EmptyState],

  templateUrl: './dashboard.html',

  styleUrl: './dashboard.css'

})

export class Dashboard implements OnInit {

  private dashboardService = inject(DashboardService);
  private authService = inject(AuthService);

  currentUser = this.authService.currentUser;

  loading = signal(true);
  error = signal('');
  data = signal<DashboardResponse | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.dashboardService.getDashboard().subscribe({
      next: (res) => {
        this.data.set(res);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(extractErrorMessage(err, 'Could not load your dashboard right now.'));
        this.loading.set(false);
      }
    });
  }

  hasRecentActivity(): boolean {
    const activity = this.data()?.recentActivity;
    return !!activity && (activity.recentResumes.length > 0 || activity.recentAnalyses.length > 0);
  }

  scoreClass(score: number | null): string {
    if (score == null) return 'badge-neutral';
    if (score >= 75) return 'badge-success';
    if (score >= 50) return 'badge-warning';
    return 'badge-danger';
  }

}
