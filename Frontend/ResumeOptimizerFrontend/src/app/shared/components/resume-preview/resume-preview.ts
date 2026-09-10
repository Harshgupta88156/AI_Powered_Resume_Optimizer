import { Component, DOCUMENT, Input, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RESUME_STYLESHEET, markdownToHtml } from '../../../core/utils/markdown.util';

const STYLE_ELEMENT_ID = 'ro-resume-stylesheet';

/**
 * Renders tailored-resume markdown as it will actually print.
 *
 * The stylesheet is injected once into <head> rather than declared as a
 * component style. Angular's view encapsulation rewrites component CSS with a
 * host attribute that [innerHTML] content never receives, so encapsulated rules
 * would simply not apply to the rendered resume. Injecting it also keeps
 * RESUME_STYLESHEET as the single source of truth shared with the PDF export,
 * so preview and download can't drift apart.
 */
@Component({
  selector: 'app-resume-preview',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="resume-preview-sheet">
      <div class="resume-doc" [innerHTML]="html()"></div>
    </div>
  `,
  styles: [`
    /* The paper is always light: it is what gets printed and shared, so it
       deliberately does not follow the app's dark theme. */
    :host {
      display: block;
      background: #e9edf2;
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 24px;
      overflow-x: auto;
    }

    .resume-preview-sheet { display: flex; justify-content: center; }

    @media (max-width: 640px) {
      :host { padding: 12px; }
    }
  `]
})
export class ResumePreview {

  private document = inject(DOCUMENT);
  private markdownSignal = signal('');

  @Input({ required: true })
  set markdown(value: string | null) {
    this.markdownSignal.set(value ?? '');
  }

  html = computed(() => markdownToHtml(this.markdownSignal()));

  constructor() {
    this.ensureStylesheet();
  }

  /** Idempotent — many previews on one page still yield a single <style>. */
  private ensureStylesheet(): void {
    if (this.document.getElementById(STYLE_ELEMENT_ID)) {
      return;
    }

    const style = this.document.createElement('style');
    style.id = STYLE_ELEMENT_ID;
    style.textContent = `
      .resume-doc {
        padding: 16mm 15mm;
        border-radius: 3px;
        box-shadow: 0 2px 14px rgba(15, 23, 42, .18);
      }

      @media (max-width: 640px) {
        .resume-doc { padding: 10mm 8mm; }
      }

      ${RESUME_STYLESHEET}
    `;

    this.document.head.appendChild(style);
  }
}
