import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-action-button',
  imports: [RouterLink],
  templateUrl: './action-button.component.html',
  styleUrl: './action-button.component.scss'
})
export class ActionButtonComponent {
  readonly label = input.required<string>();
  readonly helper = input<string>('');
  readonly routerLink = input<string | unknown[] | null>(null);
  readonly tone = input<'primary' | 'secondary'>('primary');
  readonly size = input<'regular' | 'compact'>('regular');
  readonly nativeType = input<'button' | 'submit'>('button');
  readonly disabled = input(false);
  readonly clicked = output<void>();
}
