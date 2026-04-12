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
import { CourseQuestionsPageComponent } from './features/questions/course-questions-page.component';
import { QuestionEditorPageComponent } from './features/questions/question-editor-page.component';
import { CourseQuizzesPageComponent } from './features/quizzes/course-quizzes-page.component';
import { QuizAttemptReviewPageComponent } from './features/quizzes/quiz-attempt-review-page.component';
import { QuizEditorPageComponent } from './features/quizzes/quiz-editor-page.component';
import { QuizOverviewPageComponent } from './features/quizzes/quiz-overview-page.component';
import { QuizPlayPageComponent } from './features/quizzes/quiz-play-page.component';
import { QuizStatisticsPageComponent } from './features/quizzes/quiz-statistics-page.component';

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
    path: 'courses/:courseId/questions/new',
    canActivate: [authGuard],
    canDeactivate: [pendingChangesGuard],
    component: QuestionEditorPageComponent
  },
  {
    path: 'courses/:courseId/questions/:questionId/edit',
    canActivate: [authGuard],
    canDeactivate: [pendingChangesGuard],
    component: QuestionEditorPageComponent
  },
  {
    path: 'courses/:courseId',
    canActivate: [authGuard],
    component: CourseDetailsPageComponent,
    children: [
      {
        path: '',
        redirectTo: 'quizzes',
        pathMatch: 'full'
      },
      {
        path: 'quizzes',
        component: CourseQuizzesPageComponent
      },
      {
        path: 'questions',
        component: CourseQuestionsPageComponent
      },
      {
        path: 'categories',
        component: CourseCategoriesPageComponent
      }
    ]
  },
  {
    path: 'courses/:courseId/quizzes/new',
    canActivate: [authGuard],
    canDeactivate: [pendingChangesGuard],
    component: QuizEditorPageComponent
  },
  {
    path: 'courses/:courseId/quizzes/:quizId/edit',
    canActivate: [authGuard],
    canDeactivate: [pendingChangesGuard],
    component: QuizEditorPageComponent
  },
  {
    path: 'courses/:courseId/quizzes/:quizId/statistics',
    canActivate: [authGuard],
    component: QuizStatisticsPageComponent
  },
  {
    path: 'courses/:courseId/quizzes/:quizId/play',
    canActivate: [authGuard],
    component: QuizPlayPageComponent
  },
  {
    path: 'courses/:courseId/quizzes/:quizId',
    canActivate: [authGuard],
    component: QuizOverviewPageComponent
  },
  {
    path: 'courses/:courseId/attempts/:attemptId',
    canActivate: [authGuard],
    component: QuizAttemptReviewPageComponent
  },
  {
    path: 'courses/:courseId/edit',
    canActivate: [authGuard],
    component: CourseEditPageComponent
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
