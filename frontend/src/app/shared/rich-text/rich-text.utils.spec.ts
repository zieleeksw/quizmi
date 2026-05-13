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

  it('keeps supported inline rich text markup', () => {
    const result = sanitizeRichTextHtml('<strong>Correct</strong><br><em>because</em>');

    expect(result).toBe('<strong>Correct</strong><br><em>because</em>');
  });
});
