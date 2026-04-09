import { Component, inject } from '@angular/core';

import { AuthService } from '../../core/auth/auth.service';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { AuthCardComponent } from '../../shared/ui/auth-card/auth-card.component';

@Component({
  selector: 'app-session-page',
  imports: [ActionButtonComponent, AuthCardComponent],
  templateUrl: './session-page.component.html',
  styleUrl: './session-page.component.scss'
})
export class SessionPageComponent {
  private readonly authService = inject(AuthService);

  readonly user = this.authService.user;
  readonly session = this.authService.session;

  logout(): void {
    this.authService.logout();
  }
}
