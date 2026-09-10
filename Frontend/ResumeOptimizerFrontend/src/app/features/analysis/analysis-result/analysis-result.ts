import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AnalysisService } from '../../../core/services/analysis.service';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../core/utils/error.util';
import { downloadText, toSafeFileName } from '../../../core/utils/file-download.util';
import { buildResumeDocument } from '../../../core/utils/markdown.util';
import { AnalysisResponse } from '../../../core/models/analysis.model';
import { Spinner } from '../../../shared/components/spinner/spinner';
import { ResumePreview } from '../../../shared/components/resume-preview/resume-preview';
import { LearningResourceResponse } from '../../../core/models/learning-resource.model';

interface LearningResourceGroup {
  category: string;
  resources: LearningResourceResponse[];
}

@Component({
  selector: 'app-analysis-result',
  standalone: true,
  imports: [CommonModule, RouterLink, Spinner, ResumePreview],
  templateUrl: './analysis-result.html',
  styleUrl: './analysis-result.css'
})
export class AnalysisResult implements OnInit {

  private route = inject(ActivatedRoute);
  private analysisService = inject(AnalysisService);
  private toast = inject(ToastService);

  analysisId = Number(this.route.snapshot.paramMap.get('id'));

  loading = signal(true);
  error = signal('');
  analysis = signal<AnalysisResponse | null>(null);

  generatingMarkdown = signal(false);
  markdown = signal<string | null>(null);
  /** True while we silently check whether a resume was already generated. */
  loadingCachedMarkdown = signal(false);
  downloading = signal(false);

  /** 'preview' renders the resume as it will print; 'edit' shows editable markdown; 'source' shows raw markdown. */
  view = signal<'preview' | 'edit' | 'source'>('preview');

  /** Tracks whether the user has made local edits to the markdown. */
  hasUnsavedEdits = signal(false);

  setView(next: 'preview' | 'edit' | 'source'): void {
    this.view.set(next);
  }

  /** Called from the textarea when the user types. */
  onMarkdownEdit(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    this.markdown.set(textarea.value);
    this.hasUnsavedEdits.set(true);
  }

  ngOnInit(): void {
    if (!Number.isFinite(this.analysisId) || this.analysisId <= 0) {
      this.error.set('That analysis link looks malformed.');
      this.loading.set(false);
      return;
    }
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');

    this.analysisService.getById(this.analysisId).subscribe({
      next: (analysis) => {
        this.analysis.set(analysis);
        this.loading.set(false);

        // A tailored resume generated in an earlier visit is stored server-side.
        // Without this the Download buttons stayed hidden until you clicked
        // Generate again, which is what made the feature look broken.
        if (analysis.status === 'COMPLETED') {
          this.loadCachedMarkdown();
          this.loadLearningResources();
        }
      },
      error: (err) => {
        this.error.set(extractErrorMessage(err, 'Could not load this analysis.'));
        this.loading.set(false);
      }
    });
  }

  /** Fetches the cached markdown without ever surfacing an error toast. */
  private loadCachedMarkdown(): void {
    this.loadingCachedMarkdown.set(true);

    this.analysisService.getMarkdown(this.analysisId).subscribe({
      next: (res) => {
        if (res?.markdownCode) {
          this.markdown.set(res.markdownCode);
        }
        this.loadingCachedMarkdown.set(false);
      },
      error: () => this.loadingCachedMarkdown.set(false)
    });
  }

  generateMarkdown(regenerate = false): void {
    if (this.generatingMarkdown()) return;

    this.generatingMarkdown.set(true);

    this.analysisService.generateMarkdown(this.analysisId, regenerate).subscribe({
      next: (res) => {
        this.generatingMarkdown.set(false);

        const code = res?.markdownCode?.trim();
        if (!code) {
          this.toast.error('The AI returned an empty resume. Try regenerating.');
          return;
        }

        this.markdown.set(code);
        this.toast.success(
          regenerate ? 'Tailored resume regenerated.' : 'Tailored resume ready.'
        );
      },
      error: (err) => {
        this.generatingMarkdown.set(false);
        this.toast.error(extractErrorMessage(
          err,
          'Could not generate the tailored resume. The AI service may be offline.'
        ));
      }
    });
  }

  copyMarkdown(): void {
    const md = this.markdown();
    if (!md) return;

    // navigator.clipboard is undefined on http:// origins other than localhost,
    // so fall back to a hidden textarea rather than throwing.
    if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(md)
        .then(() => this.toast.success('Markdown copied to clipboard.'))
        .catch(() => this.fallbackCopy(md));
      return;
    }

    this.fallbackCopy(md);
  }

  private fallbackCopy(text: string): void {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.setAttribute('readonly', '');
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();

    try {
      const ok = document.execCommand('copy');
      ok
        ? this.toast.success('Markdown copied to clipboard.')
        : this.toast.error('Could not copy. Select the text and copy manually.');
    } catch {
      this.toast.error('Could not copy. Select the text and copy manually.');
    } finally {
      textarea.remove();
    }
  }

  /** Base filename shared by the .md and .pdf exports. */
  private get exportFileName(): string {
    const a = this.analysis();
    const parts = [a?.resumeDisplayName, a?.jobTitle, a?.company]
      .filter((part): part is string => !!part && part.trim().length > 0);

    return parts.length
      ? toSafeFileName(parts.join('-'), `tailored-resume-${this.analysisId}`)
      : `tailored-resume-${this.analysisId}`;
  }

  downloadMarkdown(): void {
    const md = this.markdown();

    if (!md) {
      this.toast.error('Generate the tailored resume first.');
      return;
    }

    this.downloading.set(true);

    try {
      downloadText(md, `${this.exportFileName}.md`, 'text/markdown;charset=utf-8');
      this.toast.success('Tailored resume downloaded.');
    } catch {
      this.toast.error('The download was blocked by your browser.');
    } finally {
      // The anchor click is synchronous; the flag only debounces double-clicks.
      setTimeout(() => this.downloading.set(false), 600);
    }
  }

  /**
   * Word and Google Docs both open .html cleanly with formatting intact, which
   * makes this the practical route to an editable document without adding a
   * docx generator to the bundle.
   */
  downloadHtml(): void {
    const md = this.markdown();

    if (!md) {
      this.toast.error('Generate the tailored resume first.');
      return;
    }

    try {
      downloadText(
        this.buildPrintDocument(md),
        `${this.exportFileName}.html`,
        'text/html;charset=utf-8'
      );
      this.toast.success('Tailored resume downloaded. Open it in Word or Docs to edit.');
    } catch {
      this.toast.error('The download was blocked by your browser.');
    }
  }

  /**
   * Renders the markdown into a hidden same-origin iframe and prints that.
   *
   * The previous implementation used `window.open()`, which pop-up blockers
   * kill by default — the user clicked Download PDF and nothing happened. An
   * iframe needs no permission and cannot be blocked.
   */
  printMarkdownAsPdf(): void {
    const md = this.markdown();

    if (!md) {
      this.toast.error('Generate the tailored resume first.');
      return;
    }

    const iframe = document.createElement('iframe');
    iframe.setAttribute('aria-hidden', 'true');
    iframe.style.position = 'fixed';
    iframe.style.right = '0';
    iframe.style.bottom = '0';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.border = '0';

    document.body.appendChild(iframe);

    const doc = iframe.contentDocument;
    const win = iframe.contentWindow;

    if (!doc || !win) {
      iframe.remove();
      this.toast.error('Could not open the print view.');
      return;
    }

    doc.open();
    doc.write(this.buildPrintDocument(md));
    doc.close();

    const cleanup = () => setTimeout(() => iframe.remove(), 1000);

    // Fonts and layout need a frame to settle or the first page prints blank.
    setTimeout(() => {
      try {
        win.focus();
        win.print();
      } catch {
        this.toast.error('Could not open the print view.');
      } finally {
        cleanup();
      }
    }, 250);

    this.toast.info('Choose "Save as PDF" in the print dialog.');
  }

  /**
     * Builds the standalone document used for both printing and the .html
     * export, so the PDF matches the on-screen preview exactly.
     */
  private buildPrintDocument(md: string): string {
    return buildResumeDocument(md, this.exportFileName);
  }

  scoreClass(score: number | null): string {
    if (score == null) return 'badge-neutral';
    if (score >= 75) return 'badge-success';
    if (score >= 50) return 'badge-warning';
    return 'badge-danger';
  }

  statusClass(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'badge-success';
      case 'FAILED': return 'badge-danger';
      case 'PROCESSING': return 'badge-warning';
      default: return 'badge-neutral';
    }
  }

  // ── Learning resources ───────────────────────────────────────────────────
  // Served by the backend catalog rather than a bundled TypeScript file, so
  // dead links can be fixed and new skills added without a frontend release.

  learningResources = signal<LearningResourceResponse[]>([]);
  learningLoading = signal(false);

  private loadLearningResources(): void {
    this.learningLoading.set(true);

    this.analysisService.getLearningResources(this.analysisId).subscribe({
      next: (resources) => {
        this.learningResources.set(resources ?? []);
        this.learningLoading.set(false);
      },
      // Recommendations are supplementary — a failure here must never take
      // over the page the user came for.
      error: () => this.learningLoading.set(false)
    });
  }

  /** Grouped by category for the section headings. */
  get learningResourceGroups(): LearningResourceGroup[] {
    const grouped = new Map<string, LearningResourceResponse[]>();

    for (const resource of this.learningResources()) {
      const bucket = grouped.get(resource.category) ?? [];
      bucket.push(resource);
      grouped.set(resource.category, bucket);
    }

    return Array.from(grouped.entries()).map(([category, resources]) => ({ category, resources }));
  }

  get coveredLearningSkillCount(): number {
    return new Set(this.learningResources().map(r => r.skill)).size;
  }

  /** "6h" / "40h" caption; empty when the catalog has no estimate. */
  formatHours(hours: number | null): string {
    return hours && hours > 0 ? `~${hours}h` : '';
  }

  /** Title-cases the enum for display: PRACTICE -> Practice. */
  formatType(value: string): string {
    if (!value) return '';
    return value.charAt(0) + value.slice(1).toLowerCase();
  }
}
