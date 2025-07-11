import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';

import { AuthenticationService } from '../services/authentication.service';
import { RbacService } from '../services/rbac.service';
@Injectable({
  providedIn: 'root'
})
export class AdminGuard implements CanActivate {
  constructor(private authService: AuthenticationService, private router: Router) {}

  canActivate(): boolean {
    if (!this.authService.hasRole('ROLE_ADMIN')) {
      this.router.navigate(['/unauthorized']);
      return false;
    }
    return true;
  }
}