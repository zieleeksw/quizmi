export interface QuizAttemptAnswerRequest {
  questionId: number;
  answerIds: number[];
}

export interface QuizSessionDto {
  id: number;
  courseId: number;
  quizId: number;
  userId: number;
  quizTitle: string;
  questionIds: number[];
  currentIndex: number;
  answers: Record<string, number[]>;
  updatedAt: string;
}

export interface QuizAttemptDto {
  id: number;
  courseId: number;
  quizId: number;
  userId: number;
  quizTitle: string;
  correctAnswers: number;
  totalQuestions: number;
  finishedAt: string;
}

export interface QuizAttemptAnswerReviewDto {
  id: number;
  displayOrder: number;
  content: string;
  correct: boolean;
}

export interface QuizAttemptQuestionReviewDto {
  questionId: number;
  prompt: string;
  selectedAnswerIds: number[];
  correctAnswerIds: number[];
  answeredCorrectly: boolean;
  answers: QuizAttemptAnswerReviewDto[];
}

export interface QuizAttemptDetailDto {
  id: number;
  courseId: number;
  quizId: number;
  userId: number;
  quizTitle: string;
  correctAnswers: number;
  totalQuestions: number;
  finishedAt: string;
  questions: QuizAttemptQuestionReviewDto[];
}

export interface SaveQuizSessionRequest {
  currentIndex: number;
  answers: QuizAttemptAnswerRequest[];
}
