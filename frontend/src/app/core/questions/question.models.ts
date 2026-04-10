export interface QuestionCategoryDto {
  id: number;
  name: string;
}

export interface QuestionAnswerDto {
  id: number;
  displayOrder: number;
  content: string;
  correct: boolean;
}

export interface QuestionDto {
  id: number;
  courseId: number;
  currentVersionNumber: number;
  createdAt: string;
  updatedAt: string;
  prompt: string;
  categories: QuestionCategoryDto[];
  answers: QuestionAnswerDto[];
}

export interface QuestionVersionDto {
  id: number;
  questionId: number;
  versionNumber: number;
  createdAt: string;
  prompt: string;
  categories: QuestionCategoryDto[];
  answers: QuestionAnswerDto[];
}

export interface QuestionPageDto {
  items: QuestionDto[];
  pageNumber: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface SaveQuestionRequest {
  prompt: string;
  answers: Array<{
    content: string;
    correct: boolean;
  }>;
  categoryIds: number[];
}
