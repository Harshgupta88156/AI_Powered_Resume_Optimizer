import { Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

/** `-1` renders as a gap ellipsis rather than a page button. */
const GAP = -1;

/**
 * Shared pager: numbered pages, first/last shortcuts and an item range.
 *
 * Replaces the Previous/Next pairs that were duplicated across the resume, job
 * description and history lists. Those gave no sense of how much data existed
 * and made reaching page 9 a nine-click job.
 */
@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './pagination.html',
  styleUrl: './pagination.css'
})
export class Pagination {

  private pageSignal = signal(0);
  private totalPagesSignal = signal(0);

  /** Zero-based, matching Spring Data's `Page.number`. */
  @Input({ required: true })
  set page(value: number) { this.pageSignal.set(value ?? 0); }

  @Input({ required: true })
  set totalPages(value: number) { this.totalPagesSignal.set(value ?? 0); }

  /** Optional, only used for the "Showing x-y of z" caption. */
  @Input() totalElements: number | null = null;
  @Input() pageSize = 20;

  @Output() pageChange = new EventEmitter<number>();

  readonly gap = GAP;

  currentPage = this.pageSignal.asReadonly();
  totalPageCount = this.totalPagesSignal.asReadonly();

  /** Hidden entirely for a single page — a lone "Page 1 of 1" is just noise. */
  visible = computed(() => this.totalPagesSignal() > 1);

  /**
   * Windowed page list: always the first and last page, plus the current page
   * and its neighbours, with gaps collapsed. Keeps the control a fixed width
   * whether there are 3 pages or 300.
   */
  pages = computed<number[]>(() => {
    const total = this.totalPagesSignal();
    const current = this.pageSignal();

    if (total <= 7) {
      return Array.from({ length: total }, (_, i) => i);
    }

    const result: number[] = [0];
    const start = Math.max(1, current - 1);
    const end = Math.min(total - 2, current + 1);

    if (start > 1) result.push(GAP);
    for (let i = start; i <= end; i++) result.push(i);
    if (end < total - 2) result.push(GAP);

    result.push(total - 1);
    return result;
  });

  /** 1-based inclusive range of the items on screen. */
  range = computed(() => {
    const total = this.totalElements;
    if (total == null || total <= 0) return null;

    const from = this.pageSignal() * this.pageSize + 1;
    const to = Math.min(from + this.pageSize - 1, total);
    return { from, to, total };
  });

  go(page: number): void {
    if (page === GAP) return;
    if (page < 0 || page >= this.totalPagesSignal()) return;
    if (page === this.pageSignal()) return;
    this.pageChange.emit(page);
  }
}
