/**
 * Minimal Markdown -> HTML converter for the resume preview and print view.
 *
 * Deliberately not a full parser: the generation prompt constrains output to
 * headings, lists, paragraphs, bold/italic, links and a single rule, so pulling
 * in a markdown library would add ~40KB to the bundle for no benefit.
 *
 * Everything is HTML-escaped before any markup is generated, so resume content
 * containing `<` or `&` (C++ <algorithm>, R&D, generics) can never inject markup.
 *
 * Two structural classes are emitted so the stylesheet can typeset a real
 * resume rather than a generic article:
 *
 *   .contact — the single line directly under the name
 *   .meta    — the italic "Location · Jan 2022 – Present" line under an entry
 *
 * Both are inferred from position and shape, so a model that omits them
 * degrades to ordinary paragraphs instead of breaking the layout.
 */

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/** Inline formatting, applied to already-escaped text. */
function inline(text: string): string {
  return text
    // [label](https://example.com)
    .replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2">$1</a>')
    // `code`
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // **bold** before *italic* so the single-star rule can't eat it
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_]+)__/g, '<strong>$1</strong>')
    .replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>')
    // Bare URLs, but not ones already inside an href from the rule above
    .replace(/(^|[\s(])(https?:\/\/[^\s<)]+)/g, '$1<a href="$2">$2</a>');
}

/** A line that is entirely wrapped in a single pair of asterisks. */
function isItalicOnly(line: string): boolean {
  return /^\*[^*]+\*$/.test(line.trim());
}

export function markdownToHtml(markdown: string): string {
  if (!markdown) return '';

  const lines = escapeHtml(markdown).replace(/\r\n/g, '\n').split('\n');
  const html: string[] = [];

  let inList = false;
  let listTag: 'ul' | 'ol' = 'ul';

  // Tracks what was emitted last, so the contact and meta lines can be
  // identified by their position relative to a heading.
  let lastBlock: 'h1' | 'entry-heading' | 'other' = 'other';
  let seenContact = false;

  const closeList = () => {
    if (inList) {
      html.push(`</${listTag}>`);
      inList = false;
    }
  };

  const openList = (tag: 'ul' | 'ol') => {
    if (inList && listTag !== tag) closeList();
    if (!inList) {
      listTag = tag;
      html.push(`<${tag}>`);
      inList = true;
    }
  };

  for (const rawLine of lines) {
    const line = rawLine.trimEnd();

    if (!line.trim()) {
      closeList();
      continue;
    }

    // Horizontal rule
    if (/^(-{3,}|\*{3,}|_{3,})$/.test(line.trim())) {
      closeList();
      html.push('<hr />');
      // A rule between the contact line and the first section must not make
      // the next paragraph look like an entry meta line.
      lastBlock = 'other';
      continue;
    }

    // Headings, deepest first so `###` isn't matched by the `#` rule
    const heading = /^(#{1,6})\s+(.*)$/.exec(line);
    if (heading) {
      closeList();
      const level = heading[1].length;
      html.push(`<h${level}>${inline(heading[2])}</h${level}>`);
      lastBlock = level === 1 ? 'h1' : level >= 3 ? 'entry-heading' : 'other';
      continue;
    }

    // Ordered list
    const ordered = /^\s*\d+[.)]\s+(.*)$/.exec(line);
    if (ordered) {
      openList('ol');
      html.push(`<li>${inline(ordered[1])}</li>`);
      lastBlock = 'other';
      continue;
    }

    // Unordered list
    const unordered = /^\s*[-*+]\s+(.*)$/.exec(line);
    if (unordered) {
      openList('ul');
      html.push(`<li>${inline(unordered[1])}</li>`);
      lastBlock = 'other';
      continue;
    }

    closeList();

    const text = line.trim();

    // The single line under the name is the contact block.
    if (lastBlock === 'h1' && !seenContact) {
      html.push(`<p class="contact">${inline(text)}</p>`);
      seenContact = true;
      lastBlock = 'other';
      continue;
    }

    // An all-italic line under a job/project/education heading is its meta line.
    if (lastBlock === 'entry-heading' && isItalicOnly(text)) {
      html.push(`<p class="meta">${inline(text)}</p>`);
      lastBlock = 'other';
      continue;
    }

    html.push(`<p>${inline(text)}</p>`);
    lastBlock = 'other';
  }

  closeList();
  return html.join('\n');
}

/**
 * Print/preview stylesheet, shared by the in-app preview and the PDF export so
 * what the user sees on screen is what lands in the file.
 *
 * Fixed light colours on purpose: a resume is printed on white paper and is
 * shared as a PDF, so it must not follow the app's dark theme.
 */
export const RESUME_STYLESHEET = `
  .resume-doc {
    font-family: Georgia, 'Times New Roman', serif;
    color: #1a1a1a;
    line-height: 1.45;
    font-size: 10.8pt;
    max-width: 210mm;
    margin: 0 auto;
    background: #ffffff;
    text-align: left;
  }

  .resume-doc h1 {
    font-size: 21pt;
    font-weight: 700;
    letter-spacing: .06em;
    text-transform: uppercase;
    text-align: center;
    margin: 0 0 6px;
    color: #111;
  }

  .resume-doc .contact {
    text-align: center;
    font-family: Arial, Helvetica, sans-serif;
    font-size: 9pt;
    color: #444;
    margin: 0 0 10px;
    line-height: 1.5;
  }

  .resume-doc .contact a { color: #444; text-decoration: none; }

  .resume-doc hr {
    border: 0;
    border-top: 1.5px solid #111;
    margin: 0 0 14px;
  }

  .resume-doc h2 {
    font-family: Arial, Helvetica, sans-serif;
    font-size: 10.5pt;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: .09em;
    color: #111;
    margin: 16px 0 7px;
    padding-bottom: 3px;
    border-bottom: 1px solid #bbb;
  }

  .resume-doc h2:first-of-type { margin-top: 0; }

  .resume-doc h3 {
    font-size: 11.2pt;
    font-weight: 700;
    color: #111;
    margin: 11px 0 1px;
  }

  .resume-doc .meta {
    font-family: Arial, Helvetica, sans-serif;
    font-size: 8.9pt;
    color: #555;
    margin: 0 0 5px;
  }

  .resume-doc .meta em { font-style: normal; }

  .resume-doc p { margin: 0 0 8px; }

  .resume-doc ul, .resume-doc ol {
    margin: 0 0 9px;
    padding-left: 17px;
  }

  .resume-doc li { margin-bottom: 3px; }

  .resume-doc li::marker { color: #666; }

  .resume-doc strong { color: #111; }

  .resume-doc a { color: #1a1a1a; }

  .resume-doc code {
    font-family: 'Courier New', monospace;
    font-size: 9.5pt;
  }

  /* Never strand a heading at the foot of a page. */
  .resume-doc h1,
  .resume-doc h2,
  .resume-doc h3 { page-break-after: avoid; break-after: avoid; }
  .resume-doc li,
  .resume-doc p  { page-break-inside: avoid; break-inside: avoid; }
`;

/** Full standalone HTML document, used for printing and the .html export. */
export function buildResumeDocument(markdown: string, title: string): string {
  return `<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>${escapeHtml(title)}</title>
    <style>
      @page { margin: 14mm 15mm; }
      html, body { margin: 0; padding: 0; background: #fff; }
      body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
      ${RESUME_STYLESHEET}
      @media screen {
        body { padding: 20px; background: #f1f5f9; }
        .resume-doc { padding: 18mm 16mm; box-shadow: 0 2px 12px rgba(0,0,0,.15); }
      }
    </style>
  </head>
  <body><div class="resume-doc">${markdownToHtml(markdown)}</div></body>
</html>`;
}
