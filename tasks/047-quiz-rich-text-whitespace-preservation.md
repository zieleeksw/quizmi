# Task 047 - Quiz rich text whitespace preservation

## Title
preserve line breaks and indentation in quiz rich-text prompts

## Goal
Keep quiz questions readable when they contain pasted code snippets, line breaks, indentation, or plain-text examples, without changing the intended visual style of the quiz prompt.

## Why
Some quiz prompts were saved with clear spacing and indentation, but the quiz play view rendered them as collapsed inline text. This made code-based questions difficult to read, especially when the prompt contained an exhibit, Java annotations, method declarations, or multiple code lines.

The issue was caused by rich-text rendering treating plain text like HTML output without converting newline characters into visible line breaks. The quiz view also needed CSS that explicitly preserves whitespace for rendered prompt and answer content.

## Scope
- [x] preserve newline characters in sanitized rich-text output
- [x] preserve indentation and repeated spaces in quiz prompt rendering
- [x] keep the existing italic/display prompt style
- [x] apply the same whitespace resilience to answer, explanation, and review text
- [x] add a focused regression test for plain text with line breaks
- [x] avoid changing backend question storage or quiz DTOs

## Steps
- [x] update `sanitizeRichTextHtml` so plain text is escaped as text instead of parsed as unsupported HTML
- [x] convert text-node newline characters into `<br>` nodes during sanitization
- [x] add `white-space: pre-wrap` and `overflow-wrap: anywhere` to quiz play prompt rendering
- [x] add equivalent whitespace handling to quiz review prompt and rich answer text
- [x] add `rich-text.utils.spec.ts` coverage for line breaks, unsupported angle brackets, and supported inline markup

## Decisions
- the quiz prompt keeps the existing display font and italic style because the issue was whitespace collapse, not typography
- sanitization remains centralized so existing question data benefits without migration
- unsupported angle-bracket text such as `List<String>` is treated as plain text to avoid accidental HTML parsing
- supported rich-text tags remain allowed so existing formatted questions continue to render correctly

## Verification
- [x] ran `npm.cmd run build`
- [x] ran `node_modules/.bin/tsc.cmd -p tsconfig.spec.json --noEmit`
- [ ] run the focused Karma spec in a browser environment where Chrome or a stable headless Chromium is available
- [ ] manually verify a quiz question containing a multi-line code exhibit in the quiz play UI

## Date
2026-05-13
