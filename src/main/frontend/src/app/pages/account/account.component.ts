import { Component } from '@angular/core';
import { NgIf } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AlertsComponent } from '../../shared/alerts/alerts.component';
import { Alert } from '../../core/models';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [AlertsComponent, I18nPipe, NgIf, ReactiveFormsModule],
  templateUrl: './account.component.html',
  styleUrls: ['./account.component.css']
})
export class AccountComponent {
  alerts: Alert[] = [];
  form: FormGroup;
  isBusy = false;

  constructor(
    private readonly api: ApiService,
    private readonly fb: FormBuilder,
    private readonly auth: AuthService,
    private readonly i18n: I18nService
  ) {
    this.form = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', Validators.required],
      confirmPassword: ['', Validators.required]
    });
  }

  get username(): string | null {
    return this.auth.getUserName();
  }

  get fullName(): string | null {
    return this.auth.getFullName();
  }

  submit(): void {
    if (this.form.invalid) {
      this.showAlert(this.i18n.t('account.errors.requiredFields'));
      return;
    }

    const currentPassword = this.form.controls['currentPassword'].value ?? '';
    const newPassword = this.form.controls['newPassword'].value ?? '';
    const confirmPassword = this.form.controls['confirmPassword'].value ?? '';

    if (newPassword !== confirmPassword) {
      this.showAlert(this.i18n.t('account.errors.passwordMismatch'));
      this.form.controls['newPassword'].reset();
      this.form.controls['confirmPassword'].reset();
      return;
    }

    this.isBusy = true;
    this.api.patch<void>('/user/password', { currentPassword, newPassword }).subscribe({
      next: () => {
        this.isBusy = false;
        this.form.reset();
        this.showAlert(this.i18n.t('account.success.updated'), 'success');
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

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
