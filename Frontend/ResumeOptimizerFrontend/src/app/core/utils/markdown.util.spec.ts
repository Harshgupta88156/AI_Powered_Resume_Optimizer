/**
 * @vitest-environment jsdom
 *
 * These are plain TypeScript units, not Angular components, so they run
 * under bare Vitest - but downloadBlob touches document/URL, so it needs a
 * DOM. Component specs go through `ng test` instead, because JIT cannot
 * resolve templateUrl without the Angular build plugin.
 */
import { describe, expect, it } from 'vitest';
import { buildResumeDocument, markdownToHtml } from './markdown.util';

describe('markdownToHtml', () => {

  it('escapes HTML before generating markup', () => {
    // Resume content legitimately contains < and &, e.g. "C++ <algorithm>".
    const html = markdownToHtml('Used <script>alert(1)</script> & more');
    expect(html).not.toContain('<script>');
    expect(html).toContain('&lt;script&gt;');
    expect(html).toContain('&amp;');
  });

  it('renders headings at the right level', () => {
    expect(markdownToHtml('# Jane')).toBe('<h1>Jane</h1>');
    expect(markdownToHtml('## Experience')).toBe('<h2>Experience</h2>');
    expect(markdownToHtml('### Acme')).toBe('<h3>Acme</h3>');
  });

  it('groups consecutive bullets into a single list', () => {
    const html = markdownToHtml('- one\n- two\n- three');
    expect(html.match(/<ul>/g)).toHaveLength(1);
    expect(html.match(/<li>/g)).toHaveLength(3);
  });

  it('closes the list when prose follows', () => {
    const html = markdownToHtml('- one\n\nSome prose');
    expect(html).toContain('</ul>');
    expect(html).toContain('<p>Some prose</p>');
  });

  it('renders bold without being eaten by the italic rule', () => {
    expect(markdownToHtml('**Lead Engineer**')).toContain('<strong>Lead Engineer</strong>');
  });

  it('renders markdown links', () => {
    expect(markdownToHtml('[GitHub](https://github.com/me)'))
      .toContain('<a href="https://github.com/me">GitHub</a>');
  });

  it('handles ordered lists', () => {
    const html = markdownToHtml('1. first\n2. second');
    expect(html).toContain('<ol>');
    expect(html.match(/<li>/g)).toHaveLength(2);
  });

  it('returns an empty string for empty input', () => {
    expect(markdownToHtml('')).toBe('');
  });

  // ── Resume-specific structure ───────────────────────────────────────────

  it('tags the line under the name as the contact block', () => {
    const html = markdownToHtml('# Jane Doe\n\nMumbai · jane@mail.com · github.com/jane');
    expect(html).toContain('<p class="contact">');
    expect(html).toContain('jane@mail.com');
  });

  it('only treats the first paragraph after the name as contact', () => {
    const html = markdownToHtml('# Jane\n\nMumbai · jane@mail.com\n\nSome summary text');
    expect(html.match(/class="contact"/g)).toHaveLength(1);
    expect(html).toContain('<p>Some summary text</p>');
  });

  it('tags an all-italic line under an entry heading as meta', () => {
    const html = markdownToHtml('### Backend Engineer — Acme\n\n*Mumbai · Jan 2022 – Present*');
    expect(html).toContain('<p class="meta">');
  });

  it('does not treat ordinary prose under an entry heading as meta', () => {
    const html = markdownToHtml('### Backend Engineer\n\nBuilt the payments service.');
    expect(html).not.toContain('class="meta"');
  });

  it('does not treat a paragraph after a rule as an entry meta line', () => {
    // The rule sits between the contact line and the first section; without
    // resetting state the next italic line would be mislabelled.
    const html = markdownToHtml('### Role\n\n---\n\n*Not a meta line*');
    expect(html).not.toContain('class="meta"');
  });

  it('does not tag a contact line when there is no H1', () => {
    const html = markdownToHtml('Just a paragraph');
    expect(html).toBe('<p>Just a paragraph</p>');
  });

  it('builds a standalone document with the resume wrapper and title', () => {
    const doc = buildResumeDocument('# Jane Doe', 'jane-resume');
    expect(doc).toContain('<!doctype html>');
    expect(doc).toContain('<div class="resume-doc">');
    expect(doc).toContain('<title>jane-resume</title>');
    expect(doc).toContain('@page');
  });

  it('escapes the title in the generated document', () => {
    const doc = buildResumeDocument('# X', '</title><script>bad()</script>');
    expect(doc).not.toContain('<script>bad()');
    expect(doc).toContain('&lt;script&gt;');
  });
});
