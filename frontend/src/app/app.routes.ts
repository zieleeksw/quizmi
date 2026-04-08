import { Routes } from '@angular/router';

import { RegisterPageComponent } from './features/auth/register-page.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'register',
    pathMatch: 'full'
  },
  {
    path: 'register',
    component: RegisterPageComponent
  },
  {
    path: '**',
    redirectTo: 'register'
  }
];
