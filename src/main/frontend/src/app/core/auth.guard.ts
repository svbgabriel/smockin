import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const requiresAdmin = route.data?.['adminOnly'] === true;

  if (!auth.isLoggedIn()) {
    router.navigate(['/login'], { queryParams: { redirect: state.url } });
    return false;
  }

  if (requiresAdmin && !auth.isAdmin()) {
    router.navigate(['/dashboard']);
    return false;
  }

  return true;
};
