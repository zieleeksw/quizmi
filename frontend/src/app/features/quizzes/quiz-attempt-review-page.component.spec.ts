import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { QuizAttemptDetailDto } from '../../core/attempts/attempt.models';
import { AttemptService } from '../../core/attempts/attempt.service';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { QuestionDto } from '../../core/questions/question.models';
import { QuestionService } from '../../core/questions/question.service';
import { QuizAttemptReviewPageComponent } from './quiz-attempt-review-page.component';

describe('QuizAttemptReviewPageComponent', () => {
  const course: CourseDto = {
    id: 7,
    name: 'Security',
    description: 'Security course',
    createdAt: '2026-04-20T20:00:00Z',
    ownerUserId: 5,
    ownerEmail: 'owner@quizmi.app',
    membershipRole: 'OWNER',
    membershipStatus: 'ACTIVE',
    canAccess: true,
    canManage: true,
    pendingRequestsCount: 0
  };

  const question: QuestionDto = {
    id: 101,
    courseId: course.id,
    currentVersionNumber: 1,
    createdAt: '2026-04-20T20:00:00Z',
    updatedAt: '2026-04-20T20:00:00Z',
    prompt: 'Which options are valid?',
    explanation: 'Fallback explanation',
    categories: [
      { id: 1, name: 'Security' },
      { id: 2, name: 'Spring Data' }
    ],
    answers: [
      { id: 201, displayOrder: 0, content: 'Option A', correct: true },
      { id: 202, displayOrder: 1, content: 'Option B', correct: false }
    ]
  };

  const attempt: QuizAttemptDetailDto = {
    id: 501,
    courseId: course.id,
    quizId: 11,
    userId: 5,
    quizTitle: 'Random Quiz around all questions',
    correctAnswers: 1,
    totalQuestions: 1,
    finishedAt: '2026-04-20T21:22:00Z',
    questions: [
      {
        questionId: question.id,
        prompt: question.prompt,
        explanation: 'Review explanation',
        selectedAnswerIds: [201],
        correctAnswerIds: [201],
        answeredCorrectly: true,
        answers: question.answers
      }
    ]
  };

  async function configureComponent(
    attemptOverride: QuizAttemptDetailDto = attempt,
    questionOverride: QuestionDto = question
  ) {
    await TestBed.configureTestingModule({
      imports: [QuizAttemptReviewPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({
                courseId: String(course.id),
                attemptId: String(attempt.id)
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
          provide: AttemptService,
          useValue: {
            fetchAttemptDetail: () => of(attemptOverride)
          }
        },
        {
          provide: QuestionService,
          useValue: {
            fetchQuestions: () => of([questionOverride])
          }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(QuizAttemptReviewPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('should show a collapsed details toggle and reveal categories with explanation after expanding', async () => {
    const fixture = await configureComponent();

    const detailsToggle = fixture.nativeElement.querySelector('.quiz-review-card__details-toggle') as HTMLButtonElement | null;

    expect(detailsToggle).not.toBeNull();
    expect(detailsToggle?.textContent).toContain('Question details');
    expect(fixture.nativeElement.querySelector('.quiz-review-card__details-body')).toBeNull();

    detailsToggle?.click();
    fixture.detectChanges();

    const detailsBody = fixture.nativeElement.querySelector('.quiz-review-card__details-body') as HTMLElement | null;

    expect(detailsBody).not.toBeNull();
    expect(detailsBody?.textContent).toContain('Categories');
    expect(detailsBody?.textContent).toContain('Security');
    expect(detailsBody?.textContent).toContain('Spring Data');
    expect(detailsBody?.textContent).toContain('Explanation');
    expect(detailsBody?.textContent).toContain('Review explanation');
  });

  it('should hide the details toggle when a reviewed question has neither categories nor explanation', async () => {
    const fixture = await configureComponent(
      {
        ...attempt,
        questions: [
          {
            ...attempt.questions[0],
            explanation: null
          }
        ]
      },
      {
        ...question,
        explanation: null,
        categories: []
      }
    );

    expect(fixture.nativeElement.querySelector('.quiz-review-card__details')).toBeNull();
  });
});
