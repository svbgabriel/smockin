import { Component, OnInit } from '@angular/core';
import { NgIf } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiService } from '../../core/api.service';
import { AlertsComponent } from '../../shared/alerts/alerts.component';
import { Alert, SimpleMessageResponse } from '../../core/models';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [AlertsComponent, I18nPipe, NgIf, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent implements OnInit {
  alerts: Alert[] = [];
  form: FormGroup;
  isBusy = false;
  isDone = false;
  token: string | null = null;

  constructor(
    private readonly api: ApiService,
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly i18n: I18nService
  ) {
    this.form = this.fb.group({
      newPassword: ['', Validators.required],
      confirmPassword: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('rt');
    if (!this.token) {
      this.redirectToLogin();
      return;
    }

    this.checkUserMode();
  }

  submit(): void {
    if (this.form.invalid) {
      this.showAlert(this.i18n.t('resetPassword.errors.requiredFields'));
      return;
    }

    const newPassword = this.form.controls['newPassword'].value ?? '';
    const confirmPassword = this.form.controls['confirmPassword'].value ?? '';

    if (newPassword.trim().length === 0) {
      this.showAlert(this.i18n.t('resetPassword.errors.newPasswordRequired'));
      return;
    }

    if (confirmPassword.trim().length === 0) {
      this.showAlert(this.i18n.t('resetPassword.errors.confirmPasswordRequired'));
      return;
    }

    if (newPassword !== confirmPassword) {
      this.showAlert(this.i18n.t('resetPassword.errors.passwordMismatch'));
      this.form.controls['confirmPassword'].reset();
      return;
    }

    this.isBusy = true;
    this.api.post<void>(`/password/reset/token/${this.token}`, { newPassword }).subscribe({
      next: () => {
        this.isBusy = false;
        this.isDone = true;
        this.form.reset();
        this.showAlert(this.i18n.t('resetPassword.success.updated'), 'success');
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

  private checkUserMode(): void {
    this.api.get<SimpleMessageResponse>('/user/mode').subscribe({
      next: (data) => {
        if (data?.message !== 'ACTIVE') {
          this.redirectToLogin();
          return;
        }
        this.validateToken();
      },
      error: () => this.redirectToLogin()
    });
  }

  private validateToken(): void {
    if (!this.token) {
      this.redirectToLogin();
      return;
    }

    this.api.get<void>(`/password/reset/token/${this.token}`).subscribe({
      next: () => {},
      error: () => this.redirectToLogin()
    });
  }

  private redirectToLogin(): void {
    this.router.navigate(['/login']);
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
