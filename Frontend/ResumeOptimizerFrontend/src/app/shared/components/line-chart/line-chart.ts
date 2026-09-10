import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface SeriesPoint {
  /** ISO date or any label; rendered as-is on the axis. */
  label: string;
  value: number;
}

interface Plotted {
  x: number;
  y: number;
  point: SeriesPoint;
}

const WIDTH = 640;
const HEIGHT = 180;
const PAD_X = 8;
const PAD_Y = 12;

/**
 * Sparkline-style area chart for a time series, drawn as raw SVG.
 *
 * Uses a viewBox with preserveAspectRatio="none" so it stretches to any
 * container width without needing a resize observer.
 */
@Component({
  selector: 'app-line-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './line-chart.html',
  styleUrl: './line-chart.css'
})
export class LineChart {

  private data = signal<SeriesPoint[]>([]);

  @Input({ required: true })
  set points(value: SeriesPoint[] | null) {
    this.data.set(value ?? []);
  }

  readonly width = WIDTH;
  readonly height = HEIGHT;

  private plotted = computed<Plotted[]>(() => {
    const points = this.data();
    if (points.length === 0) return [];

    const max = Math.max(...points.map(p => p.value), 1);
    const usableW = WIDTH - PAD_X * 2;
    const usableH = HEIGHT - PAD_Y * 2;

    // A single point has no span to divide by; centre it instead of dividing
    // by zero and producing NaN coordinates.
    const step = points.length > 1 ? usableW / (points.length - 1) : 0;

    return points.map((point, i) => ({
      x: points.length > 1 ? PAD_X + i * step : WIDTH / 2,
      y: PAD_Y + usableH - (point.value / max) * usableH,
      point
    }));
  });

  hasData = computed(() => this.plotted().length > 0);

  /** Polyline through every point. */
  linePath = computed(() =>
    this.plotted().map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
  );

  /** Same path closed along the baseline, for the fill underneath. */
  areaPath = computed(() => {
    const pts = this.plotted();
    if (!pts.length) return '';

    const baseline = HEIGHT - PAD_Y;
    const head = `M ${pts[0].x.toFixed(1)} ${baseline}`;
    const line = pts.map(p => `L ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
    return `${head} ${line} L ${pts[pts.length - 1].x.toFixed(1)} ${baseline} Z`;
  });

  dots = computed(() => this.plotted());

  peak = computed(() => {
    const points = this.data();
    return points.length ? Math.max(...points.map(p => p.value)) : 0;
  });

  total = computed(() => this.data().reduce((sum, p) => sum + p.value, 0));

  /** First and last labels only — a tick per day is unreadable. */
  axisLabels = computed(() => {
    const points = this.data();
    if (points.length < 2) return null;
    return { first: points[0].label, last: points[points.length - 1].label };
  });

  format(value: number): string {
    return new Intl.NumberFormat('en-US').format(value);
  }
}
