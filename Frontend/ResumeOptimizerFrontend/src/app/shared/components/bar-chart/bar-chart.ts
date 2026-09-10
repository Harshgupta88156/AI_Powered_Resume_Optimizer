import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface ChartDatum {
  label: string;
  count: number;
}

interface Bar extends ChartDatum {
  percent: number;
  share: number;
}

/**
 * Horizontal bar chart, drawn with plain DOM rather than a charting library.
 *
 * chart.js and ng2-charts are already dependencies, but both render to <canvas>,
 * which cannot inherit the app's CSS custom properties — every colour would have
 * to be recomputed on theme switch. Div-based bars follow the theme for free,
 * reflow responsively, and keep the label text selectable and screen-readable.
 */
@Component({
  selector: 'app-bar-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './bar-chart.html',
  styleUrl: './bar-chart.css'
})
export class BarChart {

  private data = signal<ChartDatum[]>([]);

  @Input({ required: true })
  set items(value: ChartDatum[] | null) {
    this.data.set(value ?? []);
  }

  /** Bars are scaled against the largest value, not the total. */
  @Input() maxItems = 8;

  bars = computed<Bar[]>(() => {
    const items = this.data()
      .filter(d => d && d.count > 0)
      .slice(0, this.maxItems);

    if (!items.length) return [];

    const max = Math.max(...items.map(d => d.count));
    const total = items.reduce((sum, d) => sum + d.count, 0);

    return items.map(d => ({
      ...d,
      // Guard against a zero max producing NaN width.
      percent: max > 0 ? Math.round((d.count / max) * 100) : 0,
      share: total > 0 ? Math.round((d.count / total) * 100) : 0
    }));
  });

  hasData = computed(() => this.bars().length > 0);

  format(value: number): string {
    return new Intl.NumberFormat('en-US').format(value);
  }
}
