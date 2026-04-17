const INLINE_TAGS = new Set(['strong', 'b', 'em', 'i', 'u', 'span']);
const BLOCK_TAGS = new Set(['div', 'p', 'section', 'article']);
const LIST_TAGS = new Set(['ul', 'ol']);
const COLOR_PATTERN = /^(#[0-9a-f]{6}|#[0-9a-f]{3}|rgb\(\s*(?:[01]?\d?\d|2[0-4]\d|25[0-5])\s*,\s*(?:[01]?\d?\d|2[0-4]\d|25[0-5])\s*,\s*(?:[01]?\d?\d|2[0-4]\d|25[0-5])\s*\))$/i;

export function sanitizeRichTextHtml(value: string | null | undefined): string {
  if (!value?.trim()) {
    return '';
  }

  const source = document.createElement('div');
  source.innerHTML = value;

  const target = document.createElement('div');
  sanitizeChildren(source, target);

  return normalizeRichTextHtml(target.innerHTML);
}

export function extractRichTextPlainText(value: string | null | undefined): string {
  const sanitized = sanitizeRichTextHtml(value);

  if (!sanitized) {
    return '';
  }

  const source = document.createElement('div');
  source.innerHTML = sanitized;

  return collectPlainText(source).replace(/\u00a0/g, ' ').trim();
}

function sanitizeChildren(source: Node, target: HTMLElement | DocumentFragment): void {
  source.childNodes.forEach((node) => {
    sanitizeNode(node).forEach((sanitizedNode) => target.appendChild(sanitizedNode));
  });
}

function sanitizeNode(node: Node): Node[] {
  if (node.nodeType === Node.TEXT_NODE) {
    return [document.createTextNode(node.textContent ?? '')];
  }

  if (node.nodeType !== Node.ELEMENT_NODE) {
    return [];
  }

  const element = node as HTMLElement;
  const tagName = element.tagName.toLowerCase();

  if (tagName === 'br') {
    return [document.createElement('br')];
  }

  if (tagName === 'font') {
    const span = document.createElement('span');
    const color = normalizeColor(element.getAttribute('color'));

    if (color) {
      span.style.color = color;
    }

    sanitizeChildren(element, span);
    return isMeaningfulElement(span) ? [span] : [];
  }

  if (tagName === 'li') {
    const fragment = document.createDocumentFragment();
    fragment.appendChild(document.createTextNode('• '));
    sanitizeChildren(element, fragment);
    fragment.appendChild(document.createElement('br'));
    return Array.from(fragment.childNodes);
  }

  if (LIST_TAGS.has(tagName)) {
    const fragment = document.createDocumentFragment();
    sanitizeChildren(element, fragment);
    return Array.from(fragment.childNodes);
  }

  if (BLOCK_TAGS.has(tagName)) {
    const fragment = document.createDocumentFragment();
    sanitizeChildren(element, fragment);

    if (fragment.childNodes.length) {
      fragment.appendChild(document.createElement('br'));
    }

    return Array.from(fragment.childNodes);
  }

  if (INLINE_TAGS.has(tagName)) {
    const nextTag = normalizeInlineTag(tagName);
    const sanitizedElement = document.createElement(nextTag);
    let hasOwnFormatting = nextTag !== 'span';

    if (nextTag === 'span') {
      hasOwnFormatting = applySupportedSpanStyles(element, sanitizedElement);
    }

    sanitizeChildren(element, sanitizedElement);

    if (!hasOwnFormatting) {
      return Array.from(sanitizedElement.childNodes);
    }

    return isMeaningfulElement(sanitizedElement) ? [sanitizedElement] : [];
  }

  const fragment = document.createDocumentFragment();
  sanitizeChildren(element, fragment);
  return Array.from(fragment.childNodes);
}

function normalizeInlineTag(tagName: string): string {
  switch (tagName) {
    case 'b':
      return 'strong';
    case 'i':
      return 'em';
    default:
      return tagName;
  }
}

function normalizeColor(rawValue: string | null | undefined): string | null {
  const value = rawValue?.trim() ?? '';

  if (!value || !COLOR_PATTERN.test(value)) {
    return null;
  }

  return value;
}

function isMeaningfulElement(element: HTMLElement): boolean {
  return element.innerHTML.trim().length > 0;
}

function applySupportedSpanStyles(source: HTMLElement, target: HTMLElement): boolean {
  let hasFormatting = false;
  const color = normalizeColor(source.style.color || source.getAttribute('color'));
  const fontWeight = source.style.fontWeight.trim();
  const fontStyle = source.style.fontStyle.trim();
  const textDecoration = source.style.textDecoration.trim();

  if (color) {
    target.style.color = color;
    hasFormatting = true;
  }

  if (fontWeight === 'bold' || Number.parseInt(fontWeight, 10) >= 600) {
    target.style.fontWeight = '700';
    hasFormatting = true;
  }

  if (fontStyle === 'italic') {
    target.style.fontStyle = 'italic';
    hasFormatting = true;
  }

  if (textDecoration.includes('underline')) {
    target.style.textDecoration = 'underline';
    hasFormatting = true;
  }

  return hasFormatting;
}

function normalizeRichTextHtml(value: string): string {
  return value
    .replace(/(?:<br\s*\/?>\s*){3,}/gi, '<br><br>')
    .replace(/^(?:<br\s*\/?>\s*)+/i, '')
    .replace(/(?:<br\s*\/?>\s*)+$/i, '')
    .trim();
}

function collectPlainText(node: Node): string {
  if (node.nodeType === Node.TEXT_NODE) {
    return node.textContent ?? '';
  }

  if (node.nodeType !== Node.ELEMENT_NODE) {
    return '';
  }

  const element = node as HTMLElement;
  const tagName = element.tagName.toLowerCase();
  const parts = Array.from(element.childNodes).map((child) => collectPlainText(child));
  const text = parts.join('');

  if (tagName === 'br') {
    return '\n';
  }

  if (tagName === 'div' || tagName === 'p' || tagName === 'section' || tagName === 'article' || tagName === 'li') {
    return `${text}\n`;
  }

  return text;
}
