import { Component, OnInit } from '@angular/core';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiService } from '../../core/api.service';
import { AlertsComponent } from '../../shared/alerts/alerts.component';
import { Alert, SimpleMessageResponse, UserResponse } from '../../core/models';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-manage-users',
  standalone: true,
  imports: [AlertsComponent, DatePipe, I18nPipe, NgFor, NgIf, ReactiveFormsModule],
  templateUrl: './manage-users.component.html',
  styleUrls: ['./manage-users.component.css']
})
export class ManageUsersComponent implements OnInit {
  alerts: Alert[] = [];
  users: UserResponse[] = [];
  selectedUser: UserResponse | null = null;
  passwordResetLink: string | null = null;
  isBusy = false;

  form!: FormGroup;

  constructor(private readonly api: ApiService, private readonly fb: FormBuilder, private readonly i18n: I18nService) {
    this.form = this.fb.group({
      username: ['', Validators.required],
      fullName: ['', Validators.required],
      password: [''],
      confirmPassword: ['']
    });
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  startNewUser(): void {
    this.selectedUser = null;
    this.passwordResetLink = null;
    this.form.reset();
  }

  selectUser(user: UserResponse): void {
    this.selectedUser = user;
    this.passwordResetLink = user.passwordResetToken
      ? `${window.location.origin}/reset_password?rt=${user.passwordResetToken}`
      : null;
    this.form.reset({
      username: user.username,
      fullName: user.fullName,
      password: '',
      confirmPassword: ''
    });
  }

  saveUser(): void {
    if (this.form.invalid) {
      this.showAlert(this.i18n.t('manageUsers.errors.requiredFields'));
      return;
    }

    const username = this.form.controls['username'].value?.trim() ?? '';
    const fullName = this.form.controls['fullName'].value?.trim() ?? '';
    const password = this.form.controls['password'].value ?? '';
    const confirmPassword = this.form.controls['confirmPassword'].value ?? '';

    if (username.includes(' ')) {
      this.showAlert(this.i18n.t('manageUsers.errors.usernameSpaces'));
      return;
    }

    if (!this.selectedUser) {
      if (!password) {
        this.showAlert(this.i18n.t('manageUsers.errors.passwordRequired'));
        return;
      }
      if (password !== confirmPassword) {
        this.showAlert(this.i18n.t('manageUsers.errors.passwordMismatch'));
        return;
      }

      this.isBusy = true;
      this.api
        .post<SimpleMessageResponse>('/user', {
          username,
          fullName,
          role: 'REGULAR',
          password
        })
        .subscribe({
          next: () => {
            this.isBusy = false;
            this.showAlert(this.i18n.t('manageUsers.success.created'), 'success');
            this.loadUsers();
            this.startNewUser();
          },
          error: (err) => {
            this.isBusy = false;
            if (err?.status === 400) {
              this.showAlert(err.error?.message ?? this.i18n.t('common.errors.invalidData'));
              return;
            }
            this.showAlert(this.i18n.t('common.errors.generic'));
          }
        });
      return;
    }

    this.isBusy = true;
    this.api
      .put<void>(`/user/${this.selectedUser.extId}`, {
        username,
        fullName,
        role: this.selectedUser.role
      })
      .subscribe({
      next: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('manageUsers.success.updated'), 'success');
        this.loadUsers();
      },
      error: (err) => {
        this.isBusy = false;
        if (err?.status === 400) {
          this.showAlert(err.error?.message ?? this.i18n.t('common.errors.invalidData'));
          return;
        }
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  deleteUser(): void {
    if (!this.selectedUser) {
      return;
    }
    if (this.selectedUser.role === 'SYS_ADMIN') {
      this.showAlert(this.i18n.t('manageUsers.errors.cannotDeleteAdmin'));
      return;
    }
    if (!confirm(this.i18n.t('manageUsers.confirm.delete'))) {
      return;
    }

    this.isBusy = true;
    this.api.delete<void>(`/user/${this.selectedUser.extId}`).subscribe({
      next: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('manageUsers.success.deleted'), 'success');
        this.loadUsers();
        this.startNewUser();
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  resetPassword(): void {
    if (!this.selectedUser) {
      return;
    }
    if (!confirm(this.i18n.t('manageUsers.confirm.resetPassword'))) {
      return;
    }

    this.api.get<SimpleMessageResponse>(`/user/${this.selectedUser.extId}/password/reset`).subscribe({
      next: (data) => {
        this.passwordResetLink = `${window.location.origin}/reset_password?rt=${data.message}`;
        this.showAlert(this.i18n.t('manageUsers.success.resetToken'), 'success');
        this.loadUsers();
      },
      error: () => {
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  private loadUsers(): void {
    this.api.get<UserResponse[]>('/user').subscribe({
      next: (data) => {
        this.users = data ?? [];
      },
      error: () => {
        this.showAlert(this.i18n.t('manageUsers.errors.loadUsers'));
      }
    });
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
