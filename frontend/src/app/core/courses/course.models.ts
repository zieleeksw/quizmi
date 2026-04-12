export interface CourseDto {
  id: number;
  name: string;
  description: string;
  createdAt: string;
  ownerUserId: number;
  ownerEmail: string;
}

export interface CreateCourseRequest {
  name: string;
  description: string;
}

export interface UpdateCourseRequest {
  name: string;
  description: string;
}
