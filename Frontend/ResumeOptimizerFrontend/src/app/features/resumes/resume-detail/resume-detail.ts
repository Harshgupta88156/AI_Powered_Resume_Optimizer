import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ResumeService } from '../../../core/services/resume.service';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../core/utils/error.util';
import { ResumeResponse, ResumeVersionResponse } from '../../../core/models/resume.model';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { ConfirmDialog } from '../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-resume-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, Spinner, ConfirmDialog],
  templateUrl: './resume-detail.html',
  styleUrl: './resume-detail.css'
})
export class ResumeDetail implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private resumeService = inject(ResumeService);
  private toast = inject(ToastService);

  resumeId = Number(this.route.snapshot.paramMap.get('id'));

  loading = signal(true);
  error = signal('');
  resume = signal<ResumeResponse | null>(null);
  versions = signal<ResumeVersionResponse[]>([]);

  editMode = signal(false);
  editDisplayName = signal('');
  editNotes = signal('');
  saving = signal(false);

  addingVersion = signal(false);
  newVersionFile = signal<File | null>(null);
  versionFileError = signal('');

  showDeleteConfirm = signal(false);
  deleting = signal(false);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.resumeService.getById(this.resumeId).subscribe({
      next: (resume) => {
        this.resume.set(resume);
        this.editDisplayName.set(resume.displayName);
        this.editNotes.set(resume.notes ?? '');
        this.loading.set(false);
        this.loadVersions();
      },
      error: (err) => {
        this.error.set(extractErrorMessage(err, 'Could not load this resume.'));
        this.loading.set(false);
      }
    });
  }

  loadVersions(): void {
    this.resumeService.listVersions(this.resumeId).subscribe({
      next: (versions) => this.versions.set(versions),
      error: () => this.toast.error('Could not load resume versions.')
    });
  }

  toggleEdit(): void {
    this.editMode.update(v => !v);
    if (this.resume()) {
      this.editDisplayName.set(this.resume()!.displayName);
      this.editNotes.set(this.resume()!.notes ?? '');
    }
  }

  saveEdit(): void {
    if (!this.editDisplayName().trim()) {
      this.toast.error('Display name cannot be empty.');
      return;
    }

    this.saving.set(true);

    this.resumeService.update(this.resumeId, {
      displayName: this.editDisplayName(),
      notes: this.editNotes() || null
    }).subscribe({
      next: (resume) => {
        this.resume.set(resume);
        this.saving.set(false);
        this.editMode.set(false);
        this.toast.success('Resume updated.');
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(extractErrorMessage(err, 'Could not update resume.'));
      }
    });
  }

  toggleAddVersion(): void {
    this.addingVersion.update(v => !v);
    this.newVersionFile.set(null);
    this.versionFileError.set('');
  }

  onVersionFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.versionFileError.set('');

    if (file) {
      const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
      if (!['.pdf', '.doc', '.docx'].includes(ext)) {
        this.versionFileError.set('Only PDF, DOC, and DOCX files are supported.');
        input.value = '';
        return;
      }
    }

    this.newVersionFile.set(file);
  }

  uploadVersion(): void {
    const file = this.newVersionFile();
    if (!file) {
      this.versionFileError.set('Please choose a file.');
      return;
    }

    this.saving.set(true);

    this.resumeService.addVersion(this.resumeId, file).subscribe({
      next: (version) => {
        this.versions.update(list => [...list, version]);
        this.saving.set(false);
        this.toggleAddVersion();
        this.toast.success('New version uploaded.');
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.error(extractErrorMessage(err, 'Could not upload new version.'));
      }
    });
  }

  requestDelete(): void {
    this.showDeleteConfirm.set(true);
  }

  confirmDelete(): void {
    this.deleting.set(true);

    this.resumeService.delete(this.resumeId).subscribe({
      next: () => {
        this.toast.success('Resume deleted.');
        this.router.navigate(['/resumes']);
      },
      error: (err) => {
        this.deleting.set(false);
        this.showDeleteConfirm.set(false);
        this.toast.error(extractErrorMessage(err, 'Could not delete this resume. It may have existing analyses.'));
      }
    });
  }

  cancelDelete(): void {
    this.showDeleteConfirm.set(false);
  }
}

