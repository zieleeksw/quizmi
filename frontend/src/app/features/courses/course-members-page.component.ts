import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { CourseDto, CourseMemberDto, CourseMembersDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { PendingChangesDialogService } from '../../core/navigation/pending-changes-dialog.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { CourseDetailsPageComponent } from './course-details-page.component';

@Component({
  selector: 'app-course-members-page',
  imports: [DatePipe, ToastStackComponent],
  templateUrl: './course-members-page.component.html',
  styleUrl: './course-members-page.component.scss'
})
export class CourseMembersPageComponent {
  private readonly roleDrafts = signal<Record<number, 'MEMBER' | 'MODERATOR'>>({});
  private readonly requestActionIds = signal<number[]>([]);
  private readonly memberActionIds = signal<number[]>([]);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly courseService = inject(CourseService);
  private readonly pendingDialog = inject(PendingChangesDialogService);
  private readonly courseDetailsPage = inject(CourseDetailsPageComponent, { optional: true });
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = Number.parseInt(
    this.route.parent?.snapshot.paramMap.get('courseId') ?? this.route.snapshot.paramMap.get('courseId') ?? '',
    10
  );
  readonly course = signal<CourseDto | null>(null);
  readonly membersState = signal<CourseMembersDto>({ members: [], pendingRequests: [] });
  readonly isLoading = signal(true);
  readonly isRefreshing = signal(false);
  readonly isSavingRoleChanges = signal(false);
  readonly errorToasts = signal<ToastItem[]>([]);
  readonly canManageCourse = computed(() => this.course()?.canManage ?? false);
  readonly actorRole = computed(() => this.course()?.membershipRole ?? null);
  readonly canFullyManageRoles = computed(() => {
    const currentCourse = this.course();
    return Boolean(currentCourse?.canManage && (currentCourse.membershipRole === 'OWNER' || currentCourse.membershipRole === null));
  });
  readonly hasPendingRoleChanges = computed(() =>
    this.membersState().members.some((member) => this.canEditMemberRole(member) && this.hasRoleChanges(member))
  );

  constructor() {
    this.destroyRef.onDestroy(() => {
      for (const timeout of this.toastTimeouts.values()) {
        clearTimeout(timeout);
      }
    });

    this.loadPage();
  }

  dismissToast(id: number): void {
    const timeout = this.toastTimeouts.get(id);

    if (timeout) {
      clearTimeout(timeout);
      this.toastTimeouts.delete(id);
    }

    this.errorToasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  approveRequest(memberUserId: number): void {
    if (!Number.isFinite(this.courseId) || !this.canManageCourse() || this.isRequestActionPending(memberUserId)) {
      return;
    }

    this.markRequestAction(memberUserId, true);
    this.isRefreshing.set(true);

    this.courseService
      .approveCourseMember(this.courseId, memberUserId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.refreshMembers(),
        error: (error) => {
          this.markRequestAction(memberUserId, false);
          this.isRefreshing.set(false);
          this.pushToast(extractApiMessage(error) ?? 'Unable to approve this join request right now.');
        }
      });
  }

  declineRequest(memberUserId: number): void {
    if (!Number.isFinite(this.courseId) || !this.canManageCourse() || this.isRequestActionPending(memberUserId)) {
      return;
    }

    this.markRequestAction(memberUserId, true);
    this.isRefreshing.set(true);

    this.courseService
      .declineCourseJoinRequest(this.courseId, memberUserId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.refreshMembers(),
        error: (error) => {
          this.markRequestAction(memberUserId, false);
          this.isRefreshing.set(false);
          this.pushToast(extractApiMessage(error) ?? 'Unable to decline this join request right now.');
        }
      });
  }

  selectRole(memberUserId: number, role: 'MEMBER' | 'MODERATOR'): void {
    const member = this.membersState().members.find((item) => item.userId === memberUserId);

    if (!member || this.isSavingRoleChanges() || !this.canSelectRole(member, role)) {
      return;
    }

    this.roleDrafts.update((drafts) => ({ ...drafts, [memberUserId]: role }));
  }

  saveRoleChanges(): void {
    if (!Number.isFinite(this.courseId) || !this.hasPendingRoleChanges() || this.isSavingRoleChanges()) {
      return;
    }

    const changes = this.membersState().members
      .filter((member) => this.canEditMemberRole(member))
      .map((member) => ({ member, role: this.roleValue(member) }))
      .filter(({ member, role }) => member.role !== role);

    if (!changes.length) {
      return;
    }

    this.isSavingRoleChanges.set(true);
    this.isRefreshing.set(true);

    forkJoin(
      changes.map(({ member, role }) => this.courseService.updateCourseMemberRole(this.courseId, member.userId, { role }))
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.refreshMembers(),
        error: (error) => {
          this.isSavingRoleChanges.set(false);
          this.isRefreshing.set(false);
          this.pushToast(extractApiMessage(error) ?? 'Unable to save member role changes right now.');
        }
      });
  }

  async removeMember(member: CourseMemberDto): Promise<void> {
    if (!Number.isFinite(this.courseId) || !this.canRemoveMember(member) || this.isMemberActionPending(member.userId)) {
      return;
    }

    const confirmed = await this.pendingDialog.confirm({
      eyebrow: 'Membership Update',
      title: 'Remove course access?',
      message: `${member.email} will immediately lose access to this course.`,
      confirmLabel: 'Remove access',
      cancelLabel: 'Cancel'
    });

    if (!confirmed) {
      return;
    }

    this.markMemberAction(member.userId, true);
    this.isRefreshing.set(true);

    this.courseService
      .removeCourseMember(this.courseId, member.userId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.refreshMembers(),
        error: (error) => {
          this.markMemberAction(member.userId, false);
          this.isRefreshing.set(false);
          this.pushToast(extractApiMessage(error) ?? 'Unable to remove this course member right now.');
        }
      });
  }

  roleValue(member: CourseMemberDto): 'MEMBER' | 'MODERATOR' {
    return this.roleDrafts()[member.userId] ?? (member.role === 'MODERATOR' ? 'MODERATOR' : 'MEMBER');
  }

  hasRoleChanges(member: CourseMemberDto): boolean {
    return member.role !== this.roleValue(member);
  }

  isRequestActionPending(memberUserId: number): boolean {
    return this.requestActionIds().includes(memberUserId);
  }

  isMemberActionPending(memberUserId: number): boolean {
    return this.memberActionIds().includes(memberUserId);
  }

  isOwnerMember(member: CourseMemberDto): boolean {
    return member.role === 'OWNER';
  }

  canEditMemberRole(member: CourseMemberDto): boolean {
    if (member.role === 'OWNER') {
      return false;
    }

    if (this.canFullyManageRoles()) {
      return true;
    }

    return this.actorRole() === 'MODERATOR' && member.role === 'MODERATOR';
  }

  canSelectRole(member: CourseMemberDto, role: 'MEMBER' | 'MODERATOR'): boolean {
    if (!this.canEditMemberRole(member)) {
      return false;
    }

    if (this.canFullyManageRoles()) {
      return true;
    }

    return role === 'MEMBER' && member.role === 'MODERATOR';
  }

  canRemoveMember(member: CourseMemberDto): boolean {
    return this.canManageCourse() && member.role !== 'OWNER';
  }

  trackByMemberUserId(_index: number, member: CourseMemberDto): number {
    return member.userId;
  }

  private loadPage(): void {
    if (!Number.isFinite(this.courseId)) {
      this.isLoading.set(false);
      this.pushToast('This course link is invalid.');
      return;
    }

    this.isLoading.set(true);

    forkJoin({
      course: this.courseService.fetchCourse(this.courseId),
      members: this.courseService.fetchCourseMembers(this.courseId)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ course, members }) => {
          this.course.set(course);
          this.courseDetailsPage?.syncCourse(course);
          this.membersState.set(members);
          this.syncRoleDrafts(members.members);
          this.isLoading.set(false);
          this.isRefreshing.set(false);
          this.isSavingRoleChanges.set(false);
          this.requestActionIds.set([]);
          this.memberActionIds.set([]);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.isRefreshing.set(false);
          this.pushToast(extractApiMessage(error) ?? 'Unable to load course members right now.');
        }
      });
  }

  private refreshMembers(): void {
    forkJoin({
      course: this.courseService.fetchCourse(this.courseId),
      members: this.courseService.fetchCourseMembers(this.courseId)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ course, members }) => {
          this.course.set(course);
          this.courseDetailsPage?.syncCourse(course);
          this.membersState.set(members);
          this.syncRoleDrafts(members.members);
          this.isRefreshing.set(false);
          this.isSavingRoleChanges.set(false);
          this.requestActionIds.set([]);
          this.memberActionIds.set([]);
        },
        error: (error) => {
          this.isSavingRoleChanges.set(false);
          this.isRefreshing.set(false);
          this.pushToast(extractApiMessage(error) ?? 'Unable to refresh course members right now.');
        }
      });
  }

  private syncRoleDrafts(members: CourseMemberDto[]): void {
    this.roleDrafts.set(
      members.reduce<Record<number, 'MEMBER' | 'MODERATOR'>>((drafts, member) => {
        if (member.role !== 'OWNER') {
          drafts[member.userId] = member.role === 'MODERATOR' ? 'MODERATOR' : 'MEMBER';
        }

        return drafts;
      }, {})
    );
  }

  private markRequestAction(memberUserId: number, active: boolean): void {
    this.requestActionIds.update((ids) => active ? [...ids, memberUserId] : ids.filter((id) => id !== memberUserId));
  }

  private markMemberAction(memberUserId: number, active: boolean): void {
    this.memberActionIds.update((ids) => active ? [...ids, memberUserId] : ids.filter((id) => id !== memberUserId));
  }

  private pushToast(message: string): void {
    const id = ++this.toastId;
    const timeout = setTimeout(() => this.dismissToast(id), 5000);

    this.toastTimeouts.set(id, timeout);
    this.errorToasts.update((toasts) => [
      ...toasts,
      {
        id,
        message,
        tone: 'error'
      }
    ]);
  }
}
