# Task 048 - Rich text HTML entity decoding

## Title
avoid double-escaping HTML entities in quiz rich text

## Goal
Keep existing quiz text readable when saved content already contains HTML entities such as `&amp;`.

## Why
After the whitespace preservation change, plain text rich content was safely escaped before rendering. Text that already contained encoded entities could be escaped a second time, so a prompt like `@PostConstruct &amp; @PreDestroy annotated methods:` was displayed with a literal `&amp;` instead of `&`.

## Scope
- [x] decode existing HTML entities in the plain-text sanitization path
- [x] keep supported rich-text HTML handling unchanged
- [x] add a regression test for `@PostConstruct &amp; @PreDestroy`

## Verification
- [x] ran `npm.cmd run build`
- [x] ran `node_modules\.bin\tsc.cmd -p tsconfig.spec.json --noEmit`
- [ ] run focused Karma spec when Chrome or `CHROME_BIN` is available

## Date
2026-05-16
