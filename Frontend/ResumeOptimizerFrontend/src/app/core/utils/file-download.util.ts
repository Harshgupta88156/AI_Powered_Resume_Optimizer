/**
 * Triggers a browser download for in-memory content.
 *
 * The obvious four-liner — create an anchor, set `href`, call `click()`, then
 * `URL.revokeObjectURL()` — is what most "download doesn't work" bugs turn out
 * to be. Two things break it:
 *
 *  1. The anchor is never added to the document. Chrome tolerates a detached
 *     anchor; Firefox and Safari ignore the synthetic click entirely, so
 *     nothing happens and no error is thrown.
 *  2. `revokeObjectURL` is called on the same tick as `click()`. The download is
 *     handed off asynchronously, so revoking immediately can kill the blob
 *     before the browser has read it — producing a 0-byte or "failed" download.
 *
 * This helper attaches the anchor, defers the revoke, and always cleans up.
 */
export function downloadBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');

  anchor.href = url;
  anchor.download = fileName;
  anchor.rel = 'noopener';
  anchor.style.display = 'none';

  document.body.appendChild(anchor);

  try {
    anchor.click();
  } finally {
    // Give the browser a tick to start reading the blob before tearing it down.
    setTimeout(() => {
      URL.revokeObjectURL(url);
      anchor.remove();
    }, 1000);
  }
}

/** Convenience wrapper for text payloads (markdown, csv, json...). */
export function downloadText(
  content: string,
  fileName: string,
  mimeType = 'text/plain;charset=utf-8'
): void {
  // The BOM keeps Windows editors from mangling non-ASCII characters in the
  // generated resume (accented names, en-dashes, bullet glyphs).
  downloadBlob(new Blob(['\ufeff', content], { type: mimeType }), fileName);
}

/**
 * Turns a free-text label into something safe for a filename on every OS.
 * Windows rejects \ / : * ? " < > | outright; the rest just look bad.
 */
export function toSafeFileName(value: string, fallback = 'download'): string {
  const cleaned = value
    .normalize('NFKD')
    .replace(/[\\/:*?"<>|]+/g, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^[-.]+|[-.]+$/g, '')
    .slice(0, 80)
    .trim();

  return cleaned || fallback;
}
