import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ChartDatum } from '../bar-chart/bar-chart';

interface Slice extends ChartDatum {
  share: number;
  offset: number;
  dash: number;
  color: string;
}

const RADIUS = 60;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

/**
 * Donut chart drawn as a single SVG circle per slice, using stroke-dasharray
 * offsets rather than arc paths — no trigonometry, and no chance of the arc
 * flags flipping on a slice that crosses 180°.
 *
 * Colours come from a fixed palette rather than CSS variables because SVG
 * strokes need concrete values; the palette is chosen to stay legible on both
 * the light and dark surfaces.
 */
@Component({
  selector: 'app-donut-chart',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './donut-chart.html',
  styleUrl: './donut-chart.css'
})
export class DonutChart {

  private data = signal<ChartDatum[]>([]);

  @Input({ required: true })
  set items(value: ChartDatum[] | null) {
    this.data.set(value ?? []);
  }

  /** Anything beyond this is merged into a single "Other" slice. */
  @Input() maxSlices = 5;

  readonly radius = RADIUS;
  readonly circumference = CIRCUMFERENCE;

  private palette = ['#2563EB', '#0EA5E9', '#8B5CF6', '#F59E0B', '#10B981', '#94A3B8'];

  slices = computed<Slice[]>(() => {
    const items = this.data().filter(d => d && d.count > 0);
    if (!items.length) return [];

    const head = items.slice(0, this.maxSlices);
    const tail = items.slice(this.maxSlices);

    // Collapsing the long tail keeps the legend readable and stops a hundred
    // one-count skills from rendering as invisible hairlines.
    const merged = tail.length
      ? [...head, { label: 'Other', count: tail.reduce((s, d) => s + d.count, 0) }]
      : head;

    const total = merged.reduce((sum, d) => sum + d.count, 0);
    if (total <= 0) return [];

    let cumulative = 0;

    return merged.map((d, i) => {
      const fraction = d.count / total;
      const dash = fraction * CIRCUMFERENCE;
      // Negative offset walks the stroke clockwise from 12 o'clock.
      const offset = -cumulative * CIRCUMFERENCE;
      cumulative += fraction;

      return {
        ...d,
        share: Math.round(fraction * 100),
        dash,
        offset,
        color: this.palette[i % this.palette.length]
      };
    });
  });

  hasData = computed(() => this.slices().length > 0);

  total = computed(() => this.slices().reduce((sum, s) => sum + s.count, 0));

  format(value: number): string {
    return new Intl.NumberFormat('en-US').format(value);
  }
}
