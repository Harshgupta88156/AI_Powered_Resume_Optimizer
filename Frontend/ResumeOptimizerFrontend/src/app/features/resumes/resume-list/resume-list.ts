import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ResumeService } from '../../../core/services/resume.service';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../core/utils/error.util';
import { ResumeResponse } from '../../../core/models/resume.model';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { Pagination } from '../../../shared/components/pagination/pagination';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';

const ACCEPTED_TYPES = [
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
];

@Component({
  selector: 'app-resume-list',
  standalone: true,
  imports: [CommonModule, RouterLink, Spinner, EmptyState, ConfirmDialog, Pagination],
  templateUrl: './resume-list.html',
  styleUrl: './resume-list.css',
})
export class ResumeList implements OnInit {
  private resumeService = inject(ResumeService);
  private toast = inject(ToastService);

  loading = signal(true);
  error = signal('');
  resumes = signal<ResumeResponse[]>([]);

  page = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  readonly pageSize = 8;

  showUploadForm = signal(false);
  selectedFile = signal<File | null>(null);
  displayNameInput = signal('');
  uploading = signal(false);
  fileError = signal('');

  deleteTarget = signal<ResumeResponse | null>(null);
  deleting = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    this.loading.set(true);
    this.error.set('');

    this.resumeService.list(page, this.pageSize).subscribe({
      next: (res) => {
        this.resumes.set(res.content);
        this.page.set(res.number);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(extractErrorMessage(err, 'Could not load your resumes.'));
        this.loading.set(false);
      },
    });
  }

  toggleUploadForm(): void {
    this.showUploadForm.update((v) => !v);
    this.selectedFile.set(null);
    this.displayNameInput.set('');
    this.fileError.set('');
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.fileError.set('');

    if (!file) {
      this.selectedFile.set(null);
      return;
    }

    const extension = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
    const validExt = ['.pdf', '.doc', '.docx'].includes(extension);
    const validType = ACCEPTED_TYPES.includes(file.type) || validExt;

    if (!validType) {
      this.fileError.set('Only PDF, DOC, and DOCX files are supported.');
      this.selectedFile.set(null);
      input.value = '';
      return;
    }

    this.selectedFile.set(file);
    if (!this.displayNameInput()) {
      this.displayNameInput.set(file.name.replace(/\.[^/.]+$/, ''));
    }
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file) {
      this.fileError.set('Please choose a resume file to upload.');
      return;
    }

    this.uploading.set(true);

    this.resumeService.upload(file, this.displayNameInput() || undefined).subscribe({
      next: (resume) => {
        this.uploading.set(false);
        this.toast.success('Resume uploaded successfully.');
        this.toggleUploadForm();
        this.resumes.update((list) => [resume, ...list]);
      },
      error: (err) => {
        this.uploading.set(false);
        this.toast.error(extractErrorMessage(err, 'Upload failed. Please try again.'));
      },
    });
  }

  requestDelete(resume: ResumeResponse, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    this.deleteTarget.set(resume);
  }

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target) return;

    this.deleting.set(true);

    this.resumeService.delete(target.resumeId).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.resumes.update((list) => list.filter((r) => r.resumeId !== target.resumeId));
        this.toast.success('Resume deleted.');
      },
      error: (err) => {
        this.deleting.set(false);
        this.deleteTarget.set(null);
        this.toast.error(
          extractErrorMessage(err, 'Could not delete this resume. It may have existing analyses.'),
        );
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
