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

export interface AdminQuizAttemptPageDto {
  items: AdminQuizAttemptDto[];
  pageNumber: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface AdminCourseOptionDto {
  courseId: number;
  courseName: string;
}

export interface AdminQuizOptionDto {
  quizId: number;
  quizTitle: string;
  courseId: number;
  courseName: string;
}

export interface AdminUserOptionDto {
  userId: number;
  email: string;
}

export interface AdminQuizAttemptFilters {
  courseId: number | null;
  quizId: number | null;
  userId: number | null;
}
