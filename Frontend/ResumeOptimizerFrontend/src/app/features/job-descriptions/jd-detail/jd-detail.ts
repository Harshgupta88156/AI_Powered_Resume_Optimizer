import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { JobDescriptionService } from '../../../core/services/job-description.service';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../core/utils/error.util';
import { downloadText, toSafeFileName } from '../../../core/utils/file-download.util';
import { JobDescriptionResponse } from '../../../core/models/job-description.model';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-jd-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, Spinner, ConfirmDialog],
  templateUrl: './jd-detail.html',
  styleUrl: './jd-detail.css'
})
export class JdDetail implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private jdService = inject(JobDescriptionService);
  private toast = inject(ToastService);

  jobDescriptionId = Number(this.route.snapshot.paramMap.get('id'));

  loading = signal(true);
  error = signal('');
  jd = signal<JobDescriptionResponse | null>(null);

  /** Long descriptions are clamped until the user asks for the rest. */
  expanded = signal(false);

  showDeleteConfirm = signal(false);
  deleting = signal(false);

  ngOnInit(): void {
    if (!Number.isFinite(this.jobDescriptionId) || this.jobDescriptionId <= 0) {
      this.error.set('That job description link looks malformed.');
      this.loading.set(false);
      return;
    }
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.jdService.getById(this.jobDescriptionId).subscribe({
      next: (jd) => {
        this.jd.set(jd);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(extractErrorMessage(err, 'Could not load this job description.'));
        this.loading.set(false);
      }
    });
  }

  get title(): string {
    const jd = this.jd();
    if (!jd) return 'Job description';
    return jd.jobTitle?.trim() || jd.fileName?.trim() || 'Untitled role';
  }

  get text(): string {
    return this.jd()?.extractedText?.trim() ?? '';
  }

  get hasText(): boolean {
    return this.text.length > 0;
  }

  /** Roughly a screenful; beyond this the body is clamped. */
  get isLong(): boolean {
    return this.text.length > 1200;
  }

  toggleExpanded(): void {
    this.expanded.update(v => !v);
  }

  copyText(): void {
    const text = this.text;
    if (!text) return;

    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(text)
        .then(() => this.toast.success('Job description copied.'))
        .catch(() => this.toast.error('Could not copy. Select the text and copy manually.'));
      return;
    }
    this.toast.error('Copying is not available in this browser.');
  }

  downloadText(): void {
    if (!this.hasText) return;

    const jd = this.jd();
    const name = toSafeFileName(
      [jd?.company, jd?.jobTitle].filter(Boolean).join('-'),
      `job-description-${this.jobDescriptionId}`
    );
    downloadText(this.text, `${name}.txt`);
    this.toast.success('Job description downloaded.');
  }

  analyzeAgainst(): void {
    this.router.navigate(['/analysis'], {
      queryParams: { jobDescriptionId: this.jobDescriptionId }
    });
  }

  requestDelete(): void {
    this.showDeleteConfirm.set(true);
  }

  confirmDelete(): void {
    this.deleting.set(true);

    this.jdService.delete(this.jobDescriptionId).subscribe({
      next: () => {
        this.toast.success('Job description deleted.');
        this.router.navigate(['/job-descriptions']);
      },
      error: (err) => {
        this.deleting.set(false);
        this.showDeleteConfirm.set(false);
        this.toast.error(extractErrorMessage(
          err,
          'Could not delete this job description. It may have existing analyses.'
        ));
      }
    });
  }

  cancelDelete(): void {
    this.showDeleteConfirm.set(false);
  }
}
