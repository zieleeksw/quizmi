import { Component, input, output } from '@angular/core';

import { ToastItem } from './toast-stack.models';

@Component({
  selector: 'app-toast-stack',
  templateUrl: './toast-stack.component.html',
  styleUrl: './toast-stack.component.scss'
})
export class ToastStackComponent {
  readonly toasts = input<ToastItem[]>([]);
  readonly dismissed = output<number>();
}
