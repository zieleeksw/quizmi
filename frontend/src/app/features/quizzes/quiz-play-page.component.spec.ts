import { ComponentFixture, TestBed, fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { Subject, of } from 'rxjs';

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

  async function configureComponent(
    updateSession: jasmine.Spy | (() => unknown) = () => of(session),
    questionOverride: QuestionDto = question,
    sessionOverride: QuizSessionDto = session
  ) {
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
            fetchQuestions: () => of([questionOverride])
          }
        },
        {
          provide: AttemptService,
          useValue: {
            createOrResumeSession: () => of(sessionOverride),
            updateSession,
            createAttempt: () => of({
              id: 401,
              courseId: course.id,
              quizId: quiz.id,
              userId: sessionOverride.userId,
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
    return fixture;
  }

  it('should use the answer order stored in the quiz session', async () => {
    const fixture = await configureComponent();

    expect(fixture.componentInstance.currentOptions().map((answer) => answer.id)).toEqual([203, 201, 202]);
  });

  it('should batch rapid answer toggles into a single session save', fakeAsync(() => {
    const updatedSession: QuizSessionDto = {
      ...session,
      answers: {
        [question.id]: [201, 203]
      }
    };
    const updateSession = jasmine.createSpy('updateSession').and.returnValue(of(updatedSession));
    let fixture: ComponentFixture<QuizPlayPageComponent> | undefined;

    void configureComponent(updateSession).then((createdFixture) => {
      fixture = createdFixture;
    });
    flushMicrotasks();

    const component = fixture!.componentInstance;

    component.toggleAnswer(201);
    component.toggleAnswer(203);

    expect(component.isSelected(question.id, 201)).toBeTrue();
    expect(component.isSelected(question.id, 203)).toBeTrue();
    expect(updateSession).not.toHaveBeenCalled();

    tick(250);

    expect(updateSession).toHaveBeenCalledTimes(1);
    expect(updateSession.calls.mostRecent().args[2]).toEqual({
      currentIndex: 0,
      answers: [
        {
          questionId: question.id,
          answerIds: [201, 203]
        }
      ]
    });
  }));

  it('should ignore stale save responses when a newer selection is already queued', fakeAsync(() => {
    const firstSave = new Subject<QuizSessionDto>();
    const secondResponse: QuizSessionDto = {
      ...session,
      answers: {
        [question.id]: [201, 203]
      }
    };
    const updateSession = jasmine.createSpy('updateSession')
      .and.returnValues(firstSave.asObservable(), of(secondResponse));
    let fixture: ComponentFixture<QuizPlayPageComponent> | undefined;

    void configureComponent(updateSession).then((createdFixture) => {
      fixture = createdFixture;
    });
    flushMicrotasks();

    const component = fixture!.componentInstance;

    component.toggleAnswer(201);
    tick(250);

    expect(updateSession).toHaveBeenCalledTimes(1);
    expect(component.isSelected(question.id, 201)).toBeTrue();

    component.toggleAnswer(203);
    tick(250);

    expect(updateSession).toHaveBeenCalledTimes(1);
    expect(component.isSelected(question.id, 203)).toBeTrue();

    firstSave.next({
      ...session,
      answers: {
        [question.id]: [201]
      }
    });

    expect(component.isSelected(question.id, 201)).toBeTrue();
    expect(component.isSelected(question.id, 203)).toBeTrue();

    firstSave.complete();

    expect(updateSession).toHaveBeenCalledTimes(2);
    expect(updateSession.calls.mostRecent().args[2]).toEqual({
      currentIndex: 0,
      answers: [
        {
          questionId: question.id,
          answerIds: [201, 203]
        }
      ]
    });

    expect(component.isSelected(question.id, 201)).toBeTrue();
    expect(component.isSelected(question.id, 203)).toBeTrue();
  }));

  it('should show categories and explanation below the action buttons after checking the question', async () => {
    const questionWithFeedback: QuestionDto = {
      ...question,
      explanation: 'Explanation content',
      categories: [
        { id: 1, name: 'Security' },
        { id: 2, name: 'Tokens' }
      ]
    };
    const fixture = await configureComponent(() => of(session), questionWithFeedback);

    expect(fixture.nativeElement.querySelector('.quiz-play-question__categories')).toBeNull();
    expect(fixture.nativeElement.querySelector('.quiz-play-question__explanation')).toBeNull();

    fixture.componentInstance.checkedQuestionIds.set(new Set([question.id]));
    fixture.detectChanges();

    const actions = fixture.nativeElement.querySelector('.quiz-play-actions') as HTMLElement | null;
    const categories = fixture.nativeElement.querySelector('.quiz-play-question__categories') as HTMLElement | null;
    const explanation = fixture.nativeElement.querySelector('.quiz-play-question__explanation') as HTMLElement | null;

    expect(actions).not.toBeNull();
    expect(categories).not.toBeNull();
    expect(explanation).not.toBeNull();
    expect(categories?.textContent).toContain('Security');
    expect(categories?.textContent).toContain('Tokens');
    expect(explanation?.textContent).toContain('Explanation content');
    expect(actions?.compareDocumentPosition(categories!)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    expect(categories?.compareDocumentPosition(explanation!)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
  });
});
