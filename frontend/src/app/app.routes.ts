import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';
import { pendingChangesGuard } from './core/navigation/pending-changes.guard';
import { LoginPageComponent } from './features/auth/login-page.component';
import { RegisterPageComponent } from './features/auth/register-page.component';
import { SessionPageComponent } from './features/auth/session-page.component';
import { CategoryCreatePageComponent } from './features/categories/category-create-page.component';
import { CategoryEditPageComponent } from './features/categories/category-edit-page.component';
import { CourseCategoriesPageComponent } from './features/categories/course-categories-page.component';
import { CourseCreatePageComponent } from './features/courses/course-create-page.component';
import { CourseDetailsPageComponent } from './features/courses/course-details-page.component';
import { CourseEditPageComponent } from './features/courses/course-edit-page.component';
import { CoursesPageComponent } from './features/courses/courses-page.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    component: RegisterPageComponent
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    component: LoginPageComponent
  },
  {
    path: 'app',
    redirectTo: 'courses',
    pathMatch: 'full'
  },
  {
    path: 'courses',
    canActivate: [authGuard],
    component: CoursesPageComponent
  },
  {
    path: 'courses/create',
    canActivate: [authGuard],
    component: CourseCreatePageComponent
  },
  {
    path: 'courses/:courseId/categories/create',
    canActivate: [authGuard],
    component: CategoryCreatePageComponent
  },
  {
    path: 'courses/:courseId/categories/:categoryId/edit',
    canActivate: [authGuard],
    canDeactivate: [pendingChangesGuard],
    component: CategoryEditPageComponent
  },
  {
    path: 'courses/:courseId/categories',
    canActivate: [authGuard],
    component: CourseCategoriesPageComponent
  },
  {
    path: 'courses/:courseId/edit',
    canActivate: [authGuard],
    component: CourseEditPageComponent
  },
  {
    path: 'courses/:courseId',
    canActivate: [authGuard],
    component: CourseDetailsPageComponent
  },
  {
    path: 'session',
    canActivate: [authGuard],
    component: SessionPageComponent
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
