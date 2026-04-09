import { Component, inject } from '@angular/core';

import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-workspace-topbar',
  templateUrl: './workspace-topbar.component.html',
  styleUrl: './workspace-topbar.component.scss'
})
export class WorkspaceTopbarComponent {
  private readonly authService = inject(AuthService);

  readonly user = this.authService.user;

  logout(): void {
    this.authService.logout();
  }
}
