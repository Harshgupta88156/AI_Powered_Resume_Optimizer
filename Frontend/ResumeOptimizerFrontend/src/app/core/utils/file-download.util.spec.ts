/**
 * @vitest-environment jsdom
 *
 * These are plain TypeScript units, not Angular components, so they run
 * under bare Vitest - but downloadBlob touches document/URL, so it needs a
 * DOM. Component specs go through `ng test` instead, because JIT cannot
 * resolve templateUrl without the Angular build plugin.
 */
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';
import { downloadBlob, downloadText, toSafeFileName } from './file-download.util';

describe('file-download.util', () => {

  let createdUrls: string[];
  let revokedUrls: string[];

  beforeEach(() => {
    vi.useFakeTimers();

    // Earlier tests end with useRealTimers(), which drops their pending cleanup
    // timer and leaves their anchor attached. Start each test from a clean DOM
    // so this spec measures its own anchor, not a previous one's.
    document.body.innerHTML = '';

    createdUrls = [];
    revokedUrls = [];

    URL.createObjectURL = vi.fn(() => {
      const url = `blob:mock/${createdUrls.length}`;
      createdUrls.push(url);
      return url;
    });
    URL.revokeObjectURL = vi.fn((url: string) => { revokedUrls.push(url); });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('attaches the anchor to the document before clicking it', () => {
    // The original bug: a detached anchor is ignored by Firefox and Safari,
    // so the click silently did nothing.
    let wasConnectedAtClickTime: boolean | null = null;

    const realCreate = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tag: string) => {
      const el = realCreate(tag) as HTMLAnchorElement;
      if (tag === 'a') {
        el.click = () => { wasConnectedAtClickTime = el.isConnected; };
      }
      return el;
    });

    downloadBlob(new Blob(['hi']), 'x.md');

    expect(wasConnectedAtClickTime).toBe(true);
  });

  it('does not revoke the object URL on the same tick as the click', () => {
    downloadBlob(new Blob(['hi']), 'x.md');

    // Revoking immediately can destroy the blob before the browser reads it,
    // which produced 0-byte / failed downloads.
    expect(revokedUrls).toHaveLength(0);

    vi.advanceTimersByTime(1000);
    expect(revokedUrls).toEqual(createdUrls);
  });

  it('removes the anchor after the download settles', () => {
    downloadText('content', 'resume.md');
    vi.advanceTimersByTime(1000);

    expect(document.querySelectorAll('a[download]')).toHaveLength(0);
  });

  it('strips characters that are illegal in filenames', () => {
    expect(toSafeFileName('Senior Dev / R&D: "Lead"')).toBe('Senior-Dev-R&D-Lead');
  });

  it('falls back when the name reduces to nothing', () => {
    expect(toSafeFileName('///', 'fallback')).toBe('fallback');
  });
});
