import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { QuizAttemptDetailDto, QuizAttemptDto } from '../../core/attempts/attempt.models';
import { AttemptService } from '../../core/attempts/attempt.service';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { QuestionDto } from '../../core/questions/question.models';
import { QuestionService } from '../../core/questions/question.service';
import { QuizDto } from '../../core/quizzes/quiz.models';
import { QuizService } from '../../core/quizzes/quiz.service';
import { QuizStatisticsPageComponent } from './quiz-statistics-page.component';

describe('QuizStatisticsPageComponent', () => {
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

  const quiz: QuizDto = {
    id: 11,
    courseId: course.id,
    active: true,
    currentVersionNumber: 1,
    createdAt: '2026-04-20T20:00:00Z',
    updatedAt: '2026-04-20T20:00:00Z',
    title: 'Authentication mastery',
    mode: 'manual',
    randomCount: null,
    questionOrder: 'fixed',
    answerOrder: 'fixed',
    questionIds: Array.from({ length: 13 }, (_, index) => index + 1),
    categories: [],
    resolvedQuestionCount: 13
  };

  const questions: QuestionDto[] = Array.from({ length: 13 }, (_, index) => ({
    id: index + 1,
    courseId: course.id,
    currentVersionNumber: 1,
    createdAt: '2026-04-20T20:00:00Z',
    updatedAt: '2026-04-20T20:00:00Z',
    prompt: `Question ${index + 1}`,
    explanation: null,
    categories: [{ id: index + 1, name: `Category ${index + 1}` }],
    answers: [
      { id: 1000 + index, displayOrder: 0, content: 'Option A', correct: true },
      { id: 2000 + index, displayOrder: 1, content: 'Option B', correct: false }
    ]
  }));

  const attempts: QuizAttemptDto[] = Array.from({ length: 10 }, (_, index) => ({
    id: index + 1,
    courseId: course.id,
    quizId: quiz.id,
    userId: 5,
    quizTitle: quiz.title,
    correctAnswers: index,
    totalQuestions: questions.length,
    finishedAt: `2026-04-${String(index + 1).padStart(2, '0')}T20:00:00Z`
  }));

  const attemptReviews: QuizAttemptDetailDto[] = Array.from({ length: 10 }, (_, reviewIndex) => ({
    id: reviewIndex + 1,
    courseId: course.id,
    quizId: quiz.id,
    userId: 5,
    quizTitle: quiz.title,
    correctAnswers: questions.filter((_, questionIndex) => reviewIndex < questionIndex).length,
    totalQuestions: questions.length,
    finishedAt: `2026-04-${String(reviewIndex + 1).padStart(2, '0')}T20:00:00Z`,
    questions: questions.map((question, questionIndex) => ({
      questionId: question.id,
      prompt: question.prompt,
      explanation: null,
      selectedAnswerIds: [],
      correctAnswerIds: [question.answers[0].id],
      answeredCorrectly: reviewIndex < questionIndex,
      answers: question.answers
    }))
  }));

  async function configureComponent() {
    await TestBed.configureTestingModule({
      imports: [QuizStatisticsPageComponent],
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
          provide: AttemptService,
          useValue: {
            fetchAttempts: () => of(attempts),
            fetchAttemptReviews: () => of(attemptReviews)
          }
        },
        {
          provide: QuestionService,
          useValue: {
            fetchQuestions: () => of(questions)
          }
        }
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(QuizStatisticsPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  function getPanelByHeading(root: ParentNode, heading: string): HTMLElement {
    const panels = Array.from(root.querySelectorAll('section.quiz-statistics-panel')) as HTMLElement[];
    const panel = panels.find((candidate) => candidate.querySelector('h2')?.textContent?.trim() === heading);

    if (!panel) {
      throw new Error(`Panel "${heading}" not found`);
    }

    return panel;
  }

  it('should render All questions as a non-collapsible table sorted from worst to best and limited to 10 first', async () => {
    const fixture = await configureComponent();
    const allQuestionsPanel = getPanelByHeading(fixture.nativeElement, 'All questions');
    const renderedRows = Array.from(allQuestionsPanel.querySelectorAll('tbody tr')) as HTMLTableRowElement[];
    const accuracies = fixture.componentInstance.questionStats().map((question) => question.accuracy);

    expect(accuracies).toEqual([...accuracies].sort((left, right) => left - right));
    expect(allQuestionsPanel.querySelector('.quiz-statistics-table-wrapper')).not.toBeNull();
    expect(allQuestionsPanel.querySelector('.quiz-statistics-question-card')).toBeNull();
    expect(allQuestionsPanel.querySelector('details, summary, [aria-expanded]')).toBeNull();
    expect(renderedRows.length).toBe(10);
    expect(renderedRows[0].textContent).toContain('Question 1');
    expect(renderedRows[0].textContent).toContain('0%');
    expect(renderedRows[0].textContent).toContain('Category 1');
    expect(renderedRows[0].textContent).toContain('10');
    expect(renderedRows[9].textContent).toContain('Question 10');
    expect(renderedRows[9].textContent).toContain('90%');

    const showMoreButton = allQuestionsPanel.querySelector(
      '.quiz-statistics-question-list__button'
    ) as HTMLButtonElement | null;

    expect(showMoreButton?.textContent?.trim()).toBe('Show 3 more');
  });

  it('should append the remaining questions and hide the button when the last batch is shown', async () => {
    const fixture = await configureComponent();
    const allQuestionsPanel = getPanelByHeading(fixture.nativeElement, 'All questions');
    const showMoreButton = allQuestionsPanel.querySelector(
      '.quiz-statistics-question-list__button'
    ) as HTMLButtonElement;

    showMoreButton.click();
    fixture.detectChanges();

    const renderedRows = Array.from(allQuestionsPanel.querySelectorAll('tbody tr')) as HTMLTableRowElement[];

    expect(renderedRows.length).toBe(13);
    expect(renderedRows[10].textContent).toContain('Question 11');
    expect(renderedRows[12].textContent).toContain('Question 13');
    expect(allQuestionsPanel.querySelector('.quiz-statistics-question-list__button')).toBeNull();
  });
});
