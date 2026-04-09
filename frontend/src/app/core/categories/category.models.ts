export interface CategoryDto {
  id: number;
  courseId: number;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCategoryRequest {
  name: string;
}

export interface UpdateCategoryRequest {
  name: string;
}

export interface CategoryVersionDto {
  id: number;
  categoryId: number;
  versionNumber: number;
  name: string;
  createdAt: string;
}
