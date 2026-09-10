/**
 * @vitest-environment jsdom
 *
 * Geometry tests. Chart bugs are almost always division-by-zero or an
 * off-by-one in the point spacing, which is exactly what these cover.
 */
import '@angular/compiler';
import { describe, expect, it } from 'vitest';
import { LineChart } from './line-chart';
import { BarChart } from '../bar-chart/bar-chart';
import { DonutChart } from '../donut-chart/donut-chart';

describe('LineChart', () => {

  it('renders nothing without data', () => {
    const c = new LineChart();
    c.points = [];
    expect(c.hasData()).toBe(false);
    expect(c.areaPath()).toBe('');
  });

  it('centres a single point instead of dividing by zero', () => {
    const c = new LineChart();
    c.points = [{ label: 'Mon', value: 5 }];
    const coords = c.linePath();
    expect(coords).not.toContain('NaN');
    expect(coords).toContain((c.width / 2).toFixed(1));
  });

  it('spreads points across the full width', () => {
    const c = new LineChart();
    c.points = [
      { label: 'a', value: 1 },
      { label: 'b', value: 5 },
      { label: 'c', value: 3 }
    ];
    const xs = c.dots().map(d => Math.round(d.x));
    expect(xs[0]).toBeLessThan(xs[1]);
    expect(xs[1]).toBeLessThan(xs[2]);
    expect(c.linePath()).not.toContain('NaN');
  });

  it('puts the peak value at the top of the plot', () => {
    const c = new LineChart();
    c.points = [{ label: 'a', value: 1 }, { label: 'b', value: 10 }];
    const [low, high] = c.dots();
    // Smaller y is higher on screen in SVG.
    expect(high.y).toBeLessThan(low.y);
  });

  it('survives an all-zero series', () => {
    const c = new LineChart();
    c.points = [{ label: 'a', value: 0 }, { label: 'b', value: 0 }];
    expect(c.linePath()).not.toContain('NaN');
    expect(c.total()).toBe(0);
  });

  it('closes the area path back to the baseline', () => {
    const c = new LineChart();
    c.points = [{ label: 'a', value: 2 }, { label: 'b', value: 4 }];
    expect(c.areaPath().trim().endsWith('Z')).toBe(true);
  });
});

describe('BarChart', () => {

  it('scales bars against the largest value', () => {
    const c = new BarChart();
    c.items = [{ label: 'a', count: 10 }, { label: 'b', count: 5 }];
    const bars = c.bars();
    expect(bars[0].percent).toBe(100);
    expect(bars[1].percent).toBe(50);
  });

  it('drops zero and negative counts', () => {
    const c = new BarChart();
    c.items = [{ label: 'a', count: 4 }, { label: 'b', count: 0 }];
    expect(c.bars()).toHaveLength(1);
  });

  it('reports share of total', () => {
    const c = new BarChart();
    c.items = [{ label: 'a', count: 3 }, { label: 'b', count: 1 }];
    expect(c.bars()[0].share).toBe(75);
  });

  it('is empty for no data', () => {
    const c = new BarChart();
    c.items = null;
    expect(c.hasData()).toBe(false);
  });
});

describe('DonutChart', () => {

  it('collapses the long tail into Other', () => {
    const c = new DonutChart();
    c.maxSlices = 2;
    c.items = [
      { label: 'a', count: 5 }, { label: 'b', count: 4 },
      { label: 'c', count: 2 }, { label: 'd', count: 1 }
    ];
    const slices = c.slices();
    expect(slices).toHaveLength(3);
    expect(slices[2].label).toBe('Other');
    expect(slices[2].count).toBe(3);
  });

  it('produces shares that account for the whole circle', () => {
    const c = new DonutChart();
    c.items = [{ label: 'a', count: 1 }, { label: 'b', count: 1 }];
    const dashTotal = c.slices().reduce((sum, s) => sum + s.dash, 0);
    expect(dashTotal).toBeCloseTo(c.circumference, 4);
  });

  it('is empty when every count is zero', () => {
    const c = new DonutChart();
    c.items = [{ label: 'a', count: 0 }];
    expect(c.hasData()).toBe(false);
  });
});
