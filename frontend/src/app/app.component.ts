import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { PendingChangesDialogComponent } from './shared/ui/pending-changes-dialog/pending-changes-dialog.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, PendingChangesDialogComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {}
