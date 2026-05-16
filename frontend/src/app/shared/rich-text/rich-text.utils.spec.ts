import { extractRichTextPlainText, sanitizeRichTextHtml } from './rich-text.utils';

describe('rich text utils', () => {
  it('preserves line breaks from plain text prompts', () => {
    const result = sanitizeRichTextHtml('Refer to the exhibit.\n@Configuration\n  return service;');

    expect(result).toBe('Refer to the exhibit.<br>@Configuration<br>  return service;');
  });

  it('treats unsupported angle brackets in plain text as text', () => {
    const result = sanitizeRichTextHtml('List<String> values');

    expect(result).toBe('List&lt;String&gt; values');
    expect(extractRichTextPlainText(result)).toBe('List<String> values');
  });

  it('does not double-escape existing HTML entities in plain text', () => {
    const result = sanitizeRichTextHtml('@PostConstruct &amp; @PreDestroy annotated methods:');

    expect(result).toBe('@PostConstruct &amp; @PreDestroy annotated methods:');
    expect(extractRichTextPlainText(result)).toBe('@PostConstruct & @PreDestroy annotated methods:');
  });

  it('keeps supported inline rich text markup', () => {
    const result = sanitizeRichTextHtml('<strong>Correct</strong><br><em>because</em>');

    expect(result).toBe('<strong>Correct</strong><br><em>because</em>');
  });
});
