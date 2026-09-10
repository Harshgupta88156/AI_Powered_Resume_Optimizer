import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ResumeService } from '../../../core/services/resume.service';
import { JobDescriptionService } from '../../../core/services/job-description.service';
import { AnalysisService } from '../../../core/services/analysis.service';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../core/utils/error.util';
import { ResumeResponse } from '../../../core/models/resume.model';
import { JobDescriptionResponse } from '../../../core/models/job-description.model';
import { AnalysisResponse } from '../../../core/models/analysis.model';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-analysis-trigger',
  standalone: true,
  imports: [CommonModule, RouterLink, Spinner, EmptyState],
  templateUrl: './analysis-trigger.html',
  styleUrl: './analysis-trigger.css'
})
export class AnalysisTrigger implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private resumeService = inject(ResumeService);
  private jdService = inject(JobDescriptionService);
  private analysisService = inject(AnalysisService);
  private toast = inject(ToastService);

  loadingOptions = signal(true);
  optionsError = signal('');
  resumes = signal<ResumeResponse[]>([]);
  jobDescriptions = signal<JobDescriptionResponse[]>([]);

  selectedResumeId = signal<number | null>(null);
  selectedJdId = signal<number | null>(null);
  analyzing = signal(false);

  loadingRecent = signal(true);
  recentAnalyses = signal<AnalysisResponse[]>([]);

  ngOnInit(): void {
    const resumeIdParam = this.route.snapshot.queryParamMap.get('resumeId');
    const jdIdParam = this.route.snapshot.queryParamMap.get('jobDescriptionId');
    if (resumeIdParam) this.selectedResumeId.set(Number(resumeIdParam));
    if (jdIdParam) this.selectedJdId.set(Number(jdIdParam));

    this.loadOptions();
    this.loadRecent();
  }

  loadOptions(): void {
    this.loadingOptions.set(true);
    this.optionsError.set('');

    this.resumeService.list(0, 100).subscribe({
      next: (res) => {
        this.resumes.set(res.content);
        this.jdService.list(0, 100).subscribe({
          next: (jdRes) => {
            this.jobDescriptions.set(jdRes.content);
            this.loadingOptions.set(false);
          },
          error: (err) => {
            this.optionsError.set(extractErrorMessage(err, 'Could not load job descriptions.'));
            this.loadingOptions.set(false);
          }
        });
      },
      error: (err) => {
        this.optionsError.set(extractErrorMessage(err, 'Could not load resumes.'));
        this.loadingOptions.set(false);
      }
    });
  }

  loadRecent(): void {
    this.loadingRecent.set(true);
    this.analysisService.list({ page: 0, size: 5 }).subscribe({
      next: (res) => {
        this.recentAnalyses.set(res.content);
        this.loadingRecent.set(false);
      },
      error: () => this.loadingRecent.set(false)
    });
  }

  runAnalysis(): void {
    const resumeId = this.selectedResumeId();
    const jobDescriptionId = this.selectedJdId();

    if (resumeId == null || jobDescriptionId == null) {
      this.toast.error('Please select both a resume and a job description.');
      return;
    }

    this.analyzing.set(true);

    this.analysisService.trigger({ resumeId, jobDescriptionId }).subscribe({
      next: (analysis) => {
        this.analyzing.set(false);
        this.toast.success('Analysis complete!');
        this.router.navigate(['/analysis', analysis.analysisId]);
      },
      error: (err) => {
        this.analyzing.set(false);
        this.toast.error(extractErrorMessage(err, 'Analysis failed. Please try again.'));
      }
    });
  }

  onResumeChange(value: string): void {
    this.selectedResumeId.set(value ? Number(value) : null);
  }

  onJobDescriptionChange(value: string): void {
    this.selectedJdId.set(value ? Number(value) : null);
  }

  scoreClass(score: number | null): string {
    if (score == null) return 'badge-neutral';
    if (score >= 75) return 'badge-success';
    if (score >= 50) return 'badge-warning';
    return 'badge-danger';
  }
}

