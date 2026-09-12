import { Component, HostListener, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { JobDescriptionService } from '../../../core/services/job-description.service';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../core/utils/error.util';
import { JobDescriptionResponse } from '../../../core/models/job-description.model';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';

type InputMode = 'file' | 'text';

@Component({
  selector: 'app-jd-list',
  standalone: true,
  imports: [CommonModule, RouterLink, Spinner, EmptyState, ConfirmDialog, Pagination],
  templateUrl: './jd-list.html',
  styleUrl: './jd-list.css',
})
export class JdList implements OnInit {
  private jdService = inject(JobDescriptionService);
  private toast = inject(ToastService);

  loading = signal(true);
  error = signal('');
  jobDescriptions = signal<JobDescriptionResponse[]>([]);

  page = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  readonly pageSize = 4;

  showForm = signal(false);
  inputMode = signal<InputMode>('text');
  company = signal('');
  jobTitle = signal('');
  textContent = signal('');
  selectedFile = signal<File | null>(null);
  fileError = signal('');
  submitting = signal(false);

  deleteTarget = signal<JobDescriptionResponse | null>(null);

  // ── Viewer ───────────────────────────────────────────────────────────────
  // The list only carries a short preview, so opening a JD fetches the full
  // text from GET /job-descriptions/:id.
  viewTarget = signal<JobDescriptionResponse | null>(null);
  viewLoading = signal(false);
  viewError = signal('');
  fileLoading = signal(false);
  fileObjectUrl = signal<string | null>(null);

  /** 'text' shows the extracted text; 'file' embeds the original document. */
  viewTab = signal<'text' | 'file'>('text');

  // ── Editing ──────────────────────────────────────────────────────────────
  editing = signal(false);
  editCompany = signal('');
  editJobTitle = signal('');
  editText = signal('');
  savingEdit = signal(false);

  private sanitizer = inject(DomSanitizer);

  /**
   * Only a same-origin-safe https URL is ever embedded. Angular blocks an
   * unsanitised URL in an iframe src, and bypassing without this check would
   * defeat the protection rather than satisfy it.
   */
  safeFileUrl = computed<SafeResourceUrl | null>(() => {
    const url = this.fileObjectUrl();
    return url ? this.sanitizer.bypassSecurityTrustResourceUrl(url) : null;
  });

  /** Only PDFs render reliably in an iframe; Word docs must be downloaded. */
  canEmbedFile = computed(() => {
    const jd = this.viewTarget();
    if (!jd || !this.hasOriginalFile(jd)) return false;
    const type = (jd.contentType ?? '').toLowerCase();
    const name = (jd.fileName ?? '').toLowerCase();
    return type.includes('pdf') || name.endsWith('.pdf');
  });

  /** Text of an uploaded file is parser output, so it is read-only. */
  canEditText = computed(() => this.viewTarget()?.source === 'TEXT');

  hasOriginalFile(jd: JobDescriptionResponse | null): boolean {
    return !!jd
      && jd.source === 'FILE'
      && Number.isFinite(jd.jobDescriptionId)
      && jd.jobDescriptionId > 0
      && !!jd.cloudinaryUrl
      && !jd.cloudinaryUrl.includes('demo.invalid');
  }

  ngOnInit(): void {
    this.load();
  }

  /** Opens the reader. Shows what we already have, then fills in the full text. */
  view(jd: JobDescriptionResponse, event?: Event): void {
    event?.stopPropagation();

    this.viewTarget.set(jd);
    this.viewError.set('');
    this.viewTab.set('text');
    this.releaseFileUrl();

    // Already fetched once this session - don't re-request.
    if (jd.extractedText) {
      return;
    }

    if (!Number.isFinite(jd.jobDescriptionId) || jd.jobDescriptionId <= 0) {
      this.viewError.set('This job description has an invalid record ID and cannot be opened.');
      return;
    }

    this.viewLoading.set(true);

    this.jdService.getById(jd.jobDescriptionId).subscribe({
      next: (full) => {
        this.viewTarget.set(full);
        this.viewLoading.set(false);

        // Cache onto the list row so reopening is instant.
        this.jobDescriptions.update((list) =>
          list.map((item) =>
            item.jobDescriptionId === full.jobDescriptionId ? { ...item, ...full } : item,
          ),
        );
      },
      error: (err) => {
        this.viewLoading.set(false);
        this.viewError.set(extractErrorMessage(err, 'Could not load this job description.'));
      },
    });
  }

  /** Escape closes the reader, matching the confirm dialog's behaviour. */
  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.viewTarget()) {
      this.closeView();
    }
  }

  closeView(): void {
    this.releaseFileUrl();
    this.viewTarget.set(null);
    this.viewError.set('');
    this.viewLoading.set(false);
    this.editing.set(false);
    this.viewTab.set('text');
  }

  setViewTab(tab: 'text' | 'file'): void {
    this.viewTab.set(tab);
    if (tab === 'file') {
      this.loadOriginalFile();
    }
  }

  private loadOriginalFile(): void {
    const jd = this.viewTarget();
    if (!jd || !this.hasOriginalFile(jd) || !this.canEmbedFile() || this.fileObjectUrl() || this.fileLoading()) return;

    this.fileLoading.set(true);
    this.viewError.set('');
    this.jdService.getFile(jd.jobDescriptionId).subscribe({
      next: (blob) => {
        this.fileObjectUrl.set(URL.createObjectURL(blob));
        this.fileLoading.set(false);
      },
      error: (err) => {
        this.fileLoading.set(false);
        this.viewError.set(extractErrorMessage(err, 'Could not load the original file.'));
      },
    });
  }

  private releaseFileUrl(): void {
    const url = this.fileObjectUrl();
    if (url) {
      URL.revokeObjectURL(url);
      this.fileObjectUrl.set(null);
    }
    this.fileLoading.set(false);
  }

  openOriginalFile(): void {
    const jd = this.viewTarget();
    if (!jd || !this.hasOriginalFile(jd)) {
      this.viewError.set('The original file is not available for this job description.');
      return;
    }

    const popup = window.open('', '_blank');
    if (!popup) {
      this.toast.error('Please allow pop-ups to open the original file.');
      return;
    }

    if (this.fileObjectUrl()) {
      popup.location.href = this.fileObjectUrl()!;
      return;
    }

    this.fileLoading.set(true);
    this.jdService.getFile(jd.jobDescriptionId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        this.fileObjectUrl.set(url);
        this.fileLoading.set(false);
        popup.location.href = url;
      },
      error: (err) => {
        this.fileLoading.set(false);
        popup.close();
        this.viewError.set(extractErrorMessage(err, 'Could not load the original file.'));
      },
    });
  }

  startEdit(): void {
    const jd = this.viewTarget();
    if (!jd) return;

    this.editCompany.set(jd.company ?? '');
    this.editJobTitle.set(jd.jobTitle ?? '');
    this.editText.set(jd.extractedText ?? '');
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
  }

  saveEdit(): void {
    const jd = this.viewTarget();
    if (!jd || this.savingEdit()) return;

    const title = this.editJobTitle().trim();
    if (!title) {
      this.toast.error('Job title cannot be empty.');
      return;
    }

    const changes: { company?: string; jobTitle?: string; text?: string } = {
      company: this.editCompany().trim(),
      jobTitle: title,
    };

    // Only send text for TEXT-sourced entries; the backend rejects it for
    // uploaded files, and sending it would surface a confusing error.
    if (this.canEditText()) {
      const text = this.editText().trim();
      if (!text) {
        this.toast.error('Job description text cannot be empty.');
        return;
      }
      changes.text = text;
    }

    this.savingEdit.set(true);

    this.jdService.update(jd.jobDescriptionId, changes).subscribe({
      next: (updated) => {
        this.savingEdit.set(false);
        this.editing.set(false);
        this.viewTarget.set(updated);

        this.jobDescriptions.update((list) =>
          list.map((item) =>
            item.jobDescriptionId === updated.jobDescriptionId ? { ...item, ...updated } : item,
          ),
        );

        this.toast.success('Job description updated.');
      },
      error: (err) => {
        this.savingEdit.set(false);
        this.toast.error(extractErrorMessage(err, 'Could not update this job description.'));
      },
    });
  }

  copyText(): void {
    const text = this.viewTarget()?.extractedText;
    if (!text) return;

    navigator.clipboard
      ?.writeText(text)
      .then(() => this.toast.success('Job description copied.'))
      .catch(() => this.toast.error('Could not copy to clipboard.'));
  }

  /** "1,240 characters" caption; falls back to the text we hold. */
  charCount(jd: JobDescriptionResponse): number {
    return jd.textLength ?? jd.extractedText?.length ?? 0;
  }

  formatCount(value: number): string {
    return new Intl.NumberFormat('en-US').format(value);
  }

  load(page = 0): void {
    this.loading.set(true);
    this.error.set('');

    this.jdService.list(page, this.pageSize).subscribe({
      next: (res) => {
        this.jobDescriptions.set(res.content);
        this.page.set(res.number);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(extractErrorMessage(err, 'Could not load job descriptions.'));
        this.loading.set(false);
      },
    });
  }

  toggleForm(): void {
    this.showForm.update((v) => !v);
    this.resetForm();
  }

  resetForm(): void {
    this.company.set('');
    this.jobTitle.set('');
    this.textContent.set('');
    this.selectedFile.set(null);
    this.fileError.set('');
    this.inputMode.set('text');
  }

  setMode(mode: InputMode): void {
    this.inputMode.set(mode);
    this.fileError.set('');
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.fileError.set('');

    if (file) {
      const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
      if (!['.pdf', '.doc', '.docx'].includes(ext)) {
        this.fileError.set('Only PDF, DOC, and DOCX files are supported.');
        input.value = '';
        return;
      }
    }

    this.selectedFile.set(file);
  }

  submit(): void {
    const company = this.company() || undefined;
    const jobTitle = this.jobTitle() || undefined;
    if (company === undefined || jobTitle === undefined) {
      this.toast.error('Please enter both company and job title.');
      return;
    }
    if (this.inputMode() === 'text') {
      if (!this.textContent().trim()) {
        this.toast.error('Please paste the job description text.');
        return;
      }
      this.submitting.set(true);
      this.jdService.createFromText(this.textContent(), company, jobTitle).subscribe({
        next: (jd) => this.handleCreateSuccess(jd),
        error: (err) => this.handleCreateError(err),
      });
    } else {
      const file = this.selectedFile();
      if (!file) {
        this.fileError.set('Please choose a file.');
        return;
      }
      this.submitting.set(true);
      this.jdService.createFromFile(file, company, jobTitle).subscribe({
        next: (jd) => this.handleCreateSuccess(jd),
        error: (err) => this.handleCreateError(err),
      });
    }
  }

  private handleCreateSuccess(jd: JobDescriptionResponse): void {
    this.submitting.set(false);
    this.jobDescriptions.update((list) => [jd, ...list]);
    this.toast.success('Job description added.');
    this.toggleForm();
  }

  private handleCreateError(err: unknown): void {
    this.submitting.set(false);
    this.toast.error(extractErrorMessage(err, 'Could not save this job description.'));
  }

  requestDelete(jd: JobDescriptionResponse, event: Event): void {
    event.stopPropagation();
    this.deleteTarget.set(jd);
  }

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target) return;

    this.jdService.delete(target.jobDescriptionId).subscribe({
      next: () => {
        this.jobDescriptions.update((list) =>
          list.filter((j) => j.jobDescriptionId !== target.jobDescriptionId),
        );
        this.deleteTarget.set(null);
        this.toast.success('Job description deleted.');
      },
      error: (err) => {
        this.deleteTarget.set(null);
        this.toast.error(extractErrorMessage(err, 'Could not delete this job description.'));
      },
    });
  }

  cancelDelete(): void {
    this.deleteTarget.set(null);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages()) return;
    this.load(page);
  }
}
