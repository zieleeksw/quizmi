export type CourseMembershipRole = 'OWNER' | 'MODERATOR' | 'MEMBER';
export type CourseMembershipStatus = 'ACTIVE' | 'PENDING';

export interface CourseDto {
  id: number;
  name: string;
  description: string;
  createdAt: string;
  ownerUserId: number;
  ownerEmail: string;
  membershipRole: CourseMembershipRole | null;
  membershipStatus: CourseMembershipStatus | null;
  canAccess: boolean;
  canManage: boolean;
  pendingRequestsCount: number;
}

export interface CourseMemberDto {
  userId: number;
  email: string;
  role: CourseMembershipRole;
  status: CourseMembershipStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CourseMembersDto {
  members: CourseMemberDto[];
  pendingRequests: CourseMemberDto[];
}

export interface CreateCourseRequest {
  name: string;
  description: string;
}

export interface UpdateCourseRequest {
  name: string;
  description: string;
}

export interface UpdateCourseMemberRoleRequest {
  role: Exclude<CourseMembershipRole, 'OWNER'>;
}
