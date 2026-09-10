/**
 * @vitest-environment jsdom
 *
 * Pure logic tests — the windowing and range maths, which is where paging bugs
 * actually live. Rendering is covered by the Angular component specs.
 */
// Instantiating a decorated component outside TestBed needs the JIT compiler;
// without it @angular/common's static initialisers throw.
import '@angular/compiler';
import { describe, expect, it, vi } from 'vitest';
import { Pagination } from './pagination';

function make(page: number, totalPages: number, totalElements?: number, pageSize = 20) {
  const c = new Pagination();
  c.page = page;
  c.totalPages = totalPages;
  c.pageSize = pageSize;
  if (totalElements != null) c.totalElements = totalElements;
  return c;
}

describe('Pagination', () => {

  it('is hidden for a single page', () => {
    expect(make(0, 1).visible()).toBe(false);
    expect(make(0, 0).visible()).toBe(false);
  });

  it('lists every page when there are few', () => {
    expect(make(0, 5).pages()).toEqual([0, 1, 2, 3, 4]);
  });

  it('windows the middle and keeps first and last', () => {
    const pages = make(10, 20).pages();
    expect(pages[0]).toBe(0);
    expect(pages[pages.length - 1]).toBe(19);
    expect(pages).toContain(10);
    // Gaps on both sides, so the control stays a fixed width.
    expect(pages.filter(p => p === -1)).toHaveLength(2);
  });

  it('does not open a gap next to the first page', () => {
    const pages = make(1, 20).pages();
    expect(pages.slice(0, 3)).toEqual([0, 1, 2]);
  });

  it('does not open a gap next to the last page', () => {
    const pages = make(18, 20).pages();
    expect(pages.slice(-3)).toEqual([17, 18, 19]);
  });

  it('computes the item range shown', () => {
    expect(make(0, 5, 87)!.range()).toEqual({ from: 1, to: 20, total: 87 });
    expect(make(4, 5, 87)!.range()).toEqual({ from: 81, to: 87, total: 87 });
  });

  it('has no range when the total is unknown', () => {
    expect(make(0, 5).range()).toBeNull();
  });

  it('emits only for a real, in-bounds page change', () => {
    const c = make(2, 5);
    const spy = vi.fn();
    c.pageChange.subscribe(spy);

    c.go(-1);        // below range
    c.go(5);         // above range
    c.go(2);         // already there
    c.go(c.gap);     // the ellipsis
    expect(spy).not.toHaveBeenCalled();

    c.go(3);
    expect(spy).toHaveBeenCalledWith(3);
  });
});
