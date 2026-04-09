import { TestBed } from '@angular/core/testing';

import { pendingChangesGuard } from './pending-changes.guard';

describe('pendingChangesGuard', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should allow navigation when there are no pending changes', () => {
    const result = TestBed.runInInjectionContext(() =>
      pendingChangesGuard(
        {
          hasPendingChanges: () => false
        },
        {} as never,
        {} as never,
        {} as never
      )
    );

    expect(result).toBeTrue();
  });

  it('should ask the component whether to discard changes when there are pending changes', async () => {
    const result = TestBed.runInInjectionContext(() =>
      pendingChangesGuard(
        {
          hasPendingChanges: () => true,
          confirmDiscardChanges: () => Promise.resolve(false)
        },
        {} as never,
        {} as never,
        {} as never
      )
    );

    await expectAsync(Promise.resolve(result)).toBeResolvedTo(false);
  });
});
