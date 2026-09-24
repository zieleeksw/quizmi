export interface AdminQuizAttemptDto {
  attemptId: number;
  finishedAt: string;
  courseId: number;
  courseName: string;
  quizId: number;
  quizTitle: string;
  userId: number;
  playerEmail: string;
  correctAnswers: number;
  totalQuestions: number;
}
