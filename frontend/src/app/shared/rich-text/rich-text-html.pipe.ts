import { Pipe, PipeTransform } from '@angular/core';

import { sanitizeRichTextHtml } from './rich-text.utils';

@Pipe({
  name: 'richTextHtml',
  standalone: true
})
export class RichTextHtmlPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return sanitizeRichTextHtml(value);
  }
}
