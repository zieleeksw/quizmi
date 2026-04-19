import { TestBed } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

import { AttemptService } from '../../core/attempts/attempt.service';
import { QuizSessionDto } from '../../core/attempts/attempt.models';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { QuestionDto } from '../../core/questions/question.models';
import { QuestionService } from '../../core/questions/question.service';
import { QuizDto } from '../../core/quizzes/quiz.models';
import { QuizService } from '../../core/quizzes/quiz.service';
import { QuizPlayPageComponent } from './quiz-play-page.component';

describe('QuizPlayPageComponent', () => {
  it('should use the answer order stored in the quiz session', async () => {
    const question: QuestionDto = {
      id: 101,
      courseId: 7,
      currentVersionNumber: 1,
      createdAt: '2026-04-19T10:00:00Z',
      updatedAt: '2026-04-19T10:00:00Z',
      prompt: 'Which steps help protect a refresh flow?',
      explanation: null,
      categories: [],
      answers: [
        { id: 201, displayOrder: 0, content: 'Browser session binding', correct: true },
        { id: 202, displayOrder: 1, content: 'Static file serving', correct: false },
        { id: 203, displayOrder: 2, content: 'Refresh token rotation', correct: true }
      ]
    };
    const session: QuizSessionDto = {
      id: 301,
      courseId: 7,
      quizId: 11,
      userId: 5,
      quizTitle: 'Authentication mastery',
      questionIds: [question.id],
      answerOrderByQuestion: {
        [question.id]: [203, 201, 202]
      },
      currentIndex: 0,
      answers: {},
      updatedAt: '2026-04-19T10:00:00Z'
    };
    const quiz: QuizDto = {
      id: 11,
      courseId: 7,
      active: true,
      currentVersionNumber: 1,
      createdAt: '2026-04-19T10:00:00Z',
      updatedAt: '2026-04-19T10:00:00Z',
      title: 'Authentication mastery',
      mode: 'manual',
      randomCount: null,
      questionOrder: 'fixed',
      answerOrder: 'random',
      questionIds: [question.id],
      categories: [],
      resolvedQuestionCount: 1
    };
    const course: CourseDto = {
      id: 7,
      name: 'Security',
      description: 'Security course',
      createdAt: '2026-04-19T10:00:00Z',
      ownerUserId: 5,
      ownerEmail: 'owner@quizmi.app',
      membershipRole: 'OWNER',
      membershipStatus: 'ACTIVE',
      canAccess: true,
      canManage: true,
      pendingRequestsCount: 0
    };

    await TestBed.configureTestingModule({
      imports: [QuizPlayPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({
                courseId: String(course.id),
                quizId: String(quiz.id)
              })
            }
          }
        },
        {
          provide: CourseService,
          useValue: {
            fetchCourse: () => of(course)
          }
        },
        {
          provide: QuizService,
          useValue: {
            fetchQuiz: () => of(quiz)
          }
        },
        {
          provide: QuestionService,
          useValue: {
            fetchQuestions: () => of([question])
          }
        },
        {
          provide: AttemptService,
          useValue: {
            createOrResumeSession: () => of(session),
            updateSession: () => of(session),
            createAttempt: () => of({
              id: 401,
              courseId: course.id,
              quizId: quiz.id,
              userId: session.userId,
              quizTitle: quiz.title,
              correctAnswers: 0,
              totalQuestions: 1,
              finishedAt: '2026-04-19T10:00:00Z'
            })
          }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(QuizPlayPageComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.currentOptions().map((answer) => answer.id)).toEqual([203, 201, 202]);
  });
});
