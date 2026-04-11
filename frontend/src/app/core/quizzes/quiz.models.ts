export type QuizMode = 'manual' | 'random' | 'category';
export type QuizOrderMode = 'fixed' | 'random';

export interface QuizCategoryDto {
  id: number;
  name: string;
}

export interface QuizDto {
  id: number;
  courseId: number;
  active: boolean;
  currentVersionNumber: number;
  createdAt: string;
  updatedAt: string;
  title: string;
  mode: QuizMode;
  randomCount: number | null;
  questionOrder: QuizOrderMode;
  answerOrder: QuizOrderMode;
  questionIds: number[];
  categories: QuizCategoryDto[];
  resolvedQuestionCount: number;
}

export interface QuizVersionDto {
  id: number;
  quizId: number;
  versionNumber: number;
  createdAt: string;
  title: string;
  mode: QuizMode;
  randomCount: number | null;
  questionOrder: QuizOrderMode;
  answerOrder: QuizOrderMode;
  questionIds: number[];
  categories: QuizCategoryDto[];
  resolvedQuestionCount: number;
}

export interface SaveQuizRequest {
  title: string;
  mode: QuizMode;
  randomCount: number | null;
  questionOrder: QuizOrderMode;
  answerOrder: QuizOrderMode;
  questionIds: number[];
  categoryIds: number[];
}
