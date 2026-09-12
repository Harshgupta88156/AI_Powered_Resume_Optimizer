import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HistoryService } from '../../../core/services/history.service';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../core/utils/error.util';
import {
  AnalysisComparisonResponse,
  HistoryTimelineItemResponse,
} from '../../../core/models/history.model';
import { AnalysisStatus } from '../../../core/models/analysis.model';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-history-list',
  standalone: true,
  imports: [CommonModule, RouterLink, Spinner, EmptyState, Pagination],
  templateUrl: './history-list.html',
  styleUrl: './history-list.css',
})
export class HistoryList implements OnInit {
  private historyService = inject(HistoryService);
  private toast = inject(ToastService);

  loading = signal(true);
  error = signal('');
  items = signal<HistoryTimelineItemResponse[]>([]);
  page = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  readonly pageSize = 5;

  showFilters = signal(false);
  filterCompany = signal('');
  filterJobTitle = signal('');
  filterStatus = signal<AnalysisStatus | ''>('');
  filterFrom = signal('');
  filterTo = signal('');

  selectedForCompare = signal<number[]>([]);
  comparing = signal(false);
  comparison = signal<AnalysisComparisonResponse | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    this.loading.set(true);
    this.error.set('');

    this.historyService
      .search({
        page,
        size: this.pageSize,
        company: this.filterCompany() || undefined,
        jobTitle: this.filterJobTitle() || undefined,
        status: (this.filterStatus() || undefined) as AnalysisStatus | undefined,
        from: this.filterFrom() || undefined,
        to: this.filterTo() || undefined,
      })
      .subscribe({
        next: (res) => {
          this.items.set(res.content);
          this.page.set(res.number);
          this.totalPages.set(res.totalPages);
          this.totalElements.set(res.totalElements);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(extractErrorMessage(err, 'Could not load history.'));
          this.loading.set(false);
        },
      });
  }

  applyFilters(): void {
    this.load(0);
  }

  clearFilters(): void {
    this.filterCompany.set('');
    this.filterJobTitle.set('');
    this.filterStatus.set('');
    this.filterFrom.set('');
    this.filterTo.set('');
    this.load(0);
  }

  toggleFilters(): void {
    this.showFilters.update((v) => !v);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.load(page);
  }

  scoreClass(score: number | null): string {
    if (score == null) return 'badge-neutral';
    if (score >= 75) return 'badge-success';
    if (score >= 50) return 'badge-warning';
    return 'badge-danger';
  }

  statusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'badge-success';
      case 'FAILED':
        return 'badge-danger';
      case 'PROCESSING':
        return 'badge-warning';
      default:
        return 'badge-neutral';
    }
  }

  toggleCompareSelection(analysisId: number, event: Event): void {
    event.stopPropagation();
    this.selectedForCompare.update((list) => {
      if (list.includes(analysisId)) {
        return list.filter((id) => id !== analysisId);
      }
      if (list.length >= 2) {
        return [list[1], analysisId];
      }
      return [...list, analysisId];
    });
  }

  runCompare(): void {
    const [left, right] = this.selectedForCompare();
    if (left == null || right == null) {
      this.toast.error('Select exactly two analyses to compare.');
      return;
    }

    this.comparing.set(true);

    this.historyService.compare(left, right).subscribe({
      next: (res) => {
        this.comparison.set(res);
        this.comparing.set(false);
      },
      error: (err) => {
        this.comparing.set(false);
        this.toast.error(extractErrorMessage(err, 'Could not compare these analyses.'));
      },
    });
  }

  closeComparison(): void {
    this.comparison.set(null);
    this.selectedForCompare.set([]);
  }
}
